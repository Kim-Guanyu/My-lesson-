package com.mdkj.component;

import com.mdkj.util.ML;
import com.mdkj.util.MyRedis;
import com.mdkj.util.SeckillRedisKeys;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
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

    public void rollbackKill(Long seckillId, Long courseId, Long userId) {
        redis.incr(SeckillRedisKeys.stock(seckillId, courseId), 1);
        redis.del(SeckillRedisKeys.userOrder(seckillId, courseId, userId));
    }

    public void rollbackStock(Long seckillId, Long courseId) {
        redis.incr(SeckillRedisKeys.stock(seckillId, courseId), 1);
    }

    public void clearUserOrder(Long seckillId, Long courseId, Long userId) {
        redis.del(SeckillRedisKeys.userOrder(seckillId, courseId, userId));
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
}
