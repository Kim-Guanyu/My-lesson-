package com.mdkj.service;

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
    boolean update(OrderDetailUpdateDTO dto);
    boolean delete(Long id);
    boolean deleteBatch(List<Long> ids);
    List<OrderDetailExcelDTO> getExcelData();


}
