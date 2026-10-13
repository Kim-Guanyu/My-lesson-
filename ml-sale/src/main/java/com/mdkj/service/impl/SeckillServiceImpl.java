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
import com.mdkj.component.SeckillTxContext;
import com.mdkj.component.TokenUserResolver;
import com.mdkj.config.SeckillTxProducerConfig;
import com.mdkj.entity.User;
import com.mdkj.mapper.SeckillMapper;
import com.mdkj.service.SeckillService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;


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
    /**
     * 秒杀事务消息专用 Template（独立 producer group，见 {@link SeckillTxProducerConfig}）。
     * 字段名必须与 Bean 名一致：容器里存在两个 RocketMQTemplate，
     * {@code @Resource} 先按字段名匹配，名字对不上会退化成按类型注入而报 NoUniqueBeanDefinitionException。
     */
    @Resource
    private RocketMQTemplate seckillTxRocketMQTemplate;

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

        // 活动状态优先读 Redis 短 TTL 缓存，未命中才查库并回填，避免每次 kill 请求都打 MySQL
        Integer status = seckillStockService.getCachedStatus(fkSeckillId);
        if (status == null) {
            Seckill seckill = seckillMapper.selectOneById(fkSeckillId);
            if (ObjectUtil.isNull(seckill)) {
                throw new ServiceException(ResultCode.SECKILL_NOT_FOUND, fkSeckillId + "号秒杀活动不存在");
            }
            status = seckill.getStatus();
            seckillStockService.cacheStatus(fkSeckillId, status);
        }
        if (status.equals(ML.Seckill.NOT_START)) {
            throw new ServiceException(ResultCode.SECKILL_NOT_START, fkSeckillId + "号秒杀活动未开始");
        }
        if (status.equals(ML.Seckill.ENDED)) {
            throw new ServiceException(ResultCode.SECKILL_END, fkSeckillId + "号秒杀活动已结束");
        }
        if (!status.equals(ML.Seckill.STARTED)) {
            throw new ServiceException(ResultCode.SERVER_ERROR, "秒杀活动状态异常");
        }

        // 商品明细（标题/封面/价格）同样优先读 Redis 缓存，未命中才查库并回填
        String courseTitle;
        String courseCover;
        Double coursePrice;
        Double skPrice;
        Map<Object, Object> cachedDetail = seckillStockService.getCachedDetail(fkSeckillId, fkCourseId);
        if (!cachedDetail.isEmpty()) {
            courseTitle = (String) cachedDetail.get("courseTitle");
            courseCover = (String) cachedDetail.get("courseCover");
            coursePrice = Double.valueOf((String) cachedDetail.get("coursePrice"));
            skPrice = Double.valueOf((String) cachedDetail.get("skPrice"));
        } else {
            SeckillDetail detail = QueryChain.of(seckillDetailMapper)
                    .where(SECKILL_DETAIL.FK_SECKILL_ID.eq(fkSeckillId))
                    .and(SECKILL_DETAIL.FK_COURSE_ID.eq(fkCourseId))
                    .one();
            if (ObjectUtil.isNull(detail)) {
                throw new ServiceException(ResultCode.SECKILL_DETAIL_NOT_FOUND,
                        fkSeckillId + "号活动下不存在课程" + fkCourseId);
            }
            courseTitle = detail.getCourseTitle();
            courseCover = detail.getCourseCover();
            coursePrice = detail.getCoursePrice();
            skPrice = detail.getSkPrice();
            seckillStockService.cacheDetail(fkSeckillId, fkCourseId, courseTitle, courseCover, coursePrice, skPrice);
        }

        // 只读预检：库存已空或用户已占位时直接返回，不往 broker 写半消息。
        // 事务消息把「先扣减再发送」倒转成「先发半消息再扣减」，不预检的话
        // 1000 打 100 时约九成注定失败的请求也要先写半消息再回滚，broker 写入放大约 10 倍。
        // 它不是权威判据，真正的裁决始终在事务内的 Lua。
        long preCheck = seckillStockService.preCheckKill(fkSeckillId, fkCourseId, fkUserId);
        if (preCheck == SeckillTxContext.LUA_DUPLICATE) {
            String reservedSn = seckillStockService.getUserOrderSn(fkSeckillId, fkCourseId, fkUserId);
            if (StrUtil.isNotBlank(reservedSn)) {
                log.info("用户 {} 重复秒杀请求（预检命中），返回已有订单号 {}", fkUserId, reservedSn);
                return reservedSn;
            }
            // 预检与读占位之间占位被补偿掉了，继续走事务消息让 Lua 重新裁决
        } else if (preCheck == SeckillTxContext.LUA_SOLD_OUT) {
            throw new ServiceException(ResultCode.SECKILL_STOCK_OUT, ResultCode.SECKILL_STOCK_OUT.getMESSAGE());
        }

        String sn = RandomUtil.randomNumbers(19);

        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setSn(sn);
        orderMessage.setFkSeckillId(fkSeckillId);
        orderMessage.setFkUserId(fkUserId);
        orderMessage.setFkCourseId(fkCourseId);
        orderMessage.setSkPrice(skPrice);
        orderMessage.setPrice(coursePrice);
        // 携带用户名/课程信息快照，ml-order 消费端建单时可直接使用，无需再同步调用 user/course 微服务
        orderMessage.setUsername(user.getUsername());
        orderMessage.setCourseTitle(courseTitle);
        orderMessage.setCourseCover(courseCover);

        // 事务消息：先发对消费端不可见的半消息，再在本地事务里做 Lua 扣减与预支付快照，
        // 最后由本地事务的返回值决定这条消息是投递还是丢弃（见 SeckillTxListener）。
        // 旧实现是「先扣减再发送、发送异常就回滚库存」，而网络超时的语义是「结果未知」而非「失败」，
        // 把未知当失败回滚，会在消息其实已投递时把库存白还回去，造出「订单有效但库存已释放」的悬空单。
        SeckillTxContext txContext = new SeckillTxContext(fkSeckillId, fkCourseId, fkUserId, sn, skPrice);
        TransactionSendResult sendResult;
        try {
            sendResult = seckillTxRocketMQTemplate.sendMessageInTransaction(
                    "ml-topic:ml-tag",
                    MessageBuilder.withPayload(orderMessage)
                            // 带上订单号做消息 KEY，rocketmq-dashboard 可按 sn 直接检索这条消息，
                            // 排查「某一单的半消息到底提交没有」时最快。对消费端无影响。
                            .setHeader(RocketMQHeaders.KEYS, sn)
                            .build(),
                    txContext);
        } catch (Exception e) {
            // 半消息都没发出去 → 本地事务根本没执行 → 库存分毫未动 → 无需任何补偿。
            // 这正是事务消息相对旧实现的关键差别：不存在「扣了但不知道消息发没发出去」的中间态。
            log.error("秒杀半消息发送失败，本地事务未执行，库存未变动，订单 {}", sn, e);
            throw new ServiceException(ResultCode.SERVER_ERROR, "秒杀下单失败，请稍后重试");
        }

        long luaResult = txContext.getLuaResult();
        log.info("秒杀事务消息返回：用户 {} 课程 {} 订单 {}，本地事务状态={}，Lua={}",
                fkUserId, fkCourseId, sn, sendResult.getLocalTransactionState(), luaResult);

        if (luaResult == SeckillTxContext.LUA_OK) {
            // 库存已扣。即便提交 ack 丢失导致这里的状态不是 COMMIT，broker 也会回查，
            // 而占位值此刻就等于 sn，回查必然判定提交，消息终将投递。对用户而言这一单已经成立。
            return sn;
        }
        if (luaResult == SeckillTxContext.LUA_DUPLICATE) {
            String reservedSn = seckillStockService.getUserOrderSn(fkSeckillId, fkCourseId, fkUserId);
            if (StrUtil.isNotBlank(reservedSn)) {
                log.info("用户 {} 重复秒杀请求，返回已有订单号 {}", fkUserId, reservedSn);
                return reservedSn;
            }
            throw new ServiceException(ResultCode.SERVER_ERROR, "秒杀状态异常，请稍后重试");
        }
        if (luaResult == SeckillTxContext.LUA_SOLD_OUT) {
            throw new ServiceException(ResultCode.SECKILL_STOCK_OUT, ResultCode.SECKILL_STOCK_OUT.getMESSAGE());
        }

        // 本地事务执行时抛了异常，库存到底扣没扣未知：占位值是唯一可信依据。
        // 若占位就是本次的 sn，说明扣减其实成功了，回查也会提交，照常把订单号返回给用户。
        String heldSn = seckillStockService.getUserOrderSn(fkSeckillId, fkCourseId, fkUserId);
        if (sn.equals(heldSn)) {
            log.warn("秒杀本地事务状态未知但占位已持有本单，按成功返回：订单 {}", sn);
            return sn;
        }
        throw new ServiceException(ResultCode.SERVER_ERROR, "秒杀下单失败，请稍后重试");
    }

    @Override
    public boolean prepareLoadTest(Long seckillId, Integer stock) {
        Seckill seckill = requireSeckillWithDetails(seckillId);

        markStatus(seckillId, ML.Seckill.STARTED);
        preheat(seckill, stock);

        redis.deleteByPrefix(com.mdkj.util.SeckillRedisKeys.userOrderPrefix(seckillId));
        log.info("压测准备：秒杀活动 {} 已开启，用户占位已清理", seckillId);
        return true;
    }

    @Override
    public boolean start(Long seckillId) {
        // 必须先配好商品明细，否则活动开了也抢不到东西
        Seckill seckill = requireSeckillWithDetails(seckillId);

        markStatus(seckillId, ML.Seckill.STARTED);
        // 用明细里配置的 skCount 作为库存，不覆盖
        preheat(seckill, null);
        seckillStockService.cacheStatus(seckillId, ML.Seckill.STARTED);

        log.info("秒杀活动 {} 已开启，共预热 {} 个商品", seckillId, seckill.getSeckillDetails().size());
        return true;
    }

    @Override
    public boolean stop(Long seckillId) {
        this.existsById(seckillId);

        markStatus(seckillId, ML.Seckill.ENDED);
        seckillStockService.cacheStatus(seckillId, ML.Seckill.ENDED);

        log.info("秒杀活动 {} 已结束", seckillId);
        return true;
    }

    /**
     * 查出活动及其商品明细，活动不存在或没有明细时抛业务异常。
     */
    private Seckill requireSeckillWithDetails(Long seckillId) {
        Seckill seckill = seckillMapper.selectOneWithRelationsById(seckillId);
        if (ObjectUtil.isNull(seckill)) {
            throw new ServiceException(ResultCode.SECKILL_NOT_FOUND, seckillId + "号秒杀活动不存在");
        }
        if (CollUtil.isEmpty(seckill.getSeckillDetails())) {
            throw new ServiceException(ResultCode.SERVER_ERROR, "秒杀活动无商品明细，请先添加秒杀课程");
        }
        return seckill;
    }

    /**
     * 更新活动状态。
     */
    private void markStatus(Long seckillId, Integer status) {
        UpdateChain.of(seckillMapper)
                .set(SECKILL.STATUS, status)
                .set(SECKILL.UPDATED, LocalDateTime.now())
                .where(SECKILL.ID.eq(seckillId))
                .update();
    }

    /**
     * 预热 Redis：写入每个商品的库存与明细快照。
     *
     * @param overrideStock 覆盖库存，为 null 时用明细里配置的 skCount（压测场景才会传值）
     */
    private void preheat(Seckill seckill, Integer overrideStock) {
        Long seckillId = seckill.getId();
        for (SeckillDetail detail : seckill.getSeckillDetails()) {
            Long fkCourseId = detail.getFkCourseId();
            int skCount = overrideStock != null ? overrideStock : detail.getSkCount();

            seckillStockService.initStock(seckillId, fkCourseId, skCount);
            seckillStockService.cacheDetail(seckillId, fkCourseId,
                    detail.getCourseTitle(), detail.getCourseCover(),
                    detail.getCoursePrice(), detail.getSkPrice());

            log.info("预热：活动 {} 课程 {} 库存置为 {}", seckillId, fkCourseId, skCount);
        }
    }




}
