package com.novel.platform.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "[A-Za-z0-9_]{3,50}", message = "用户名须为 3～50 位字母、数字或下划线")
        String username,
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 72, message = "密码长度须为 8～72 位")
        String password,
        @NotBlank(message = "昵称不能为空")
        @Size(max = 50, message = "昵称长度不能超过 50")
        String nickname,
        @Email(message = "邮箱格式不正确")
        @Size(max = 254, message = "邮箱长度不能超过 254")
        String email,
        @Pattern(regexp = "^$|^[0-9+\\- ]{5,20}$", message = "手机号格式不正确")
        String phone
) {
}
