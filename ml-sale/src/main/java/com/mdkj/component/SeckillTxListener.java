package com.mdkj.component;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mdkj.config.SeckillTxProducerConfig;
import com.mdkj.dto.OrderMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;

import java.nio.charset.StandardCharsets;

/**
 * 秒杀事务消息监听器：把「Redis 扣库存」作为本地事务，与「建单消息可投递」绑成一个原子单元。
 *
 * <p><b>要解决的问题</b>：旧实现是「先 Lua 扣减，再 convertAndSend，catch 到异常就回滚库存」。
 * 但网络超时的语义是三态（成功 / 失败 / <b>未知</b>），把「未知」当成「失败」去回滚，
 * 会在消息其实已投递的情况下把库存白还回去 —— 消费端照常建单，于是出现
 * 「订单有效但库存已释放」的悬空单，理论上可被别人再抢走，形成超卖窗口。
 * 1000 并发压测实测：96 单落库，占位只剩 71，库存多出 25。</p>
 *
 * <p><b>事务消息如何消除它</b>：半消息在提交前对消费端不可见；
 * 提交/回滚那次网络往返失败时，RocketMQ 客户端只记日志不抛异常，
 * 改由 broker 反向回查 {@link #checkLocalTransaction} 裁决。生产者不再需要靠猜。</p>
 *
 * @author Kim-Guanyu
 * @since v2.1.0
 */
@Slf4j
// @RocketMQTransactionListener 本身已被 @Component 元注解，无需再标注
@RocketMQTransactionListener(rocketMQTemplateBeanName = SeckillTxProducerConfig.TX_TEMPLATE_BEAN)
public class SeckillTxListener implements RocketMQLocalTransactionListener {

    @Resource
    private SeckillStockService seckillStockService;

