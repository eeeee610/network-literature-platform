package com.novel.platform.modules.interaction.dto;

import java.time.LocalDateTime;

/**
 * 榜单期次列表项。
 */
public record RankingPeriodItem(String periodNo, long itemCount, LocalDateTime createdAt) {
}
