package com.mdkj.component;

import com.mdkj.dto.OrderMessage;
import com.mdkj.service.OrderService;
import com.mdkj.util.ML;
import com.mdkj.util.MyRedis;
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
    private MyRedis redis;

    @Override
    public void onMessage(OrderMessage orderMessage) {
        log.info("MQ 收到秒杀订单消息：用户 {} 课程 {} 订单号 {}",
                orderMessage.getFkUserId(), orderMessage.getFkCourseId(), orderMessage.getSn());
        try {
            orderService.createSeckillOrder(orderMessage);
        } catch (Exception e) {
            log.error("MQ 创建秒杀订单失败，回滚库存", e);
            if (orderMessage.getFkCourseId() != null) {
                redis.incr(ML.Redis.SECKILL_COURSE_COUNT_PREFIX + orderMessage.getFkCourseId(), 1);
            }
            throw e;
        }
    }
}
