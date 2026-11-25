package com.novel.platform.modules.novel.dto;

public record ChapterUpdateResponse(
        String chapterId,
        Integer chapterNo,
        Integer wordCount,
        Integer status,
        Integer auditStatus
) {
}
