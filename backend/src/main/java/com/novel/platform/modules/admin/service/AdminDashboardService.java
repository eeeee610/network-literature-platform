package com.novel.platform.modules.admin.service;

import com.novel.platform.modules.admin.dto.DashboardPendingAuditRow;
import com.novel.platform.modules.admin.dto.DashboardStatsRow;
import com.novel.platform.modules.admin.dto.DashboardSummaryResponse;
import com.novel.platform.modules.admin.mapper.AdminDashboardMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AdminDashboardService {

    private final AdminDashboardMapper dashboardMapper;

    public AdminDashboardService(AdminDashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    public DashboardSummaryResponse summary() {
        DashboardStatsRow stats = dashboardMapper.selectStats();
        List<DashboardSummaryResponse.AuditItem> pendingAudits = dashboardMapper
                .selectRecentPendingAudits().stream()
                .map(this::toAuditItem)
                .toList();
        return new DashboardSummaryResponse(
                "DB",
                stats.getUserCount(),
                stats.getNovelCount(),
                stats.getCommentCount(),
                stats.getSubscriptionCount(),
                amount(stats.getSubscriptionAmount()),
                stats.getTipCount(),
                amount(stats.getTipAmount()),
                new DashboardSummaryResponse.PendingAuditCounts(
                        stats.getPendingNovelCount(),
                        stats.getPendingChapterCount(),
                        stats.getPendingCommentCount(),
                        stats.getPendingRatingCount()),
                pendingAudits
        );
    }

    private DashboardSummaryResponse.AuditItem toAuditItem(DashboardPendingAuditRow row) {
        return new DashboardSummaryResponse.AuditItem(
                String.valueOf(row.getId()), row.getType(), row.getTitle(),
                row.getAuthor(), row.getSubmittedAt());
    }

    private static BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
