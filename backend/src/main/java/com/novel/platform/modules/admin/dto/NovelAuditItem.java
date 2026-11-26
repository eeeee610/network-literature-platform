package com.novel.platform.modules.admin.dto;

import java.time.LocalDateTime;

public record NovelAuditItem(
        String novelId,
        String authorId,
        String title,
        String authorName,
        String coverUrl,
        String description,
        int status,
        int auditStatus,
        int totalWords,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
