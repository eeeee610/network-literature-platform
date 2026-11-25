package com.novel.platform.modules.interaction.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 评论状态变更：1 正常、2 隐藏、3 软删除。
 */
public record CommentStatusRequest(
        @NotNull(message = "status 不能为空")
        @Min(value = 1, message = "status 只能为 1、2 或 3")
        @Max(value = 3, message = "status 只能为 1、2 或 3")
        Integer status
) {
}
