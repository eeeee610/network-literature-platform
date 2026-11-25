package com.novel.platform;

import com.novel.platform.modules.user.dto.SubscriptionItem;
import com.novel.platform.modules.user.mapper.UserTradeMapper;
import com.novel.platform.modules.user.service.ChapterEntitlementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChapterEntitlementServiceTest {

    @Mock
    private UserTradeMapper tradeMapper;
    @InjectMocks
    private ChapterEntitlementService entitlementService;

    @Test
    void shouldAcceptActiveSubscription() {
        when(tradeMapper.selectSubscription(101L, 501L)).thenReturn(subscription("ACTIVE"));

        assertTrue(entitlementService.hasActiveSubscription(101L, 501L));
    }

    @Test
    void shouldRejectRefundedSubscription() {
        when(tradeMapper.selectSubscription(101L, 501L)).thenReturn(subscription("REFUNDED"));

        assertFalse(entitlementService.hasActiveSubscription(101L, 501L));
    }

    @Test
    void shouldRejectGuestWithoutQueryingSubscription() {
        assertFalse(entitlementService.hasActiveSubscription(null, 501L));
        verifyNoInteractions(tradeMapper);
    }

    private SubscriptionItem subscription(String status) {
        return new SubscriptionItem("501", "301", "测试作品", 1, "第一章",
                new BigDecimal("2.50"), new BigDecimal("2.50"), "subscribe-txn", "SUB-test-1", status,
                "2026-11-25T11:00:00+08:00", null, null, null);
    }
}
