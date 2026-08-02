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
 * 秒杀库存与用户占位：Redis Lua 原子扣减，无全局锁
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

    @Resource
    private MyRedis redis;
    @Resource
    private SeckillStockCompensator stockCompensator;
    @Resource
    private SeckillPrepayStore prepayStore;

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

    public boolean rollbackKill(Long seckillId, Long courseId, Long userId, String sn) {
        return stockCompensator.rollbackIfOwned(seckillId, courseId, userId, sn);
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
