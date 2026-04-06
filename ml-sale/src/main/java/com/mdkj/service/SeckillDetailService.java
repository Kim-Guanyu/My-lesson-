package com.mdkj.service;

import com.mdkj.dto.SeckillDetailInsertDTO;
import com.mdkj.dto.SeckillDetailPageDTO;
import com.mdkj.dto.SeckillDetailUpdateDTO;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.service.IService;
import com.mdkj.entity.SeckillDetail;

import java.util.List;

/**
 * 秒杀明细表 服务层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public interface SeckillDetailService extends IService<SeckillDetail> {
    boolean insert(SeckillDetailInsertDTO dto);
    SeckillDetail select(Long id);
    PageVO<SeckillDetail> page(SeckillDetailPageDTO dto);
    boolean update(SeckillDetailUpdateDTO dto);
    boolean delete(Long id);
    boolean deleteBatch(List<Long> ids);


}
