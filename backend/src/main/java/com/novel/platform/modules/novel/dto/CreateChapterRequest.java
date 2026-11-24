package com.novel.platform.modules.novel.dto;

import java.math.BigDecimal;

public record CreateChapterRequest(
        String novelId,
        String title,
        String content,
        Integer isVip,
        BigDecimal vipPrice
) {
}
