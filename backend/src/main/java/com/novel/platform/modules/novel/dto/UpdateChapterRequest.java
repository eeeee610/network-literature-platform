package com.novel.platform.modules.novel.dto;

import java.math.BigDecimal;

public record UpdateChapterRequest(String title, String content, Integer isVip, BigDecimal vipPrice) {
}
