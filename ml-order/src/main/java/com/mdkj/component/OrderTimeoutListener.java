package com.mdkj.component;

import com.mdkj.dto.OrderTimeoutMessage;
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
        consumerGroup = "ml-order-timeout-group",
        topic = "ml-topic",
        nameServer = "${rocketmq.name-server:192.168.211.132:9876}",
        selectorExpression = "order-timeout-tag",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING)
public class OrderTimeoutListener implements RocketMQListener<OrderTimeoutMessage> {

    @Resource
    private OrderService orderService;

    @Override
    public void onMessage(OrderTimeoutMessage message) {
        log.info("MQ 收到订单超时消息：订单号 {} 课程 {}", message.getSn(), message.getFkCourseId());
        orderService.handleSeckillOrderTimeout(message.getSn(), message.getFkCourseId());
    }
}
