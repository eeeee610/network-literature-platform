package com.novel.platform.modules.user.dto;

import java.math.BigDecimal;

public record AccountTransactionItem(
        String txnId,
        String txnNo,
        String bizType,
        String direction,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String relatedType,
        String relatedId,
        String requestNo,
        String remark,
        String createdAt
) {
}
