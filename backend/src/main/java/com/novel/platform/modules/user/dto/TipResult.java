package com.novel.platform.modules.user.dto;

import java.math.BigDecimal;

public record TipResult(
        String requestNo,
        String tipNo,
        BigDecimal fromBalanceAfter,
        BigDecimal toBalanceAfter,
        boolean duplicate,
        String message
) {
}
