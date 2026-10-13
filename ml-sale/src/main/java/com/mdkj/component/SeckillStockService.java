package com.mdkj.component;

import cn.hutool.core.util.StrUtil;
import com.mdkj.util.ML;
import com.mdkj.util.MyRedis;
import com.mdkj.util.SeckillRedisKeys;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀库存与用户占位：Redis Lua 原子扣减，无全局锁。
 *
 * <p>{@link #tryKill} 现在作为 RocketMQ 事务消息的「本地事务」被调用，
 * 由 {@code SeckillTxListener} 驱动，不再由业务代码直接调用后自行补偿。</p>
 */
@Component
public class SeckillStockService {

    /**
     * 返回值：1=扣减成功，0=用户已占位（重复请求），-1=库存不足
     */
    private static final String LUA_TRY_KILL = """
            local stockKey = KEYS[1]
            local userKey = KEYS[2]
            local sn = ARGV[1]
            local ttl = tonumber(ARGV[2])
            local existing = redis.call('GET', userKey)
            if existing and existing ~= '' then
                return 0
            end
            local stock = tonumber(redis.call('GET', stockKey) or '0')
            if stock <= 0 then
                return -1
            end
            redis.call('DECR', stockKey)
            redis.call('SET', userKey, sn, 'EX', ttl)
            return 1
            """;

    /**
     * 只读预检脚本，语义与 {@link #LUA_TRY_KILL} 的前两个判断一致，但不做任何写操作。
     * 返回值：1=可以尝试，0=用户已占位，-1=库存不足
     */
    private static final String LUA_PRECHECK_KILL = """
            local existing = redis.call('GET', KEYS[2])
            if existing and existing ~= '' then
                return 0
            end
            if tonumber(redis.call('GET', KEYS[1]) or '0') <= 0 then
                return -1
            end
            return 1
            """;

    @Resource
    private MyRedis redis;
    @Resource
    private SeckillPrepayStore prepayStore;

    /**
     * 只读预检：不修改任何 Key，仅用于在发半消息之前挡掉注定失败的请求。
     *
     * <p>事务消息把「先扣减再发送」倒转成了「先发半消息再扣减」，若不预检，
     * 库存早已售罄的请求也要先往 broker 写一条半消息、再回滚，
     * 1000 打 100 的场景下 broker 写入量会放大约 10 倍，
     * 还会给最热的 7002 响应路径插进一次 broker 往返。</p>
     *
     * <p><b>它不是权威判据</b>：预检放行后仍可能被事务内的 {@link #tryKill} 判为 -1，
     * 这是正常的，正确性始终由事务内那次 Lua 保证。</p>
     *
     * @return 1=可以尝试，0=用户已占位，-1=库存不足
     */
    public long preCheckKill(Long seckillId, Long courseId, Long userId) {
        Long result = redis.lua(
                LUA_PRECHECK_KILL,
                List.of(SeckillRedisKeys.stock(seckillId, courseId),
                        SeckillRedisKeys.userOrder(seckillId, courseId, userId)));
        // 读不到就放行，把裁决权交给事务内的 Lua，避免预检自身故障误伤正常请求
        return result == null ? 1L : result;
    }

    public long tryKill(Long seckillId, Long courseId, Long userId, String sn) {
        String stockKey = SeckillRedisKeys.stock(seckillId, courseId);
        String userKey = SeckillRedisKeys.userOrder(seckillId, courseId, userId);
        Long result = redis.lua(
                LUA_TRY_KILL,
                List.of(stockKey, userKey),
                sn,
                String.valueOf(ML.Seckill.USER_ORDER_TTL_SECONDS));
        return result == null ? -1L : result;
    }

    public String getUserOrderSn(Long seckillId, Long courseId, Long userId) {
        return redis.get(SeckillRedisKeys.userOrder(seckillId, courseId, userId));
    }

    /**
     * 是否已有支付标记。事务回查必须先问这一句：用户既然已经付款，
     * 无论占位是否还在，订单都必须建出来，绝不能判回滚。
     * 与 {@code SeckillStockCompensator#rollbackIfOwned} 和
     * {@code OrderServiceImpl#handleSeckillOrderTimeout} 的守卫保持一致。
     */
    public boolean isPaid(String sn) {
        return prepayStore.isPaid(sn);
    }

    public void savePrepay(String sn, Double payAmount, Long userId, Long seckillId, Long courseId) {
        prepayStore.savePrepay(sn, payAmount, userId, seckillId, courseId);
    }

    public void initStock(Long seckillId, Long courseId, int stock) {
        redis.setEx(
                SeckillRedisKeys.stock(seckillId, courseId),
                String.valueOf(stock),
                ML.Seckill.STOCK_CACHE_HOURS,
                TimeUnit.HOURS);
    }

    /**
     * 单用户秒级限流，默认每秒最多 maxPerSecond 次 kill 请求
     */
    public boolean tryAcquireUserRate(Long userId, int maxPerSecond) {
        long second = System.currentTimeMillis() / 1000;
        String key = SeckillRedisKeys.userRate(userId, second);
        long count = redis.incr(key, 1);
        if (count == 1) {
            redis.expire(key, 2, TimeUnit.SECONDS);
        }
        return count <= maxPerSecond;
    }

    /**
     * 读取活动状态缓存，null 表示未命中（需要调用方回源查库并调用 {@link #cacheStatus} 回填）
     */
    public Integer getCachedStatus(Long seckillId) {
        String val = redis.get(SeckillRedisKeys.status(seckillId));
        return StrUtil.isBlank(val) ? null : Integer.valueOf(val);
    }

    /**
     * 缓存活动状态，短 TTL：既能挡住状态刚翻转瞬间的查库风暴，缓存又能很快自然失效更新
     */
    public void cacheStatus(Long seckillId, Integer status) {
        redis.setEx(SeckillRedisKeys.status(seckillId), String.valueOf(status),
                ML.Seckill.STATUS_CACHE_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 读取商品明细缓存（标题/封面/价格），返回空 Map 表示未命中
     */
    public Map<Object, Object> getCachedDetail(Long seckillId, Long courseId) {
        return redis.getHashOps().entries(SeckillRedisKeys.detail(seckillId, courseId));
    }

    /**
     * 缓存商品明细（标题/封面/价格），与库存缓存保持同样的时长，随每日预热任务刷新
     */
    public void cacheDetail(Long seckillId, Long courseId, String courseTitle, String courseCover,
                             Double coursePrice, Double skPrice) {
        String key = SeckillRedisKeys.detail(seckillId, courseId);
        redis.getHashOps().putAll(key, Map.of(
                "courseTitle", StrUtil.blankToDefault(courseTitle, ""),
                "courseCover", StrUtil.blankToDefault(courseCover, ""),
                "coursePrice", String.valueOf(coursePrice),
                "skPrice", String.valueOf(skPrice)));
        redis.expire(key, ML.Seckill.STOCK_CACHE_HOURS, TimeUnit.HOURS);
    }
}
