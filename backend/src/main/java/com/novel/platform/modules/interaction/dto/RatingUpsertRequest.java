package com.novel.platform.modules.interaction.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 新增或修改我的评分。首次评分与改分共用本接口。
 */
public record RatingUpsertRequest(
        @NotBlank(message = "novelId 不能为空")
        String novelId,

        @NotNull(message = "评分不能为空")
        @Min(value = 1, message = "评分必须为 1~5 星")
        @Max(value = 5, message = "评分必须为 1~5 星")
        Integer score,

        @Size(max = 500, message = "短评不能超过 500 字")
        String reviewContent
) {
}
