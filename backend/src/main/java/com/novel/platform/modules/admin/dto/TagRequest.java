package com.novel.platform.modules.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TagRequest(
        @NotBlank(message = "标签名称不能为空")
        @Size(max = 32, message = "标签名称不能超过 32 字")
        String tagName,
        @NotNull(message = "isActive 不能为空")
        @Min(value = 0, message = "isActive 只能为 0 或 1")
        @Max(value = 1, message = "isActive 只能为 0 或 1")
        Integer isActive
) {
}
