package com.novel.platform.modules.novel.dto;

import java.util.List;

public record NovelListItem(
        String novelId,
        String title,
        String coverUrl,
        String description,
        Integer status,
        Integer totalWords,
        String latestChapterId,
        String latestChapterTitle,
        String authorId,
        String authorName,
        List<TagBrief> tags,
        String updatedAt
) {
}
