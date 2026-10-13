package com.mdkj.service;

import com.mdkj.dto.MyCoursePageDTO;
import com.mdkj.dto.OrderDetailExcelDTO;
import com.mdkj.dto.OrderDetailInsertDTO;
import com.mdkj.dto.OrderDetailPageDTO;
import com.mdkj.dto.OrderDetailUpdateDTO;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.service.IService;
import com.mdkj.entity.OrderDetail;

import java.util.List;

/**
 * 订单明细表 服务层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public interface OrderDetailService extends IService<OrderDetail> {
    boolean insert(OrderDetailInsertDTO dto);
    OrderDetail select(Long id);
    PageVO<OrderDetail> page(OrderDetailPageDTO dto);
    PageVO<OrderDetail> myCourses(MyCoursePageDTO dto);

    /**
     * 判断用户是否已购买某课程，即该课程是否出现在用户任意一张已付款订单的明细里。
     *
     * @param fkUserId   用户主键
     * @param fkCourseId 课程主键
     * @return {@code true} 已购买
     */
    boolean isPurchased(Long fkUserId, Long fkCourseId);

    /**
     * 从给定课程中筛出用户已购买的那些，供下单前批量校验使用。
     *
     * @param fkUserId  用户主键
     * @param courseIds 待校验的课程主键集合
     * @return 其中已购买的课程主键，未购买任何一门时返回空集合
     */
    List<Long> filterPurchased(Long fkUserId, List<Long> courseIds);

    boolean update(OrderDetailUpdateDTO dto);
    boolean delete(Long id);
    boolean deleteBatch(List<Long> ids);
    List<OrderDetailExcelDTO> getExcelData();


}
