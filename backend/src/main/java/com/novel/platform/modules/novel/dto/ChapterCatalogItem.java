package com.novel.platform.modules.novel.dto;

import java.math.BigDecimal;

public record ChapterCatalogItem(
        String chapterId,
        Integer chapterNo,
        String title,
        Integer isVip,
        BigDecimal vipPrice,
        Integer wordCount,
        String publishTime
) {
}
