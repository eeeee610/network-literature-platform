package com.novel.platform.modules.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContentAuditRequest(
        @NotNull(message = "action 不能为空")
        @Min(value = 1, message = "action 只能为 1 或 2")
        @Max(value = 2, message = "action 只能为 1 或 2")
        Integer action,
        @Size(max = 255, message = "审核原因不能超过 255 字")
        String reason
) {
}
