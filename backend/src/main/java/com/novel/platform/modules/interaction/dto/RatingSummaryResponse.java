package com.novel.platform.modules.interaction.dto;

import java.math.BigDecimal;

/**
 * 小说评分汇总，stats 来自 novel_rating_stat；my* 为当前登录用户评分。
 */
public record RatingSummaryResponse(
        String novelId,
        String novelTitle,
        int ratingCount,
        long scoreTotal,
        BigDecimal avgScore,
        Integer myScore,
        String myReviewContent,
        Integer myReviewAuditStatus
) {
}
