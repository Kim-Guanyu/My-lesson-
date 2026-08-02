package com.mdkj.component;

import com.mdkj.util.MyRedis;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeckillStockCompensatorTest {

    @Mock
    private MyRedis redis;
    @Mock
    private SeckillPrepayStore prepayStore;
    @InjectMocks
    private SeckillStockCompensator compensator;

    @Test
    void shouldReportRollbackOnlyWhenLuaChangedStock() {
        when(prepayStore.isPaid("sn-1")).thenReturn(false);
        when(redis.lua(anyString(), anyList(), any(Object[].class)))
                .thenReturn(1L, 0L);

        assertTrue(compensator.rollbackIfOwned(1L, 2L, 3L, "sn-1"));
        assertFalse(compensator.rollbackIfOwned(1L, 2L, 3L, "sn-1"));
        verify(prepayStore).deletePrepay("sn-1");
    }

    @Test
    void shouldSkipRollbackWhenIdentityIsIncomplete() {
        assertFalse(compensator.rollbackIfOwned(1L, 2L, 3L, null));
        assertFalse(compensator.rollbackIfOwned(null, 2L, 3L, "sn-1"));
    }

    @Test
    void shouldNeverRollbackWhenAlreadyPaid() {
        when(prepayStore.isPaid("sn-paid")).thenReturn(true);

        assertFalse(compensator.rollbackIfOwned(1L, 2L, 3L, "sn-paid"));
        verify(redis, never()).lua(anyString(), anyList(), any(Object[].class));
        verify(prepayStore, never()).deletePrepay(anyString());
    }
}
