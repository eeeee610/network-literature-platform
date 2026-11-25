package com.novel.platform.modules.user.dto;

import java.math.BigDecimal;

public record SubscribeResponse(
        String chapterId,
        String resultCode,
        String txnNo,
        BigDecimal paidAmount,
        BigDecimal balanceAfter,
        Boolean duplicate
) {
}
