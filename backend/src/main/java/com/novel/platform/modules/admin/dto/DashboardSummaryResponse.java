package com.novel.platform.modules.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardSummaryResponse(
        String dataSource,
        long userCount,
        long novelCount,
        long commentCount,
        long subscriptionCount,
        BigDecimal subscriptionAmount,
        long tipCount,
        BigDecimal tipAmount,
        PendingAuditCounts pendingCounts,
        List<AuditItem> recentPendingAudits
) {
    public record PendingAuditCounts(long novel, long chapter, long comment, long rating) {
    }

    public record AuditItem(String id, String type, String title, String author, LocalDateTime submittedAt) {
    }
}
