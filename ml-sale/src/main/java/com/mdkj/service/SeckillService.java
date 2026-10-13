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

    /**
     * 开启秒杀活动：状态置为「已开始」，并按明细配置的库存预热 Redis。
     * <p>
     * 不预热的话，小程序端点「立即秒杀」会因为 Redis 无库存而失败。
     *
     * @param seckillId 活动主键
     * @return 是否成功
     */
    boolean start(Long seckillId);

    /**
     * 结束秒杀活动：状态置为「已结束」。
     *
     * @param seckillId 活动主键
     * @return 是否成功
     */
    boolean stop(Long seckillId);
}
