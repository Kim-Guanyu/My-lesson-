package com.mdkj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.mdkj.dto.OrderDetailExcelDTO;
import com.mdkj.dto.OrderDetailInsertDTO;
import com.mdkj.dto.OrderDetailPageDTO;
import com.mdkj.dto.OrderDetailUpdateDTO;
import com.mdkj.entity.Course;
import com.mdkj.entity.Order;
import com.mdkj.exception.ServiceException;
import com.mdkj.feign.CourseFeign;
import com.mdkj.util.ML;
import com.mdkj.util.Result;
import com.mdkj.util.ResultCode;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mdkj.entity.OrderDetail;
import com.mdkj.mapper.OrderDetailMapper;
import com.mdkj.service.OrderDetailService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.mdkj.entity.table.OrderDetailTableDef.ORDER_DETAIL;

/**
 * 订单明细表 服务层实现。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail>  implements OrderDetailService{
    @Resource
    private CourseFeign courseFeign;

    @Override
    public boolean insert(OrderDetailInsertDTO dto) {
        Long fkOrderId = dto.getFkOrderId();
        Long fkCourseId = dto.getFkCourseId();

        // 订单明细记录查重
        // select count(*) from order_detail where fk_order_id = ? and fk_course_id = ?
        if (QueryChain.of(mapper)
                .where(ORDER_DETAIL.FK_ORDER_ID.eq(fkOrderId))
                .and(ORDER_DETAIL.FK_COURSE_ID.eq(fkCourseId))
                .exists()) {
            throw new ServiceException(ResultCode.ORDER_DETAIL_REPEAT, "订单明细已重复，" + fkOrderId + "号订单已经添加了" + fkCourseId + "号课程");
        }

        // 组装实体类
        OrderDetail orderDetail = BeanUtil.copyProperties(dto, OrderDetail.class);
        Result<Course> courseResult = courseFeign.select(fkCourseId);
        if (ObjectUtil.isNull(courseResult)) {
            throw new ServiceException(ResultCode.OPEN_FEIGN_ERROR, "课程微服务远程调用失败，请联系管理员。");
        }
        Course course = courseResult.getData();
        if (ObjectUtil.isNull(course)) {
            throw new ServiceException(ResultCode.COURSE_NOT_FOUND, fkCourseId + "号课程数据不存在");
        }
        orderDetail.setCourseTitle(course.getTitle());
        orderDetail.setCoursePrice(course.getPrice());
        orderDetail.setCourseCover(course.getCover());
        orderDetail.setCreated(LocalDateTime.now());
        orderDetail.setUpdated(LocalDateTime.now());

        // insert into order_detail (fk_order_id, fk_course_id, sn, course_title, course_cover, course_price, created, updated) values (?, ?, ?, ?, ?, ?, ?, ?)
        if (mapper.insert(orderDetail) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库添加失败");
        }
        return true;
    }

    @Override
    public OrderDetail select(Long id) {
        // select * from order_detail where id = ?
        OrderDetail orderDetail = mapper.selectOneWithRelationsById(id);
        if (orderDetail == null) {
            throw new ServiceException(ResultCode.ORDER_DETAIL_NOT_FOUND, id + "号订单明细数据不存在");
        }
        return orderDetail;
    }

    @Override
    public PageVO<OrderDetail> page(OrderDetailPageDTO dto) {
        QueryChain<OrderDetail> queryChain = QueryChain.of(mapper).orderBy(ORDER_DETAIL.UPDATED.desc());

        // courseTitle条件
        String courseTitle = dto.getCourseTitle();
        if (ObjectUtil.isNotNull(courseTitle)) {
            queryChain.where(ORDER_DETAIL.COURSE_TITLE.like(courseTitle));
        }

        // DB分页并转为VO
        Page<OrderDetail> result = queryChain.withRelations().page(new Page<>(dto.getPageNum(), dto.getPageSize()));
        PageVO<OrderDetail> pageVO = new PageVO<>();
        BeanUtil.copyProperties(result, pageVO);
        pageVO.setPageNum(result.getPageNumber());
        return pageVO;
    }

    @Override
    public boolean update(OrderDetailUpdateDTO dto) {
        Long id = dto.getId();
        Long fkOrderId = dto.getFkOrderId();
        Long fkCourseId = dto.getFkCourseId();

        // 检查订单明细记录是否存在
        this.existsById(id);

        // 订单明细记录查重
        // select count(*) from order_detail where fk_order_id = ? and fk_course_id = ? and id <> ?
        if (QueryChain.of(mapper)
                .where(ORDER_DETAIL.FK_ORDER_ID.eq(dto.getFkOrderId()))
                .and(ORDER_DETAIL.FK_COURSE_ID.eq(dto.getFkCourseId()))
                .and(ORDER_DETAIL.ID.ne(dto.getId()))
                .exists()) {
            throw new ServiceException(ResultCode.ORDER_DETAIL_REPEAT, "订单明细已重复，" + fkOrderId + "号订单已经添加了" + fkCourseId + "号课程");
        }

        // 组装实体类
        OrderDetail orderDetail = BeanUtil.copyProperties(dto, OrderDetail.class);
        Result<Course> courseResult = courseFeign.select(fkCourseId);
        if (ObjectUtil.isNull(courseResult)) {
            throw new ServiceException(ResultCode.OPEN_FEIGN_ERROR, "课程微服务远程调用失败，请联系管理员。");
        }
        Course course = courseResult.getData();
        if (ObjectUtil.isNull(course)) {
            throw new ServiceException(ResultCode.COURSE_NOT_FOUND, fkCourseId + "号课程数据不存在");
        }
        orderDetail.setCourseTitle(course.getTitle());
        orderDetail.setCoursePrice(course.getPrice());
        orderDetail.setCourseCover(course.getCover());
        orderDetail.setUpdated(LocalDateTime.now());

        // update order_detail set fk_order_id = ?, fk_course_id = ?, sn = ?, course_title = ?, course_cover = ?, course_price = ?, updated = ? where id = ?
        if (!UpdateChain.of(orderDetail)
                .where(ORDER_DETAIL.ID.eq(orderDetail.getId()))
                .update()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库修改失败");
        }
        return true;
    }

    @Override
    public boolean delete(Long id) {

        // 检查订单明细记录是否存在
        this.existsById(id);

        // delete from order_detail where id = ?
        if (mapper.deleteById(id) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    @Override
    public boolean deleteBatch(List<Long> ids) {

        // 检查订单明细记录是否存在
        // select count(*) from order_detail where id in (?, ?, ?)
        if (QueryChain.of(mapper)
                .where(ORDER_DETAIL.ID.in(ids))
                .count() < ids.size()) {
            throw new ServiceException(ResultCode.ORDER_DETAIL_NOT_FOUND, "至少一个订单明细记录不存在");
        }

        // delete from order_detail where id in (?, ?, ?)
        if (mapper.deleteBatchByIds(ids) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    /**
     * 按主键检查订单明细记录是否存在，如果不存在则直接抛出异常
     *
     * @param id 订单明细记录主键
     */
    private void existsById(Long id) {
        // select count(*) from order_detail where id = ?
        if (!QueryChain.of(mapper)
                .where(ORDER_DETAIL.ID.eq(id))
                .exists()) {
            throw new ServiceException(ResultCode.ORDER_DETAIL_NOT_FOUND, id + "号订单明细记录不存在");
        }
    }

    @Override
    public List<OrderDetailExcelDTO> getExcelData() {

        // 查询全部订单明细记录
        // select * from order_detail
        List<OrderDetail> orderDetails = mapper.selectAllWithRelations();

        // 类型转换：List<OrderDetail> -> List<OrderDetailExcelDTO>
        List<OrderDetailExcelDTO> result = new ArrayList<>();
        orderDetails.forEach(orderDetail -> {
            OrderDetailExcelDTO excel = BeanUtil.copyProperties(orderDetail, OrderDetailExcelDTO.class);
            Order order = orderDetail.getOrder();
            excel.setSn(order.getSn());
            excel.setTotalAmount(order.getTotalAmount());
            excel.setPayAmount(order.getPayAmount());
            excel.setPayType(ML.Order.payTypeFormat(order.getPayType()));
            excel.setInfo(order.getInfo());
            excel.setStatus(ML.Order.statusFormat(order.getStatus()));
            excel.setUsername(order.getUsername());
            excel.setCreated(order.getCreated());
            excel.setUpdated(order.getUpdated());
            result.add(excel);
        });
        return result;
    }


}
