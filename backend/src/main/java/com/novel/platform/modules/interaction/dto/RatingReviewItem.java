package com.novel.platform.modules.interaction.dto;

import java.time.LocalDateTime;

/**
 * 评分短评列表项。后台审核列表额外使用 status/reviewAuditStatus。
 */
public record RatingReviewItem(
        String ratingId,
        String userId,
        String nickname,
        String avatarUrl,
        String novelId,
        String novelTitle,
        int score,
        String reviewContent,
        int status,
        Integer reviewAuditStatus,
        LocalDateTime createdAt
) {
}
