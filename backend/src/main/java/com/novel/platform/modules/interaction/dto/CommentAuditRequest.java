package com.novel.platform.modules.interaction.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 评论审核：action=1 通过，action=2 驳回（驳回原因必填）。
 */
public record CommentAuditRequest(
        @NotNull(message = "action 不能为空")
        @Min(value = 1, message = "action 只能为 1 或 2")
        @Max(value = 2, message = "action 只能为 1 或 2")
        Integer action,

        @Size(max = 255, message = "审核原因不能超过 255 字")
        String reason
) {
}
