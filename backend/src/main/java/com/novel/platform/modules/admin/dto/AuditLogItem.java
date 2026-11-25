package com.novel.platform.modules.admin.dto;

import java.time.LocalDateTime;

public record AuditLogItem(
        String auditId,
        String adminId,
        String adminName,
        String targetType,
        String targetId,
        int auditAction,
        String auditReason,
        LocalDateTime createdAt
) {
}
