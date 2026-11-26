package com.novel.platform.modules.admin.dto;

import java.time.LocalDateTime;

public record ChapterAuditItem(
        String chapterId,
        String novelId,
        String novelTitle,
        String authorName,
        int chapterNo,
        String title,
        int wordCount,
        int isVip,
        int status,
        Integer auditStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
