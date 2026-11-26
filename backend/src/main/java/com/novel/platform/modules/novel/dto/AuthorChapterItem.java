package com.novel.platform.modules.novel.dto;

import java.math.BigDecimal;

public record AuthorChapterItem(
        String chapterId,
        Integer chapterNo,
        String title,
        Integer isVip,
        BigDecimal vipPrice,
        Integer wordCount,
        Integer status,
        Integer auditStatus,
        String publishTime,
        String createdAt,
        String updatedAt
) {
}
