package com.novel.platform.modules.novel.dto;

public record OfflineChapterResponse(
        String chapterId,
        String novelId,
        Integer status,
        Integer totalWords,
        String latestChapterId
) {
}
