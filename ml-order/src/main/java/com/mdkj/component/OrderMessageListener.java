package com.mdkj.component;

import com.mdkj.dto.OrderMessage;
import com.mdkj.service.OrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
        consumerGroup = "ml-consumer-group",
        topic = "ml-topic",
        nameServer = "${rocketmq.name-server:192.168.211.132:9876}",
        selectorExpression = "ml-tag",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING)
public class OrderMessageListener implements RocketMQListener<OrderMessage> {

    @Resource
    private OrderService orderService;
    @Resource
    private SeckillStockCompensator stockCompensator;
    @Resource
    private SeckillPrepayStore prepayStore;

    @Override
    public void onMessage(OrderMessage orderMessage) {
        log.info("MQ 收到秒杀订单消息：用户 {} 课程 {} 订单号 {}",
                orderMessage.getFkUserId(), orderMessage.getFkCourseId(), orderMessage.getSn());
        try {
            orderService.createSeckillOrder(orderMessage);
        } catch (Exception e) {
            String sn = orderMessage.getSn();
            // 用户已付款时禁止回补库存，必须让 MQ 重试直到建单成功，否则会丢单
            if (prepayStore.isPaid(sn)) {
                log.error("MQ 创建秒杀订单失败但订单已支付，跳过库存补偿并触发重试，sn={}", sn, e);
                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new IllegalStateException("已支付订单建单失败，需重试: " + sn, e);
            }
            log.error("MQ 创建秒杀订单失败，执行幂等库存补偿", e);
            Long fkSeckillId = orderMessage.getFkSeckillId();
            Long fkCourseId = orderMessage.getFkCourseId();
            Long fkUserId = orderMessage.getFkUserId();
            boolean rolledBack = stockCompensator.rollbackIfOwned(
                    fkSeckillId,
                    fkCourseId,
                    fkUserId,
                    sn);
            log.warn("MQ 订单 {} 消费失败已结束消费，库存实际补偿={}", sn, rolledBack);
            // 补偿成功后不再抛异常，避免 RocketMQ 重投同一消息并重复补库存。
            // 若 Redis 补偿自身失败，rollbackIfOwned 会抛异常，消息仍会重试。
        }
    }
}
