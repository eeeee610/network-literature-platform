package com.novel.platform.modules.novel.dto;

public record ChapterDraftResponse(
        String chapterId,
        String novelId,
        Integer chapterNo,
        Integer wordCount,
        Integer status,
        Integer auditStatus
) {
}
