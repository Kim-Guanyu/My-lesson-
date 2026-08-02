package com.mdkj.component;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mdkj.util.ML;
import com.mdkj.util.MyRedis;
import com.mdkj.util.SeckillRedisKeys;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 秒杀预支付快照与「先支付后落库」标记。
 *
 * <p>拉码可在 MQ 建单之前进行；支付回调若早于建单，先记 paid 标记，
 * 建单成功后再把订单补成已支付。任何库存回滚前必须先检查 paid。</p>
 */
@Component
public class SeckillPrepayStore {

    @Resource
    private MyRedis redis;

    public void savePrepay(String sn, Double payAmount, Long userId, Long seckillId, Long courseId) {
        if (StrUtil.isBlank(sn) || payAmount == null) {
            return;
        }
        JSONObject json = new JSONObject();
        json.set("payAmount", payAmount);
        json.set("fkUserId", userId);
        json.set("fkSeckillId", seckillId);
        json.set("fkCourseId", courseId);
        redis.setEx(
                SeckillRedisKeys.prepay(sn),
                json.toString(),
                ML.Seckill.USER_ORDER_TTL_SECONDS,
                TimeUnit.SECONDS);
    }

    public JSONObject getPrepay(String sn) {
        if (StrUtil.isBlank(sn)) {
            return null;
        }
        String raw = redis.get(SeckillRedisKeys.prepay(sn));
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        return JSONUtil.parseObj(raw);
    }

    public Double getPrepayAmount(String sn) {
        JSONObject prepay = getPrepay(sn);
        if (prepay == null) {
            return null;
        }
        return prepay.getDouble("payAmount");
    }

    public void deletePrepay(String sn) {
        if (StrUtil.isBlank(sn)) {
            return;
        }
        redis.del(SeckillRedisKeys.prepay(sn));
    }

    /**
     * 标记已支付但订单可能尚未落库；TTL 略长于占位，避免建单稍晚时丢标记。
     */
    public void markPaid(String sn, Double payAmount) {
        if (StrUtil.isBlank(sn)) {
            return;
        }
        String val = payAmount == null ? "1" : String.valueOf(payAmount);
        redis.setEx(
                SeckillRedisKeys.paid(sn),
                val,
                ML.Seckill.USER_ORDER_TTL_SECONDS + 300L,
                TimeUnit.SECONDS);
    }

    public boolean isPaid(String sn) {
        return StrUtil.isNotBlank(sn) && redis.exists(SeckillRedisKeys.paid(sn));
    }

    public Double getPaidAmount(String sn) {
        if (StrUtil.isBlank(sn)) {
            return null;
        }
        String val = redis.get(SeckillRedisKeys.paid(sn));
        if (StrUtil.isBlank(val) || "1".equals(val)) {
            return null;
        }
        try {
            return Double.valueOf(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void clearPaid(String sn) {
        if (StrUtil.isBlank(sn)) {
            return;
        }
        redis.del(SeckillRedisKeys.paid(sn));
    }

    /** 建单完成后清理预支付快照；paid 由调用方在补状态后清理 */
    public void clearPrepayAfterOrderCreated(String sn) {
        deletePrepay(sn);
    }
}
