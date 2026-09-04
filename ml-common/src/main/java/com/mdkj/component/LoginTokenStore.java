package com.mdkj.component;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mdkj.entity.User;
import com.mdkj.util.ML;
import com.mdkj.util.MyRedis;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 登录令牌仓库：令牌的签发、解析与失效全部收口在这里。
 *
 * <h3>Redis 数据结构</h3>
 * <pre>
 * login:token:{uuid}   String  用户信息JSON      TTL 30min（网关每次请求续期）
 * login:user:{userId}  Set     该用户的全部uuid  无TTL（登录时自清理，踢人时整体删除）
 * </pre>
 *
 * <h3>为什么需要反向索引</h3>
 * <p>只有 {@code token -> user} 的正向映射时，登录态是一份「签发那一刻的用户快照」：
 * 管理员改了他的角色、禁用了他的账号，Redis 里那份快照不会变，而网关每次请求都在续期，
 * 于是只要用户保持活跃，这份旧身份可以无限期活下去。</p>
 *
 * <p>要主动作废就必须能从 userId 找到 token，这就是 {@code login:user:{userId}} 的用途。</p>
 *
 * <h3>两个操作顺序上的讲究</h3>
 * <ol>
 *   <li><b>签发：先写索引，后写令牌。</b>中途失败只会在索引里留下一个指向「不存在的令牌」的
 *       垃圾成员（无害，下次登录会被清掉）。反过来先写令牌的话，中途失败会留下一个
 *       <b>有效但踢不掉</b>的令牌，这是安全问题。失败要往安全的方向倒。</li>
 *   <li><b>失效：先删令牌，后删索引。</b>令牌一删，鉴权立刻失效；索引残留只是垃圾。</li>
 * </ol>
 *
 * @author Kim-Guanyu
 */
@Slf4j
@Component
public class LoginTokenStore {

    @Resource
    private MyRedis redis;

    /**
     * 签发一个新令牌。
     *
     * <p>同一用户多次登录会得到多个令牌并共存（支持后台与小程序同时在线），
     * 不会互相踢下线；需要「单点登录」时改成签发前先调用 {@link #invalidateByUserId} 即可。</p>
     *
     * @param user 已通过密码/验证码校验的用户实体
     * @return 返回给客户端的裸令牌（不含 Redis key 前缀）
     */
    public String issue(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("签发令牌失败：用户信息不完整");
        }
        Long userId = user.getId();
        String token = UUID.randomUUID().toString();

        // 顺手清掉索引里已经失效的历史令牌，避免 Set 随着登录次数无限膨胀。
        // 放在登录这条冷路径上做，是因为它低频；网关热路径一次都不碰索引。
        pruneDeadTokens(userId);

        // 先写索引（失败方向安全，见类注释）
        redis.sAdd(userIndexKey(userId), token);

        // 再写令牌本体。存的是脱去密码的副本：
        // ① 密码哈希在鉴权阶段完全用不到，存进 Redis 只是白白多一处泄露面；
        // ② 用副本是因为 UserUtil.desensitization 之类的工具是「原地修改」，
        //    直接改入参会污染调用方后续要返回给前端的那个对象。
        User snapshot = BeanUtil.copyProperties(user, User.class);
        snapshot.setPassword(null);
        redis.setEx(
                tokenKey(token),
                JSONUtil.toJsonStr(snapshot),
                ML.Redis.LOGIN_TOKEN_TTL_MINUTES,
                TimeUnit.MINUTES);

        log.info("签发登录令牌：userId={}", userId);
        return token;
    }

    /**
     * 解析令牌对应的用户，令牌无效时返回 {@code null}（不抛异常，由调用方决定怎么处理）。
     *
     * @param token 客户端携带的裸令牌
     * @return 登录用户快照，或 {@code null}
     */
    public User resolve(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        String userJson = redis.get(tokenKey(token));
        if (StrUtil.isBlank(userJson)) {
            return null;
        }
        return JSONUtil.toBean(userJson, User.class);
    }

    /**
     * 作废单个令牌，用于用户主动登出。
     *
     * @param token 客户端携带的裸令牌
     */
    public void invalidateByToken(String token) {
        if (StrUtil.isBlank(token)) {
            return;
        }
        // 先把 userId 取出来，否则删掉令牌后就不知道该从哪个索引里摘除了
        User user = resolve(token);
        redis.del(tokenKey(token));
        if (user != null && user.getId() != null) {
            redis.sRem(userIndexKey(user.getId()), token);
            log.info("登出，令牌已作废：userId={}", user.getId());
        }
    }

    /**
     * 作废某用户的<b>全部</b>令牌，强制其重新登录。
     *
     * <p>调用时机：改用户信息、改密码、改手机号、删用户、调整角色。
     * 只要 Redis 里那份快照和数据库不再一致，就应该调这个方法。</p>
     *
     * @param userId 用户主键
     * @return 实际作废的令牌数量
     */
    public int invalidateByUserId(Long userId) {
        if (userId == null) {
            return 0;
        }
        String indexKey = userIndexKey(userId);
        Set<String> tokens = redis.getSetOps().members(indexKey);
        if (tokens == null || tokens.isEmpty()) {
            // 索引不存在说明该用户当前没有登录态，直接返回
            return 0;
        }

        // 先删令牌本体，鉴权立刻失效
        Set<String> tokenKeys = new HashSet<>(tokens.size());
        for (String token : tokens) {
            tokenKeys.add(tokenKey(token));
        }
        redis.del(tokenKeys);

        // 再整体删掉索引
        redis.del(indexKey);

        log.info("用户信息变更，已强制下线：userId={}，作废令牌数={}", userId, tokens.size());
        return tokens.size();
    }

    /**
     * 批量作废多个用户的全部令牌，用于批量删除用户等场景。
     *
     * @param userIds 用户主键集合
     * @return 实际作废的令牌总数
     */
    public int invalidateByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (Long userId : userIds) {
            total += invalidateByUserId(userId);
        }
        return total;
    }

    /**
     * 拼装令牌的 Redis key。
     *
     * <p>客户端只见到裸 uuid，前缀由服务端拼接——这样既有独立命名空间，
     * 又不会把内部 key 规范暴露给客户端。</p>
     */
    public static String tokenKey(String token) {
        return ML.Redis.LOGIN_TOKEN_PREFIX + token;
    }

    /** 拼装用户反向索引的 Redis key */
    public static String userIndexKey(Long userId) {
        return ML.Redis.LOGIN_USER_INDEX_PREFIX + userId;
    }

    /**
     * 从索引里摘除那些「令牌本体已经过期、索引却还留着」的成员。
     *
     * <p>令牌是 TTL 到点自然消失的，Redis 不会顺带通知 Set 把成员删掉，
     * 所以索引必须自己清。放在登录时做：低频、且此时该用户的 Set 一定很小。</p>
     */
    private void pruneDeadTokens(Long userId) {
        String indexKey = userIndexKey(userId);
        Set<String> tokens = redis.getSetOps().members(indexKey);
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        for (String token : tokens) {
            if (!redis.exists(tokenKey(token))) {
                redis.sRem(indexKey, token);
            }
        }
    }
}
