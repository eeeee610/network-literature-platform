package com.novel.platform.modules.user.dto;

import java.math.BigDecimal;

public record TipItem(
        String tipId,
        String tipNo,
        String requestNo,
        String fromUserId,
        String toUserId,
        String counterpartyName,
        String novelId,
        String novelTitle,
        String chapterId,
        String chapterTitle,
        BigDecimal amount,
        String message,
        String status,
        String outTxnNo,
        String inTxnNo,
        String createdAt,
        String refundedAt,
        String direction
) {
}
