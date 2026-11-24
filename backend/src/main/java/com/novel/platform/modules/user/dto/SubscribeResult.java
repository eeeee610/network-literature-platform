package com.novel.platform.modules.user.dto;

import java.math.BigDecimal;

public record SubscribeResult(
        String requestNo,
        String txnNo,
        BigDecimal balanceAfter,
        boolean alreadySubscribed,
        String message
) {
}
