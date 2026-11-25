package com.novel.platform.modules.user.dto;

import java.math.BigDecimal;

public record AccountResponse(String userId, BigDecimal balance, String updatedAt) {
}
