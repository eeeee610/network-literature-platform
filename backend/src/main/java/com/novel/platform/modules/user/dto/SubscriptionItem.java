package com.novel.platform.modules.user.dto;

import java.math.BigDecimal;

public record SubscriptionItem(
        String chapterId,
        String novelId,
        String novelTitle,
        Integer chapterNo,
        String chapterTitle,
        BigDecimal unitPrice,
        BigDecimal paidAmount,
        String txnNo,
        String requestNo,
        String status,
        String subscribedAt,
        String refundedAt,
        BigDecimal balanceAfter,
        Boolean refunded
) {
}
