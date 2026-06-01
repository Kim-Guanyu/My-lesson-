package com.mdkj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mdkj.dto.*;
import com.mdkj.exception.ServiceException;
import com.mdkj.mapper.SeckillDetailMapper;
import com.mdkj.util.ML;
import com.mdkj.util.MyRedis;
import com.mdkj.util.ResultCode;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mdkj.entity.Seckill;
import com.mdkj.entity.SeckillDetail;
import com.mdkj.component.SeckillStockService;
import com.mdkj.component.TokenUserResolver;
import com.mdkj.entity.User;
import com.mdkj.mapper.SeckillMapper;
import com.mdkj.service.SeckillService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


import static com.mdkj.entity.table.SeckillDetailTableDef.SECKILL_DETAIL;
import static com.mdkj.entity.table.SeckillTableDef.SECKILL;
import static com.mybatisflex.core.query.QueryMethods.curDate;
import static com.mybatisflex.core.query.QueryMethods.date;


/**
 * 秒杀表 服务层实现。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Service
@Slf4j
public class SeckillServiceImpl extends ServiceImpl<SeckillMapper, Seckill>  implements SeckillService{

    @Resource
    private SeckillDetailMapper seckillDetailMapper;
    @Resource
    private MyRedis redis;
    @Resource
    private SeckillStockService seckillStockService;
    @Resource
    private TokenUserResolver tokenUserResolver;
    @Resource
    private SeckillMapper seckillMapper;
    @Resource
    private RocketMQTemplate rocketmqTemplate;

    @Override
    public boolean insert(SeckillInsertDTO dto) {
        String title = dto.getTitle();
        LocalDateTime startTime = dto.getStartTime();

        // 标题和开始时间查重
        // select count(*) from seckill where title = ? and start_time = ?
        if (QueryChain.of(mapper)
                .where(SECKILL.TITLE.eq(title))
                .and(SECKILL.START_TIME.eq(startTime))
                .exists()) {
            throw new ServiceException(ResultCode.SECKILL_REPEAT, startTime.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")) + title + "的秒杀活动重复");
        }

        // 组装实体类
        Seckill seckill = BeanUtil.copyProperties(dto, Seckill.class);
        seckill.setInfo(StrUtil.isEmpty(dto.getInfo()) ? "暂无描述。" : dto.getInfo());
        seckill.setCreated(LocalDateTime.now());
        seckill.setUpdated(LocalDateTime.now());

        // insert into seckill (title, info, start_time, end_time, status, created, updated) values (?, ?, ?, ?, ?, ?, ?)
        if (mapper.insert(seckill) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库添加失败");
        }
        return true;
    }

    @Override
    public Seckill select(Long id) {
        // select * from seckill where id = ?
        Seckill seckill = mapper.selectOneWithRelationsById(id);
        if (ObjectUtil.isNull(seckill)) {
            throw new ServiceException(ResultCode.SECKILL_NOT_FOUND, id + "号秒杀活动不存在");
        }
        return seckill;
    }

    @Override
    public List<SeckillSimpleListVO> simpleList() {
        // select * from seckill
        return QueryChain.of(mapper)
                .withRelations()
                .listAs(SeckillSimpleListVO.class);
    }

    @Override
    public PageVO<Seckill> page(SeckillPageDTO dto) {
        QueryChain<Seckill> queryChain = QueryChain.of(mapper);

        // title条件
        String title = dto.getTitle();
        if (ObjectUtil.isNotNull(title)) {
            queryChain.where(SECKILL.TITLE.like(title));
        }

        // DB分页并转为VO
        Page<Seckill> result = queryChain.withRelations().page(new Page<>(dto.getPageNum(), dto.getPageSize()));
        PageVO<Seckill> pageVO = new PageVO<>();
        BeanUtil.copyProperties(result, pageVO);
        pageVO.setPageNum(result.getPageNumber());
        return pageVO;
    }

    @Override
    public boolean update(SeckillUpdateDTO dto) {
        Long id = dto.getId();
        String title = dto.getTitle();

        // 检查秒杀活动是否存在
        this.existsById(id);

        // 标题查重
        // select count(*) from seckill where title = ? and id <> ?
        if (QueryChain.of(mapper)
                .where(SECKILL.TITLE.eq(title))
                .and(SECKILL.ID.ne(id))
                .exists()) {
            throw new ServiceException(ResultCode.TITLE_REPEAT, "标题" + title + "重复");
        }

        // 组装实体类
        Seckill seckill = BeanUtil.copyProperties(dto, Seckill.class);
        // update seckill set title = ?, info = ?, start_time = ?, end_time = ?, updated = ? where id = ?
        seckill.setUpdated(LocalDateTime.now());
        if (!UpdateChain.of(seckill)
                .where(SECKILL.ID.eq(id))
                .update()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库修改失败");
        }
        return true;
    }

    @Override
    public boolean delete(Long id) {

        // 检查秒杀活动是否存在
        this.existsById(id);

        // 按秒杀主键主键删除秒杀明细记录
        // delete from seckill_detail where fk_seckill_id = id
        UpdateChain.of(seckillDetailMapper)
                .where(SECKILL_DETAIL.FK_SECKILL_ID.eq(id))
                .remove();

        // 按秒杀主键删除一条秒杀记录
        // delete from seckill where id = id
        if (mapper.deleteById(id) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    @Override
    public boolean deleteBatch(List<Long> ids) {

        // 检查秒杀活动是否存在
        // select count(*) from seckill where id in (?, ?, ?)
        if (QueryChain.of(mapper)
                .where(SECKILL.ID.in(ids))
                .count() < ids.size()) {
            throw new ServiceException(ResultCode.SECKILL_NOT_FOUND, "至少一个秒杀活动数据不存在");
        }

        // 按秒杀主键批量删除秒杀明细记录
        // delete from seckill_detail where fk_seckill_id in (ids)
        UpdateChain.of(seckillDetailMapper)
                .where(SECKILL_DETAIL.FK_SECKILL_ID.in(ids))
                .remove();

        // 按秒杀主键批量删除秒杀记录
        // delete from seckill where id in (ids)
        if (mapper.deleteBatchByIds(ids) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    /**
     * 按主键检查秒杀活动是否存在，如果不存在则直接抛出异常
     *
     * @param id 秒杀活动主键
     */
    private void existsById(Long id) {
        // select count(*) from seckill where id = ?
        if (!QueryChain.of(mapper)
                .where(SECKILL.ID.eq(id))
                .exists()) {
            throw new ServiceException(ResultCode.SECKILL_NOT_FOUND, id + "号秒杀活动不存在");
        }
    }

    @Override
    public List<Seckill> today() {
        // 查询今日的秒杀活动数据，根据开始时间升序
        // select * from seckill where date(start_time) = curdate() order by start_time
        return QueryChain.of(mapper)
                .where(date(SECKILL.START_TIME).eq(curDate()))
                .orderBy(SECKILL.START_TIME.asc())
                .withRelations()
                .list();
    }


    @Override
    public String kill(String token, KillDTO dto) {
        User user = tokenUserResolver.requireUser(token);
        Long fkUserId = user.getId();
        Long fkSeckillId = dto.getFkSeckillId();
        Long fkCourseId = dto.getFkCourseId();

        if (!seckillStockService.tryAcquireUserRate(fkUserId, ML.Seckill.USER_KILL_RATE_PER_SECOND)) {
            throw new ServiceException(ResultCode.SECKILL_TOO_FAST, ResultCode.SECKILL_TOO_FAST.getMESSAGE());
        }

        Seckill seckill = seckillMapper.selectOneById(fkSeckillId);
        if (ObjectUtil.isNull(seckill)) {
            throw new ServiceException(ResultCode.SECKILL_NOT_FOUND, fkSeckillId + "号秒杀活动不存在");
        }

        Integer status = seckill.getStatus();
        if (status.equals(ML.Seckill.NOT_START)) {
            throw new ServiceException(ResultCode.SECKILL_NOT_START, fkSeckillId + "号秒杀活动未开始");
        }
        if (status.equals(ML.Seckill.ENDED)) {
            throw new ServiceException(ResultCode.SECKILL_END, fkSeckillId + "号秒杀活动已结束");
        }
        if (!status.equals(ML.Seckill.STARTED)) {
            throw new ServiceException(ResultCode.SERVER_ERROR, "秒杀活动状态异常");
        }

        SeckillDetail detail = QueryChain.of(seckillDetailMapper)
                .where(SECKILL_DETAIL.FK_SECKILL_ID.eq(fkSeckillId))
                .and(SECKILL_DETAIL.FK_COURSE_ID.eq(fkCourseId))
                .one();
        if (ObjectUtil.isNull(detail)) {
            throw new ServiceException(ResultCode.SECKILL_DETAIL_NOT_FOUND,
                    fkSeckillId + "号活动下不存在课程" + fkCourseId);
        }

        String existingSn = seckillStockService.getUserOrderSn(fkSeckillId, fkCourseId, fkUserId);
        if (StrUtil.isNotBlank(existingSn)) {
            log.info("用户 {} 重复秒杀请求，返回已有订单号 {}", fkUserId, existingSn);
            return existingSn;
        }

        String sn = RandomUtil.randomNumbers(19);
        long luaResult = seckillStockService.tryKill(fkSeckillId, fkCourseId, fkUserId, sn);

        if (luaResult == 0) {
            String reservedSn = seckillStockService.getUserOrderSn(fkSeckillId, fkCourseId, fkUserId);
            if (StrUtil.isNotBlank(reservedSn)) {
                return reservedSn;
            }
            throw new ServiceException(ResultCode.SERVER_ERROR, "秒杀状态异常，请稍后重试");
        }
        if (luaResult < 0) {
            throw new ServiceException(ResultCode.SECKILL_STOCK_OUT, ResultCode.SECKILL_STOCK_OUT.getMESSAGE());
        }

        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setSn(sn);
        orderMessage.setFkSeckillId(fkSeckillId);
        orderMessage.setFkUserId(fkUserId);
        orderMessage.setFkCourseId(fkCourseId);
        orderMessage.setSkPrice(detail.getSkPrice());
        orderMessage.setPrice(detail.getCoursePrice());

        try {
            rocketmqTemplate.convertAndSend("ml-topic:ml-tag", orderMessage);
            log.info("秒杀 MQ 发送成功，用户 {} 课程 {} 订单 {}", fkUserId, fkCourseId, sn);
        } catch (Exception e) {
            log.error("秒杀 MQ 发送失败，回滚库存", e);
            seckillStockService.rollbackKill(fkSeckillId, fkCourseId, fkUserId);
            throw new ServiceException(ResultCode.SERVER_ERROR, "秒杀下单失败，请稍后重试");
        }
        return sn;
    }

    @Override
    public boolean prepareLoadTest(Long seckillId, Integer stock) {
        Seckill seckill = seckillMapper.selectOneWithRelationsById(seckillId);
        if (ObjectUtil.isNull(seckill)) {
            throw new ServiceException(ResultCode.SECKILL_NOT_FOUND, seckillId + "号秒杀活动不存在");
        }
        List<SeckillDetail> details = seckill.getSeckillDetails();
        if (CollUtil.isEmpty(details)) {
            throw new ServiceException(ResultCode.SERVER_ERROR, "秒杀活动无商品明细");
        }

        UpdateChain.of(seckillMapper)
                .set(SECKILL.STATUS, ML.Seckill.STARTED)
                .set(SECKILL.UPDATED, LocalDateTime.now())
                .where(SECKILL.ID.eq(seckillId))
                .update();

        for (SeckillDetail detail : details) {
            int skCount = stock != null ? stock : detail.getSkCount();
            seckillStockService.initStock(seckillId, detail.getFkCourseId(), skCount);
            log.info("压测准备：活动 {} 课程 {} 库存重置为 {}", seckillId, detail.getFkCourseId(), skCount);
        }
        redis.deleteByPrefix(com.mdkj.util.SeckillRedisKeys.userOrderPrefix(seckillId));
        log.info("压测准备：秒杀活动 {} 已开启，用户占位已清理", seckillId);
        return true;
    }




}
