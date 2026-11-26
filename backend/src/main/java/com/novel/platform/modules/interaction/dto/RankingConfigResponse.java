package com.novel.platform.modules.interaction.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 榜单配置展示项。weightConfig 以 JSON 对象返回。
 */
public record RankingConfigResponse(
        String rankingId,
        String rankingCode,
        String rankingName,
        String rankingType,
        String periodType,
        int topN,
        Map<String, Object> weightConfig,
        boolean active,
        String latestPeriodNo,
        long latestItemCount,
        LocalDateTime updatedAt
) {
}
