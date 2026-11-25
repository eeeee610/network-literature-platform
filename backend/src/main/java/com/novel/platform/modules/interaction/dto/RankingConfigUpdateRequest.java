package com.novel.platform.modules.interaction.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 修改榜单配置。榜单编码、指标类型与周期创建后不可修改，避免历史快照失真。
 */
public record RankingConfigUpdateRequest(
        @NotBlank(message = "榜单名称不能为空")
        @Size(max = 100, message = "榜单名称不能超过 100 字")
        String rankingName,

        @NotNull(message = "展示条数上限不能为空")
        @Min(value = 1, message = "展示条数上限至少为 1")
        @Max(value = 1000, message = "展示条数上限不能超过 1000")
        Integer topN,

        Map<String, Object> weightConfig,

        Boolean isActive
) {
}
