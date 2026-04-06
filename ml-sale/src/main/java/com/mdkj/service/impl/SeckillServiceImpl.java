package com.mdkj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
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
import com.mdkj.mapper.SeckillMapper;
import com.mdkj.service.SeckillService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;


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
    private RedissonClient redissonClient;
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
    public boolean kill(KillDTO dto) {
        Long fkSeckillId = dto.getFkSeckillId();
        Long fkCourseId = dto.getFkCourseId();

        // 检查秒杀活动是否存在
        Seckill seckill = seckillMapper.selectOneById(fkSeckillId);
        if (ObjectUtil.isNull(seckill)) {
            throw new ServiceException(ResultCode.SECKILL_NOT_FOUND, fkSeckillId + "号秒杀活动不存在");
        }

        // 根据活动状态决定处理方案
        Integer status = seckill.getStatus();
        if (status.equals(ML.Seckill.NOT_START)) {
            throw new ServiceException(ResultCode.SECKILL_NOT_START, fkSeckillId + "号秒杀活动未开始");
        } else if (status.equals(ML.Seckill.STARTED)) {
            // 开始秒杀
            final String KEY = ML.Redis.SECKILL_COURSE_COUNT_PREFIX + fkCourseId;

            // 加锁：30秒后过期，线程存活状态下，每10秒自动续期一次
            RLock lock = redissonClient.getLock("skLock");
            lock.lock(30, TimeUnit.SECONDS);

            try {
                // 判断库存充足：扣减库存
                if (Integer.parseInt(redis.get(KEY)) > 0) {
                    redis.incr(KEY, -1);
                    // MQ发送到订单微服务
                    OrderMessage orderMessage = new OrderMessage();
                    orderMessage.setFkUserId(dto.getFkUserId());
                    orderMessage.setFkCourseId(fkCourseId);
                    orderMessage.setSkPrice(dto.getSkPrice());
                    orderMessage.setPrice(dto.getPrice());
                    rocketmqTemplate.convertAndSend("ml-topic:ml-tag", orderMessage);
                    log.info("MQ：订单消息发送成功！");
                    return true;
                } else {
                    throw new ServiceException(ResultCode.SERVER_ERROR, "库存不足，秒杀失败");
                }
            } finally {
                lock.unlock();
            }
        } else if (status.equals(ML.Seckill.ENDED)) {
            throw new ServiceException(ResultCode.SECKILL_END, fkSeckillId + "号秒杀活动已结束");
        } else {
            throw new ServiceException(ResultCode.SERVER_ERROR, "秒杀活动状态异常");
        }
    }




}
