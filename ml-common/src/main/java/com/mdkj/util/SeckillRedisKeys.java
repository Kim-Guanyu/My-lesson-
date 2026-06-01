package com.mdkj.util;

/**
 * 秒杀 Redis Key 规范（活动维度隔离库存与用户占位）
 */
public final class SeckillRedisKeys {

    private SeckillRedisKeys() {
    }

    /** 库存：seckill:stock:{seckillId}:{courseId} */
    public static String stock(Long seckillId, Long courseId) {
        return ML.Redis.SECKILL_STOCK_PREFIX + seckillId + ":" + courseId;
    }

    /** 用户占位（存订单号 sn）：seckill:user:{seckillId}:{courseId}:{userId} */
    public static String userOrder(Long seckillId, Long courseId, Long userId) {
        return ML.Redis.SECKILL_USER_ORDER_PREFIX + seckillId + ":" + courseId + ":" + userId;
    }

    /** 用户限流：seckill:rate:{userId}:{epochSecond} */
    public static String userRate(Long userId, long epochSecond) {
        return ML.Redis.SECKILL_RATE_PREFIX + userId + ":" + epochSecond;
    }

    /** 压测清理：seckill:user:{seckillId}: */
    public static String userOrderPrefix(Long seckillId) {
        return ML.Redis.SECKILL_USER_ORDER_PREFIX + seckillId + ":";
    }
}
