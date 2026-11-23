package com.novel.platform.modules.admin.dto;

import java.util.List;

public record DashboardSummaryResponse(
        String dataSource,
        List<Metric> metrics,
        List<AuditItem> pendingAudits
) {
    public record Metric(String key, String label, long value, String unit) {
    }

    public record AuditItem(String id, String type, String title, String author, String submittedAt) {
    }
}
