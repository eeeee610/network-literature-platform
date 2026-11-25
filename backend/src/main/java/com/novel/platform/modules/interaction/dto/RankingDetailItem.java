package com.novel.platform.modules.interaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 榜单条目展示项。
 */
public record RankingDetailItem(
        int rankNo,
        String novelId,
        String novelTitle,
        BigDecimal score,
        Map<String, Object> metricSnapshot,
        LocalDateTime createdAt
) {
}
