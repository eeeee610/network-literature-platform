package com.novel.platform.modules.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 生成榜单期次。常规生成 allowHistoryOverwrite 为 false；
 * 仅历史修复时显式传入 true。
 */
public record RankingGenerateRequest(
        @NotBlank(message = "期次号不能为空")
        @Size(max = 20, message = "期次号不能超过 20 个字符")
        String periodNo,

        Boolean allowHistoryOverwrite
) {
}
