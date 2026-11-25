package com.novel.platform;

import com.novel.platform.modules.admin.dto.DashboardPendingAuditRow;
import com.novel.platform.modules.admin.dto.DashboardStatsRow;
import com.novel.platform.modules.admin.dto.DashboardSummaryResponse;
import com.novel.platform.modules.admin.mapper.AdminDashboardMapper;
import com.novel.platform.modules.admin.service.AdminDashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private AdminDashboardMapper dashboardMapper;

    @InjectMocks
    private AdminDashboardService dashboardService;

    @Test
    void shouldAggregateCountsAmountsAndPendingAudits() {
        DashboardStatsRow stats = new DashboardStatsRow();
        stats.setUserCount(10);
        stats.setNovelCount(6);
        stats.setCommentCount(30);
        stats.setSubscriptionCount(4);
        stats.setSubscriptionAmount(new BigDecimal("88.50"));
        stats.setTipCount(2);
        stats.setTipAmount(new BigDecimal("20.00"));
        stats.setPendingNovelCount(1);
        stats.setPendingChapterCount(2);
        stats.setPendingCommentCount(3);
        stats.setPendingRatingCount(4);
        DashboardPendingAuditRow pending = new DashboardPendingAuditRow();
        pending.setId(99L);
        pending.setType("CHAPTER");
        pending.setTitle("作品 / 第一章");
        pending.setAuthor("作者甲");
        when(dashboardMapper.selectStats()).thenReturn(stats);
        when(dashboardMapper.selectRecentPendingAudits()).thenReturn(List.of(pending));

        DashboardSummaryResponse response = dashboardService.summary();

        assertEquals("DB", response.dataSource());
        assertEquals(10, response.userCount());
        assertEquals(new BigDecimal("88.50"), response.subscriptionAmount());
        assertEquals(4, response.pendingCounts().rating());
        assertEquals("99", response.recentPendingAudits().get(0).id());
        assertEquals("CHAPTER", response.recentPendingAudits().get(0).type());
    }
}
