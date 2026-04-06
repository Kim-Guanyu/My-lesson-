package com.mdkj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mdkj.dto.OrderInsertDTO;
import com.mdkj.dto.OrderPageDTO;
import com.mdkj.dto.OrderUpdateDTO;
import com.mdkj.dto.PrePayDTO;
import com.mdkj.entity.*;
import com.mdkj.exception.ServiceException;
import com.mdkj.feign.CourseFeign;
import com.mdkj.feign.UserFeign;
import com.mdkj.mapper.OrderDetailMapper;
import com.mdkj.service.CartService;
import com.mdkj.util.ML;
import com.mdkj.util.MyRedis;
import com.mdkj.util.Result;
import com.mdkj.util.ResultCode;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mdkj.mapper.OrderMapper;
import com.mdkj.service.OrderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.mdkj.entity.table.CartTableDef.CART;
import static com.mdkj.entity.table.OrderDetailTableDef.ORDER_DETAIL;
import static com.mdkj.entity.table.OrderTableDef.ORDER;
import static com.mybatisflex.core.query.QueryMethods.*;

/**
 * 订单表 服务层实现。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order>  implements OrderService{

    @Resource
    private UserFeign userFeign;
    @Resource
    private OrderDetailMapper orderDetailMapper;
    @Resource
    private MyRedis redis;
    @Resource
    private CourseFeign courseFeign;
    @Resource
    private CartService cartService;

    @Override
    public boolean insert(OrderInsertDTO dto) {
        Long fkUserId = dto.getFkUserId();

        // 组装实体类
        Order order = BeanUtil.copyProperties(dto, Order.class);
        order.setSn(RandomUtil.randomNumbers(19));
        Result<User> userResult = userFeign.select(fkUserId);
        if (ObjectUtil.isNull(userResult)) {
            throw new ServiceException(ResultCode.OPEN_FEIGN_ERROR, "用户微服务远程调用失败，请联系管理员。");
        }
        User user = userResult.getData();
        if (ObjectUtil.isNull(user)) {
            throw new ServiceException(ResultCode.USER_NOT_FOUND, fkUserId + "号用户数据不存在");
        }
        order.setUsername(user.getUsername());
        order.setInfo(StrUtil.isEmpty(dto.getInfo()) ? "暂无描述。" : dto.getInfo());
        order.setCreated(LocalDateTime.now());
        order.setUpdated(LocalDateTime.now());
        // insert into order (sn, total_amount, pay_amount, pay_type, info, status, fk_user_id, username, created, updated)
        // values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        if (mapper.insert(order) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库添加失败");
        }
        return false;
    }

    @Override
    public Order select(Long id) {
        // select * from order where id = ?
        Order order = mapper.selectOneWithRelationsById(id);
        if (ObjectUtil.isNull(order)) {
            throw new ServiceException(ResultCode.ORDER_NOT_FOUND, id + "号订单数据不存在");
        }
        return order;
    }

    @Override
    public PageVO<Order> page(OrderPageDTO dto) {
        QueryChain<Order> queryChain = QueryChain.of(mapper)
                .orderBy(ORDER.UPDATED.desc());

        // sn条件
        String sn = dto.getSn();
        if (ObjectUtil.isNotEmpty(sn)) {
            queryChain.where(ORDER.SN.like(sn));
        }

        // status条件
        Integer status = dto.getStatus();
        if (ObjectUtil.isNotNull(status)) {
            queryChain.where(ORDER.STATUS.eq(status));
        }

        // username条件
        String username = dto.getUsername();
        if (ObjectUtil.isNotNull(username)) {
            queryChain.where(ORDER.USERNAME.like(username));
        }

        // DB分页并转为VO
        Page<Order> result = queryChain.withRelations().page(new Page<>(dto.getPageNum(), dto.getPageSize()));
        PageVO<Order> pageVO = new PageVO<>();
        BeanUtil.copyProperties(result, pageVO);
        pageVO.setPageNum(result.getPageNumber());
        return pageVO;
    }

    @Override
    public boolean update(OrderUpdateDTO dto) {
        Long id = dto.getId();

        // 检查订单记录是否存在
        this.existsById(id);

        // 组装实体类
        Order order = BeanUtil.copyProperties(dto, Order.class);
        order.setUpdated(LocalDateTime.now());
        // update order set sn = ?, total_amount = ?, pay_amount = ?, pay_type = ?, info = ?, status = ?, fk_user_id = ?, username = ?, updated = ? where id = ?
        if (!UpdateChain.of(order)
                .where(ORDER.ID.eq(order.getId()))
                .update()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库修改失败");
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean delete(Long id) {

        // 检查订单记录是否存在
        this.existsById(id);

        // 按订单主键删除订单明细记录
        // delete from order_detail where fk_order_id = ?
        UpdateChain.of(orderDetailMapper)
                .where(ORDER_DETAIL.FK_ORDER_ID.eq(id))
                .remove();

        // 按订单主键删除一条订单记录
        // delete from `order` where id = ?
        if (mapper.deleteById(id) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteBatch(List<Long> ids) {

        // 检查订单记录是否存在
        // select count(*) from `order` where id in (?, ?, ?)
        if (QueryChain.of(mapper)
                .where(ORDER.ID.in(ids))
                .count() < ids.size()) {
            throw new ServiceException(ResultCode.ORDER_NOT_FOUND, "至少一个订单记录不存在");
        }

        // 按订单主键批量删除订单明细记录
        // delete from order_detail where fk_order_id in (?, ?, ?)
        UpdateChain.of(orderDetailMapper)
                .where(ORDER_DETAIL.FK_ORDER_ID.in(ids))
                .remove();

        // 按订单主键批量删除订单记录
        // delete from `order` where id in (?, ?, ?)
        if (mapper.deleteBatchByIds(ids) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    /**
     * 按主键检查订单记录是否存在，如果不存在则直接抛出异常
     *
     * @param id 订单记录主键
     */
    private void existsById(Long id) {
        // select count(*) from `order` where id = ?
        if (!QueryChain.of(mapper)
                .where(ORDER.ID.eq(id))
                .exists()) {
            throw new ServiceException(ResultCode.ORDER_NOT_FOUND, id + "号订单记录不存在");
        }
    }


    @Override
    public Map<String, Object> statistics() {

        // 尝试从缓存中获取统计数据，若存在则直接返回
        String dataFromRedis = redis.get(ML.Redis.ORDER_STATISTICS_DATA_KEY);
        if (ObjectUtil.isNotNull(dataFromRedis)) {
            return JSONUtil.parseObj(dataFromRedis);
        }

        Map<String, Object> result = new HashMap<>();

        // 统计订单支付方式比例
        // select pay_type as name, count(*) as value from `order` group by pay_type
        result.put("payTypeCount", mapper.selectListByQueryAs(QueryWrapper.create()
                .select(ORDER.PAY_TYPE.as("name"), QueryMethods.count().as("value"))
                .groupBy(ORDER.PAY_TYPE)
                .orderBy(ORDER.PAY_TYPE.asc()), Map.class));

        // 统计今日订单数
        // select count(*) from `order` where datediff(curdate(), date_format(created, '%Y-%m-%d')) = 0
        double todayCount = QueryChain.of(mapper)
                .where(dateDiff(currentDate(), dateFormat(ORDER.CREATED, "%Y-%m-%d")).eq(0))
                .count();

        // 统计昨日订单数
        // select count(*) from `order` where datediff(curdate(), date_format(created, '%Y-%m-%d')) = 1
        double yesterdayCount = QueryChain.of(mapper)
                .where(dateDiff(currentDate(), dateFormat(ORDER.CREATED, "%Y-%m-%d")).eq(1))
                .count();

        // 统计今年订单数
        // select count(*) from `order` where year(created) = year(current_date);
        double thisYearCount = QueryChain.of(mapper)
                .where(year(ORDER.CREATED).eq(year(currentDate())))
                .count();

        // 统计去年订单总数
        // select count(*) from `order` where year(created) - year(current_date) = -1;
        double lastYearCount = QueryChain.of(mapper)
                .where(year(ORDER.CREATED).subtract(year(currentDate())).eq(-1))
                .count();

        result.put("todayCount", todayCount);
        result.put("yesterdayCount", yesterdayCount);
        result.put("dayIncrease", increase(todayCount, yesterdayCount));
        result.put("thisYearCount", thisYearCount);
        result.put("lastYearCount", lastYearCount);
        result.put("yearIncrease", increase(thisYearCount, lastYearCount));

        // 加入Redis缓存，2 个小时后过期
        redis.setEx(ML.Redis.ORDER_STATISTICS_DATA_KEY, JSONUtil.toJsonStr(result), 2, TimeUnit.HOURS);

        return result;
    }

    /**
     * 计算a到b的增长率
     *
     * @param a 第一个操作数
     * @param b 第二个操作数
     * @return 保留两位小数的增长率
     */
    private String increase(double a, double b) {
        if (b == 0) {
            return a > b ? "100.00" : a < b ? "-100.00" : "0";
        }
        return String.format("%.2f", (a - b) / b);
    }




    @Transactional(rollbackFor = Exception.class)
    @Override
    public String prePay(PrePayDTO dto) {
        Long userId = dto.getFkUserId();
        List<Long> courseIds = dto.getCourseIds();

        // 获取该用户的全部订单ID集合
        // select id from order where fk_user_id = ?
        List<Long> orderIds = QueryChain.of(mapper)
                .select(ORDER.ID)
                .where(ORDER.FK_USER_ID.eq(userId))
                .listAs(Long.class);
        // 如果该用户存在订单记录，需要判断是否存在重复购买课程现象
        if (CollUtil.isNotEmpty(orderIds)) {
            // 获取该用户的全部已购买的课程ID集合
            // select fk_course_id from order_detail where fk_order_id in ?
            List<Long> purchasedCourseIds = QueryChain.of(orderDetailMapper)
                    .select(ORDER_DETAIL.FK_COURSE_ID)
                    .where(ORDER_DETAIL.FK_ORDER_ID.in(orderIds))
                    .listAs(Long.class);
            // 判断是否存在重复购买课程现象：有交集说明重复购买
            purchasedCourseIds.retainAll(courseIds);
            if (CollUtil.isNotEmpty(purchasedCourseIds)) {
                throw new ServiceException(ResultCode.ORDER_DETAIL_REPEAT, "订单明细重复");
            }
        }

        // 组装 entity 实体类
        Order order = BeanUtil.copyProperties(dto, Order.class);
        String sn = RandomUtil.randomNumbers(19);
        order.setSn(sn);
        order.setPayType(ML.Order.NO_PAY);
        order.setStatus(ML.Order.UNPAID);
        order.setPayAmount(0.0);
        order.setInfo("暂无描述。");
        Result<User> userResult = userFeign.select(userId);
        if (ObjectUtil.isNull(userResult)) {
            throw new ServiceException(ResultCode.OPEN_FEIGN_ERROR, "用户微服务远程调用失败，请联系管理员。");
        }
        User user = userResult.getData();
        if (ObjectUtil.isNull(user)) {
            throw new ServiceException(ResultCode.USER_NOT_FOUND, userId + "号用户数据不存在");
        }
        order.setUsername(user.getUsername());
        order.setCreated(LocalDateTime.now());
        order.setUpdated(LocalDateTime.now());

        // DB添加订单表记录
        if (mapper.insert(order) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库添加失败");
        }

        // 批量添加订单明细表记录
        Long orderId = order.getId();
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (Long courseId : courseIds) {
            Result<Course> courseResult = courseFeign.select(courseId);
            if (ObjectUtil.isNull(courseResult)) {
                throw new ServiceException(ResultCode.OPEN_FEIGN_ERROR, "课程微服务远程调用失败，请联系管理员。");
            }
            Course course = courseResult.getData();
            if (ObjectUtil.isNull(course)) {
                throw new ServiceException(ResultCode.COURSE_NOT_FOUND, courseId + "号课程数据不存在");
            }

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setFkCourseId(courseId);
            orderDetail.setFkOrderId(orderId);
            orderDetail.setCourseTitle(course.getTitle());
            orderDetail.setCourseCover(course.getCover());
            orderDetail.setCoursePrice(course.getPrice());
            orderDetail.setCreated(LocalDateTime.now());
            orderDetail.setUpdated(LocalDateTime.now());
            orderDetails.add(orderDetail);
        }
        if (orderDetailMapper.insertBatch(orderDetails) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库添加失败");
        }

        // 删除购物车表记录（不做保护，因为有可能不从购物车进行下单）
        UpdateChain.of(Cart.class)
                .where(CART.FK_USER_ID.eq(userId))
                .and(CART.FK_COURSE_ID.in(courseIds))
                .remove();

        // todo 将订单发送到MQ，延迟15分钟后取出，若仍然是未支付状态，则设置为已超时
        return sn;
    }

    @Override
    public boolean updateStatusBySn(String sn, Integer status) {
        // 根据订单编号更新订单状态
        if (!UpdateChain.of(mapper)
                .set(ORDER.STATUS, status)
                .set(ORDER.UPDATED, LocalDateTime.now())
                .where(ORDER.SN.eq(sn))
                .update()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "更新订单状态失败");
        }
        return true;
    }

    @Override
    public boolean checkStatusBySn(String sn) {
        // 根据订单编号查询订单记录（仅查询支付成功的订单）
        Order order = QueryChain.of(mapper)
                .where(ORDER.SN.eq(sn))
                .and(ORDER.STATUS.eq(ML.Order.PAID))
                .one();
        return ObjectUtil.isNotNull(order);
    }






}