    /**
     * 本地事务：执行 Lua 原子扣减。由发送线程内联同步调用。
     *
     * <p><b>不变量：Lua 一旦返回 1（库存已扣），就绝不能返回 ROLLBACK。</b>
     * ROLLBACK 只是让 broker 丢弃半消息，<b>不会</b>自动把库存加回去，
     * 那样会造成库存永久少卖。只有在 Lua 没扣成功时回滚才是安全的。</p>
     */
    @Override
    @SuppressWarnings("rawtypes")
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        if (!(arg instanceof SeckillTxContext ctx)) {
            // 理论上不会发生；返回 UNKNOWN 而不是 ROLLBACK，让回查按占位 Key 这个权威口径裁决
            log.error("秒杀事务消息缺少本地事务上下文，转入事务回查，arg={}", arg);
            return RocketMQLocalTransactionState.UNKNOWN;
        }
        try {
            long luaResult = seckillStockService.tryKill(
                    ctx.getSeckillId(), ctx.getCourseId(), ctx.getUserId(), ctx.getSn());
            ctx.setLuaResult(luaResult);

            if (luaResult != SeckillTxContext.LUA_OK) {
                // 0 重复占位 / -1 库存不足：库存没有发生变化，丢弃半消息即可，无需任何补偿
                log.info("秒杀本地事务未扣减，回滚半消息：订单 {}，Lua={}", ctx.getSn(), luaResult);
                return RocketMQLocalTransactionState.ROLLBACK;
            }

            // 预支付快照必须在消息对消费端可见「之前」写入：否则消费端建单后会先调
            // clearPrepayAfterOrderCreated 删快照，这里再写就留下一个永远没人清理的脏 Key。
            try {
                seckillStockService.savePrepay(ctx.getSn(), ctx.getSkPrice(),
                        ctx.getUserId(), ctx.getSeckillId(), ctx.getCourseId());
            } catch (Exception e) {
                // 快照只服务于「订单落库前抢先拉二维码」这一个窗口：拉码接口是 DB 优先、
                // 快照兜底，订单落库后根本不读它。缺失最多让用户早几秒拉码失败需重试，
                // 不影响最终支付 —— 为它回滚一个已扣成功的库存并不划算。
                log.warn("秒杀预支付快照写入失败，订单 {} 仍继续提交", ctx.getSn(), e);
            }
            return RocketMQLocalTransactionState.COMMIT;
        } catch (Exception e) {
            // Lua 执行本身异常，库存到底扣没扣未知，交给 broker 回查按占位 Key 裁决
            log.error("秒杀本地事务执行异常，转入事务回查：订单 {}", ctx.getSn(), e);
            ctx.setLuaResult(SeckillTxContext.LUA_NOT_EXECUTED);
            return RocketMQLocalTransactionState.UNKNOWN;
        }
    }

    /**
     * 事务回查：判断本地事务究竟成没成功。
     *
     * <p>用户占位 Key {@code seckill:user:{seckillId}:{courseId}:{userId}} 的值就是订单号，
     * 而它正是 Lua 扣减成功时才写入的 —— 「占位值 == 本消息的 sn」与「本地事务成功」完全等价。
     * 所以这个 Key 天然就是一张事务状态表，不需要额外建表或落库。</p>
     */
    @Override
    @SuppressWarnings("rawtypes")
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        OrderMessage orderMessage = parsePayload(msg);
        if (orderMessage == null
                || StrUtil.isBlank(orderMessage.getSn())
                || orderMessage.getFkSeckillId() == null
                || orderMessage.getFkCourseId() == null
                || orderMessage.getFkUserId() == null) {
            log.error("秒杀事务回查：消息体无法解析或身份字段不完整，判定回滚");
            return RocketMQLocalTransactionState.ROLLBACK;
        }

        String sn = orderMessage.getSn();
        try {
            // 已支付一律提交：用户钱都付了，订单必须建出来。这一条优先于占位校验，
            // 否则占位若因任何原因失效，就会变成「付了款却永远没有订单」——
            // 比原来的超卖窗口更严重。与项目其它回补路径的 isPaid 守卫保持一致。
            if (seckillStockService.isPaid(sn)) {
                log.warn("秒杀事务回查：订单 {} 已有支付标记，强制提交", sn);
                return RocketMQLocalTransactionState.COMMIT;
            }

            String heldSn = seckillStockService.getUserOrderSn(
                    orderMessage.getFkSeckillId(), orderMessage.getFkCourseId(), orderMessage.getFkUserId());
            boolean committed = sn.equals(heldSn);

            // 占位为空还有一种可能：TTL（960s）早于回查窗口（最长约 456s）过期。
            // 两者有 2 倍余量，正常不会发生；真发生也只是少卖，方向上安全（不会超卖）。
            log.info("秒杀事务回查：订单 {}，当前占位={}，判定={}", sn, heldSn, committed ? "COMMIT" : "ROLLBACK");
            return committed
                    ? RocketMQLocalTransactionState.COMMIT
                    : RocketMQLocalTransactionState.ROLLBACK;
        } catch (Exception e) {
            // 基础设施故障不能当业务结论：Redis 读不到不等于本地事务失败。
            // 返回 UNKNOWN 让 broker 下一轮再来，绝不在这里猜 ROLLBACK。
            log.error("秒杀事务回查异常，返回未知等待下一轮回查：订单 {}", sn, e);
            return RocketMQLocalTransactionState.UNKNOWN;
        }
    }

    /** 回查时拿到的 payload 是原始 JSON 字节，需要自己反序列化 */
    @SuppressWarnings("rawtypes")
    private OrderMessage parsePayload(Message msg) {
        try {
            Object payload = msg.getPayload();
            String json = payload instanceof byte[]
                    ? new String((byte[]) payload, StandardCharsets.UTF_8)
                    : String.valueOf(payload);
            return JSONUtil.toBean(json, OrderMessage.class);
        } catch (Exception e) {
            log.error("秒杀事务回查：消息体反序列化失败", e);
            return null;
        }
    }
}
