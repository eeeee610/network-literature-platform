package com.novel.platform.modules.novel.dto;

import java.math.BigDecimal;

public record ChapterReadResponse(
        String chapterId,
        String novelId,
        String novelTitle,
        Integer chapterNo,
        String title,
        String content,
        boolean canRead,
        Integer isVip,
        BigDecimal vipPrice,
        Integer wordCount,
        String publishTime,
        String prevChapterId,
        String prevChapterTitle,
        String nextChapterId,
        String nextChapterTitle
) {
}
