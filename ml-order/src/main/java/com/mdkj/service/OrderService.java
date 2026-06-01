package com.mdkj.service;

import com.mdkj.dto.OrderInsertDTO;
import com.mdkj.dto.OrderMessage;
import com.mdkj.dto.OrderPageDTO;
import com.mdkj.dto.OrderUpdateDTO;
import com.mdkj.dto.PrePayDTO;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.service.IService;
import com.mdkj.entity.Order;

import java.util.List;
import java.util.Map;

/**
 * 订单表 服务层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public interface OrderService extends IService<Order> {
    boolean insert(OrderInsertDTO dto);
    Order select(Long id);
    PageVO<Order> page(OrderPageDTO dto);
    PageVO<Order> myPage(OrderPageDTO dto);
    boolean update(OrderUpdateDTO dto);
    boolean delete(Long id);
    boolean deleteBatch(List<Long> ids);
    Map<String, Object> statistics();
    String prePay(PrePayDTO dto);
    String createSeckillOrder(OrderMessage orderMessage);
    String findUnpaidSn(Long fkUserId, Long fkCourseId);
    void handleSeckillOrderTimeout(String sn, Long fkSeckillId, Long fkCourseId, Long fkUserId);
    Order getBySn(String sn);
    boolean updateStatusBySn(String sn, Integer status);
    boolean paySuccessBySn(String sn, Double payAmount);
    boolean checkStatusBySn(String sn);
}
