package com.novel.platform.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserStatusRequest(
        @NotBlank(message = "status 不能为空")
        @Pattern(regexp = "ACTIVE|LOCKED|DISABLED", message = "status 只能为 ACTIVE、LOCKED 或 DISABLED")
        String status
) {
}
