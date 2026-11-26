package com.novel.platform.modules.user.dto;

public record BookshelfItem(
        String novelId,
        String title,
        String coverUrl,
        String authorId,
        String authorName,
        Integer novelStatus,
        Integer auditStatus,
        String lastReadChapterId,
        String lastReadChapterTitle,
        String lastReadAt,
        Boolean isTop,
        Boolean isNotify,
        String createdAt,
        String updatedAt,
        Boolean added,
        Boolean removed
) {
}
