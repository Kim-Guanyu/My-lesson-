package com.mdkj.service;

import com.mdkj.dto.CouponsInsertDTO;
import com.mdkj.dto.CouponsPageDTO;
import com.mdkj.dto.CouponsSimpleListVO;
import com.mdkj.dto.CouponsUpdateDTO;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.service.IService;
import com.mdkj.entity.Coupons;

import java.util.List;

/**
 * 优惠卷表 服务层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public interface CouponsService extends IService<Coupons> {
    boolean insert(CouponsInsertDTO dto);
    Coupons select(Long id);
    List<CouponsSimpleListVO> simpleList();
    PageVO<Coupons> page(CouponsPageDTO dto);
    boolean update(CouponsUpdateDTO dto);
    boolean delete(Long id);
    boolean deleteBatch(List<Long> ids);
    Coupons selectByCode(String code);

}
