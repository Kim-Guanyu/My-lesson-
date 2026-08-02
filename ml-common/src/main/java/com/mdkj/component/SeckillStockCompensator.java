package com.mdkj.component;

import com.mdkj.util.MyRedis;
import com.mdkj.util.SeckillRedisKeys;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 秒杀库存补偿。
 *
 * <p>只有用户占位仍属于指定订单号时才回补库存。校验、回补和删除占位在
 * 同一个 Lua 脚本中完成，因此同一订单消息被重复消费时最多补偿一次。</p>
 *
 * <p>若该 sn 已有支付成功标记（支付回调早于建单），则禁止回补库存，
 * 避免「用户已付款却被退库存」。</p>
 */
@Component
public class SeckillStockCompensator {

    private static final String LUA_ROLLBACK_IF_OWNED = """
            local stockKey = KEYS[1]
            local userKey = KEYS[2]
            local expectedSn = ARGV[1]
            local currentSn = redis.call('GET', userKey)
            if not currentSn or currentSn ~= expectedSn then
                return 0
            end
            redis.call('INCR', stockKey)
            redis.call('DEL', userKey)
            return 1
            """;

    @Resource
    private MyRedis redis;
    @Resource
    private SeckillPrepayStore prepayStore;

    /**
     * @return true 表示本次实际回补了库存；false 表示已补偿、占位已不属于该订单、或已支付禁止回滚
     */
    public boolean rollbackIfOwned(Long seckillId, Long courseId, Long userId, String sn) {
        if (seckillId == null || courseId == null || userId == null || sn == null || sn.isBlank()) {
            return false;
        }
        // 已支付（含先支付后落库）绝不能回补库存
        if (prepayStore.isPaid(sn)) {
            return false;
        }
        Long result = redis.lua(
                LUA_ROLLBACK_IF_OWNED,
                List.of(
                        SeckillRedisKeys.stock(seckillId, courseId),
                        SeckillRedisKeys.userOrder(seckillId, courseId, userId)),
                sn);
        boolean rolledBack = Long.valueOf(1L).equals(result);
        if (rolledBack) {
            // 占位已清，预支付快照一并失效，禁止再拉码
            prepayStore.deletePrepay(sn);
        }
        return rolledBack;
    }
}
