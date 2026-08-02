package com.mdkj.component;

import com.mdkj.dto.OrderMessage;
import com.mdkj.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderMessageListenerTest {

    @Mock
    private OrderService orderService;
    @Mock
    private SeckillStockCompensator stockCompensator;
    @Mock
    private SeckillPrepayStore prepayStore;
    @InjectMocks
    private OrderMessageListener listener;

    @Test
    void shouldCompensateOnceAndAckWhenOrderCreationFails() {
        OrderMessage message = new OrderMessage();
        message.setSn("2026072300000000001");
        message.setFkSeckillId(1L);
        message.setFkCourseId(1L);
        message.setFkUserId(3L);

        doThrow(new IllegalStateException("database unavailable"))
                .when(orderService).createSeckillOrder(message);
        when(prepayStore.isPaid(message.getSn())).thenReturn(false);
        when(stockCompensator.rollbackIfOwned(1L, 1L, 3L, message.getSn()))
                .thenReturn(true);

        assertDoesNotThrow(() -> listener.onMessage(message));
        verify(stockCompensator, times(1))
                .rollbackIfOwned(1L, 1L, 3L, message.getSn());
    }

    @Test
    void shouldNotRetryWhenMessageWasAlreadyCompensated() {
        OrderMessage message = new OrderMessage();
        message.setSn("2026072300000000002");
        message.setFkSeckillId(1L);
        message.setFkCourseId(1L);
        message.setFkUserId(4L);

        doThrow(new IllegalStateException("duplicate delivery"))
                .when(orderService).createSeckillOrder(message);
        when(prepayStore.isPaid(message.getSn())).thenReturn(false);
        when(stockCompensator.rollbackIfOwned(1L, 1L, 4L, message.getSn()))
                .thenReturn(false);

        assertDoesNotThrow(() -> listener.onMessage(message));
        verify(stockCompensator, times(1))
                .rollbackIfOwned(1L, 1L, 4L, message.getSn());
    }

    @Test
    void shouldRetryWithoutRollbackWhenAlreadyPaid() {
        OrderMessage message = new OrderMessage();
        message.setSn("2026072300000000003");
        message.setFkSeckillId(1L);
        message.setFkCourseId(1L);
        message.setFkUserId(5L);

        doThrow(new IllegalStateException("database unavailable"))
                .when(orderService).createSeckillOrder(message);
        when(prepayStore.isPaid(message.getSn())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> listener.onMessage(message));
        verify(stockCompensator, never())
                .rollbackIfOwned(any(), any(), any(), any());
    }
}
