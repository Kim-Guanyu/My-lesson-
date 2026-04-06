//package com.mdkj.component;
//import cn.hutool.core.util.ObjectUtil;
//import cn.hutool.core.util.RandomUtil;
//
//import com.mdkj.dto.OrderMessage;
//import com.mdkj.entity.Course;
//import com.mdkj.entity.Order;
//import com.mdkj.entity.OrderDetail;
//import com.mdkj.entity.User;
//import com.mdkj.exception.ServiceException;
//import com.mdkj.feign.CourseFeign;
//import com.mdkj.feign.UserFeign;
//import com.mdkj.mapper.OrderDetailMapper;
//import com.mdkj.mapper.OrderMapper;
//import com.mdkj.util.ML;
//import com.mdkj.util.Result;
//import com.mdkj.util.ResultCode;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.rocketmq.spring.annotation.ConsumeMode;
//import org.apache.rocketmq.spring.annotation.MessageModel;
//import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
//import org.apache.rocketmq.spring.core.RocketMQListener;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//
//@Slf4j
//@Component
//@RocketMQMessageListener(
//        // 消费者组名
//        consumerGroup = "ml-consumer-group",
//        // 监听的主题名
//        topic = "ml-topic",
//        // 监听的标签名，默认为 `*` 表示监听全部标签
//        selectorExpression = "ml-tag",
//        // 并发模式CONCURRENTLY: 同组消费者并发消费消息，默认值
//        // 顺序模式ORDERLY: 同组消费者按顺序依次消费消息
//        consumeMode = ConsumeMode.CONCURRENTLY,
//        // 集群模式CLUSTERING: 同组消费者平均分摊消费消息，每人只需消费部分消息，默认值
//        // 广播模式BROADCASTING: 同组消费者每人都要将全部消息消费一遍
//        messageModel = MessageModel.CLUSTERING)
//public class OrderMessageListener implements RocketMQListener<OrderMessage> {
//
//    @Resource
//    private UserFeign userFeign;
//    @Resource
//    private CourseFeign courseFeign;
//    @Resource
//    private OrderMapper orderMapper;
//    @Resource
//    private OrderDetailMapper orderDetailMapper;
//
//    /**
//     * 该方法在Broker投递消息时触发并执行
//     *
//     * @param orderMessage 消息内容
//     */
//    @Override
//    public void onMessage(OrderMessage orderMessage) {
//
//        Long fkUserId = orderMessage.getFkUserId();
//        Long fkCourseId = orderMessage.getFkCourseId();
//        Double skPrice = orderMessage.getSkPrice();
//        Double price = orderMessage.getPrice();
//
//        log.info("{} 号用户成功秒杀到了 {} 号课程，共花费 {} 元", fkUserId, fkCourseId, skPrice);
//
//        // 准备实体类
//        Order order = new Order();
//        order.setSn(RandomUtil.randomNumbers(19));
//        order.setTotalAmount(price);
//        order.setPayAmount(skPrice);
//        order.setPayType(ML.Order.NO_PAY);
//        order.setStatus(ML.Order.UNPAID);
//        order.setFkUserId(fkUserId);
//        Result<User> userResult = userFeign.select(fkUserId);
//        if (ObjectUtil.isNull(userResult)) {
//            throw new ServiceException(ResultCode.OPEN_FEIGN_ERROR, "用户微服务远程调用失败，请联系管理员。");
//        }
//        User user = userResult.getData();
//        if (ObjectUtil.isNull(user)) {
//            throw new ServiceException(ResultCode.USER_NOT_FOUND, fkUserId + "号用户数据不存在");
//        }
//        order.setUsername(user.getUsername());
//        order.setInfo("通过秒杀活动下单");
//        order.setCreated(LocalDateTime.now());
//        order.setUpdated(LocalDateTime.now());
//
//        // DB添加订单表记录
//        if (orderMapper.insert(order) <= 0) {
//            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库添加订单失败");
//        }
//
//        // 添加订单明细表记录
//        Long orderId = order.getId();
//        OrderDetail orderDetail = new OrderDetail();
//        orderDetail.setFkCourseId(fkCourseId);
//        orderDetail.setFkOrderId(orderId);
//        Result<Course> courseResult = courseFeign.select(fkCourseId);
//        if (ObjectUtil.isNull(courseResult)) {
//            throw new ServiceException(ResultCode.OPEN_FEIGN_ERROR, "课程微服务远程调用失败，请联系管理员。");
//        }
//        Course course = courseResult.getData();
//        if (ObjectUtil.isNull(course)) {
//            throw new ServiceException(ResultCode.COURSE_NOT_FOUND, fkCourseId + "号课程数据不存在");
//        }
//        orderDetail.setCourseTitle(course.getTitle());
//        orderDetail.setCourseCover(course.getCover());
//        orderDetail.setCoursePrice(course.getPrice());
//        orderDetail.setCreated(LocalDateTime.now());
//        orderDetail.setUpdated(LocalDateTime.now());
//        if (orderDetailMapper.insert(orderDetail) <= 0) {
//            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库添加订单明细失败");
//        }
//    }
//}
