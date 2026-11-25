package com.novel.platform.modules.interaction.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 新增榜单配置。
 */
public record RankingConfigCreateRequest(
        @NotBlank(message = "榜单编码不能为空")
        @Pattern(regexp = "^[A-Z0-9_]{2,50}$", message = "榜单编码只能由大写字母、数字和下划线组成，长度 2~50")
        String rankingCode,

        @NotBlank(message = "榜单名称不能为空")
        @Size(max = 100, message = "榜单名称不能超过 100 字")
        String rankingName,

        @NotBlank(message = "榜单指标类型不能为空")
        @Pattern(regexp = "FAVORITE|RATING|TICKET|SALE", message = "榜单指标类型只能为 FAVORITE/RATING/TICKET/SALE")
        String rankingType,

        @NotBlank(message = "统计周期不能为空")
        @Pattern(regexp = "DAILY|WEEKLY|MONTHLY|ALL", message = "统计周期只能为 DAILY/WEEKLY/MONTHLY/ALL")
        String periodType,

        @NotNull(message = "展示条数上限不能为空")
        @Min(value = 1, message = "展示条数上限至少为 1")
        @Max(value = 1000, message = "展示条数上限不能超过 1000")
        Integer topN,

        Map<String, Object> weightConfig,

        Boolean isActive
) {
}
