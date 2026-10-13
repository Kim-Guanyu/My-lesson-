package com.mdkj.component;

import cn.hutool.json.JSONUtil;
import com.mdkj.dto.OrderMessage;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeckillTxListenerTest {

    private static final String SN = "2026091800000000001";

    @Mock
    private SeckillStockService seckillStockService;
    @InjectMocks
    private SeckillTxListener listener;

    private SeckillTxContext context() {
        return new SeckillTxContext(1L, 2L, 3L, SN, 9.9);
    }

    @SuppressWarnings("rawtypes")
    private Message checkMessage(String sn, Long seckillId, Long courseId, Long userId) {
        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setSn(sn);
        orderMessage.setFkSeckillId(seckillId);
        orderMessage.setFkCourseId(courseId);
        orderMessage.setFkUserId(userId);
        // 回查时 RocketMQ 交给监听器的 payload 是原始 JSON 字节，这里按同样形态构造
        return MessageBuilder
                .withPayload(JSONUtil.toJsonStr(orderMessage).getBytes(StandardCharsets.UTF_8))
                .build();
    }

    @Test
    void shouldCommitAndSavePrepayWhenLuaDeducts() {
        when(seckillStockService.tryKill(1L, 2L, 3L, SN)).thenReturn(SeckillTxContext.LUA_OK);
        SeckillTxContext ctx = context();

        assertEquals(RocketMQLocalTransactionState.COMMIT,
                listener.executeLocalTransaction(null, ctx));
        assertEquals(SeckillTxContext.LUA_OK, ctx.getLuaResult());
        verify(seckillStockService).savePrepay(SN, 9.9, 3L, 1L, 2L);
    }

    @Test
    void shouldRollbackWithoutPrepayWhenUserAlreadyHolds() {
        when(seckillStockService.tryKill(1L, 2L, 3L, SN)).thenReturn(SeckillTxContext.LUA_DUPLICATE);
        SeckillTxContext ctx = context();

        assertEquals(RocketMQLocalTransactionState.ROLLBACK,
                listener.executeLocalTransaction(null, ctx));
        assertEquals(SeckillTxContext.LUA_DUPLICATE, ctx.getLuaResult());
        verify(seckillStockService, never())
                .savePrepay(anyString(), anyDouble(), anyLong(), anyLong(), anyLong());
    }

    @Test
    void shouldRollbackWithoutPrepayWhenSoldOut() {
        when(seckillStockService.tryKill(1L, 2L, 3L, SN)).thenReturn(SeckillTxContext.LUA_SOLD_OUT);
        SeckillTxContext ctx = context();

        assertEquals(RocketMQLocalTransactionState.ROLLBACK,
                listener.executeLocalTransaction(null, ctx));
        assertEquals(SeckillTxContext.LUA_SOLD_OUT, ctx.getLuaResult());
        verify(seckillStockService, never())
                .savePrepay(anyString(), anyDouble(), anyLong(), anyLong(), anyLong());
    }

    /**
     * 守护核心不变量：库存已经扣掉之后，任何附带动作失败都不能回滚 ——
     * ROLLBACK 只丢弃半消息、不会把库存加回去，那样是永久少卖。
     */
    @Test
    void shouldStillCommitWhenPrepaySnapshotFails() {
        when(seckillStockService.tryKill(1L, 2L, 3L, SN)).thenReturn(SeckillTxContext.LUA_OK);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(seckillStockService).savePrepay(SN, 9.9, 3L, 1L, 2L);

        assertEquals(RocketMQLocalTransactionState.COMMIT,
                listener.executeLocalTransaction(null, context()));
    }

    @Test
    void shouldReturnUnknownWhenLuaThrows() {
        when(seckillStockService.tryKill(1L, 2L, 3L, SN))
                .thenThrow(new IllegalStateException("redis unavailable"));
        SeckillTxContext ctx = context();

        assertEquals(RocketMQLocalTransactionState.UNKNOWN,
                listener.executeLocalTransaction(null, ctx));
        assertEquals(SeckillTxContext.LUA_NOT_EXECUTED, ctx.getLuaResult());
    }

    @Test
    void shouldReturnUnknownWhenContextIsMissing() {
        assertEquals(RocketMQLocalTransactionState.UNKNOWN,
                listener.executeLocalTransaction(null, "not-a-context"));
        assertEquals(RocketMQLocalTransactionState.UNKNOWN,
                listener.executeLocalTransaction(null, null));
        verifyNoInteractions(seckillStockService);
    }

    /**
     * 最关键的一条守卫：用户已付款时必须提交，否则会变成「付了钱却永远没有订单」，
     * 比修复前的超卖窗口更严重。
     */
    @Test
    void shouldCommitOnCheckWhenAlreadyPaidEvenIfPlaceholderMismatched() {
        when(seckillStockService.isPaid(SN)).thenReturn(true);

        assertEquals(RocketMQLocalTransactionState.COMMIT,
                listener.checkLocalTransaction(checkMessage(SN, 1L, 2L, 3L)));
        verify(seckillStockService, never()).getUserOrderSn(any(), any(), any());
    }

    @Test
    void shouldReturnUnknownOnCheckWhenRedisFails() {
        when(seckillStockService.getUserOrderSn(1L, 2L, 3L))
                .thenThrow(new IllegalStateException("redis unavailable"));

        // 基础设施故障不能当业务结论，交给下一轮回查
        assertEquals(RocketMQLocalTransactionState.UNKNOWN,
                listener.checkLocalTransaction(checkMessage(SN, 1L, 2L, 3L)));
    }

    @Test
    void shouldRollbackOnCheckWhenIdentityFieldsAreIncomplete() {
        assertEquals(RocketMQLocalTransactionState.ROLLBACK,
                listener.checkLocalTransaction(checkMessage(SN, null, 2L, 3L)));
        verify(seckillStockService, never()).getUserOrderSn(any(), any(), any());
    }

    @Test
    void shouldSavePrepayOnlyAfterLuaSucceeds() {
        when(seckillStockService.tryKill(1L, 2L, 3L, SN)).thenReturn(SeckillTxContext.LUA_OK);

        listener.executeLocalTransaction(null, context());

        InOrder order = inOrder(seckillStockService);
        order.verify(seckillStockService).tryKill(1L, 2L, 3L, SN);
        order.verify(seckillStockService).savePrepay(SN, 9.9, 3L, 1L, 2L);
    }

    @Test
    void shouldCommitOnCheckWhenPlaceholderHoldsThisSn() {
        when(seckillStockService.getUserOrderSn(1L, 2L, 3L)).thenReturn(SN);

        assertEquals(RocketMQLocalTransactionState.COMMIT,
                listener.checkLocalTransaction(checkMessage(SN, 1L, 2L, 3L)));
    }

    @Test
    void shouldRollbackOnCheckWhenPlaceholderMissing() {
        when(seckillStockService.getUserOrderSn(1L, 2L, 3L)).thenReturn(null);

        assertEquals(RocketMQLocalTransactionState.ROLLBACK,
                listener.checkLocalTransaction(checkMessage(SN, 1L, 2L, 3L)));
    }

    @Test
    void shouldRollbackOnCheckWhenPlaceholderHoldsAnotherSn() {
        when(seckillStockService.getUserOrderSn(1L, 2L, 3L)).thenReturn("2026091800000000999");

        assertEquals(RocketMQLocalTransactionState.ROLLBACK,
                listener.checkLocalTransaction(checkMessage(SN, 1L, 2L, 3L)));
    }

    @Test
    void shouldRollbackOnCheckWhenPayloadIsUnreadable() {
        Message<byte[]> broken = MessageBuilder
                .withPayload("not-a-json".getBytes(StandardCharsets.UTF_8))
                .build();

        assertEquals(RocketMQLocalTransactionState.ROLLBACK,
                listener.checkLocalTransaction(broken));
        verify(seckillStockService, never()).getUserOrderSn(any(), any(), any());
    }
}
