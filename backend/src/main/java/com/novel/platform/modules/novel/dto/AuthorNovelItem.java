package com.novel.platform.modules.novel.dto;

import java.util.List;

public record AuthorNovelItem(
        String novelId,
        String title,
        String coverUrl,
        Integer status,
        Integer auditStatus,
        Integer totalWords,
        Integer chapterCount,
        String latestChapterId,
        List<TagBrief> tags,
        String createdAt,
        String updatedAt
) {
}
