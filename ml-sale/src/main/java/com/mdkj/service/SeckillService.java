package com.mdkj.service;

import com.mdkj.dto.*;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.service.IService;
import com.mdkj.entity.Seckill;

import java.util.List;

/**
 * 秒杀表 服务层。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
public interface SeckillService extends IService<Seckill> {
    boolean insert(SeckillInsertDTO dto);
    Seckill select(Long id);
    List<SeckillSimpleListVO> simpleList();
    PageVO<Seckill> page(SeckillPageDTO dto);
    boolean update(SeckillUpdateDTO dto);
    boolean delete(Long id);
    boolean deleteBatch(List<Long> ids);
    List<Seckill> today();
    /**
     * 秒杀指定商品
     *
     * @param dto 秒杀实体类
     * @return 今日的秒杀活动数据
     */
    boolean kill(KillDTO dto);


}
