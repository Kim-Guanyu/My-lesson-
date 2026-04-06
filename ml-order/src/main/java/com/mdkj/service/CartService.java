package com.mdkj.service;

import com.mdkj.dto.CartInsertDTO;
import com.mdkj.dto.CartPageDTO;
import com.mdkj.dto.CartUpdateDTO;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.service.IService;
import com.mdkj.entity.Cart;

import java.util.List;

/**
 * 购物车表 服务层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public interface CartService extends IService<Cart> {
    boolean insert(CartInsertDTO dto);
    Cart select(Long id);
    PageVO<Cart> page(CartPageDTO dto);
    boolean update(CartUpdateDTO dto);
    boolean delete(Long id);
    boolean deleteBatch(List<Long> ids);
    boolean clearByUserId(Long userId);


}
