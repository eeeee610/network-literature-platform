package com.novel.platform.modules.novel.dto;

public record PublishChapterResponse(
        String chapterId,
        String novelId,
        Integer chapterNo,
        Integer wordCount,
        Integer status,
        String publishTime
) {
}
