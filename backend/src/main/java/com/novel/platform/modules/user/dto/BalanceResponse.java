package com.novel.platform.modules.user.dto;

import java.math.BigDecimal;

public record BalanceResponse(
        BigDecimal balance,
        BigDecimal totalIncome,
        BigDecimal totalConsumption,
        long transactionCount
) {
}
