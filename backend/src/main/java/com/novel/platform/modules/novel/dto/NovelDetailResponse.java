package com.novel.platform.modules.novel.dto;

import java.util.List;

public record NovelDetailResponse(
        String novelId,
        String title,
        String coverUrl,
        String description,
        Integer status,
        Integer totalWords,
        Integer chapterCount,
        LatestChapterInfo latestChapter,
        String authorId,
        String authorName,
        String authorAvatarUrl,
        List<TagDetail> tags,
        String createdAt,
        String updatedAt
) {
}
