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
     * 秒杀指定商品（用户身份从 Token 解析，价格从活动明细读取）
     *
     * @param token 登录 Token
     * @param dto   秒杀参数（活动 ID、课程 ID）
     * @return 订单编号
     */
    String kill(String token, KillDTO dto);

    /**
     * 压测准备：开启活动并重置 Redis 库存
     */
    boolean prepareLoadTest(Long seckillId, Integer stock);
}
