package com.mdkj.component;

import lombok.Getter;
import lombok.Setter;

/**
 * 秒杀本地事务上下文：作为 {@code sendMessageInTransaction} 的 arg 传入事务监听器，
 * 并由监听器把 Lua 扣减的三态结果回填，供 {@code kill()} 映射成对外的错误码。
 *
 * <p><b>为什么这样能拿到回填值</b>：{@code DefaultMQProducerImpl#sendMessageInTransaction}
 * 是在调用线程里<b>内联同步</b>调用 {@code executeLocalTransaction} 的（不走线程池，
 * 线程池只用于 broker 发起的回查）。所以发送方法返回后，{@link #luaResult} 必然已被写入。</p>
 *
 * @author Kim-Guanyu
 * @since v2.1.0
 */
@Getter
public class SeckillTxContext {

    /** Lua 扣减成功 */
    public static final long LUA_OK = 1L;
    /** 用户已占位，是重复请求 */
    public static final long LUA_DUPLICATE = 0L;
    /** 库存不足 */
    public static final long LUA_SOLD_OUT = -1L;
    /** 哨兵：本地事务压根没执行（半消息发送失败），或执行时抛了异常 */
    public static final long LUA_NOT_EXECUTED = Long.MIN_VALUE;

    private final Long seckillId;
    private final Long courseId;
    private final Long userId;
    /** 本次请求预生成的订单号，同时也是用户占位 Key 的值 */
    private final String sn;
    /** 秒杀价，用于写预支付快照；以服务端活动明细为准，不信任客户端 */
    private final Double skPrice;

    /** 由 {@code SeckillTxListener#executeLocalTransaction} 回填 */
    @Setter
    private long luaResult = LUA_NOT_EXECUTED;

    public SeckillTxContext(Long seckillId, Long courseId, Long userId, String sn, Double skPrice) {
        this.seckillId = seckillId;
        this.courseId = courseId;
        this.userId = userId;
        this.sn = sn;
        this.skPrice = skPrice;
    }
}
