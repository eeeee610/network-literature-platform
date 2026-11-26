package com.novel.platform.modules.interaction.dto;

import java.util.List;

/**
 * 榜单某期次详情。
 */
public record RankingDetailResponse(
        String rankingCode,
        String rankingName,
        String rankingType,
        String periodType,
        String periodNo,
        List<RankingDetailItem> items
) {
}
