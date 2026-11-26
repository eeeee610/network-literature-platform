package com.novel.platform.modules.user.dto;

import java.math.BigDecimal;

public record TipResponse(
        String tipId,
        String tipNo,
        String resultCode,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Boolean duplicate
) {
}
