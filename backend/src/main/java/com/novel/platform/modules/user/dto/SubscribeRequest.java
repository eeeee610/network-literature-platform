package com.novel.platform.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubscribeRequest(
        @NotBlank(message = "章节 ID 不能为空") String chapterId,
        @NotBlank(message = "请求号不能为空")
        @Size(max = 64, message = "请求号长度不能超过 64")
        @Pattern(regexp = "[A-Za-z0-9._:-]+", message = "请求号只能包含字母、数字、点、下划线、冒号或短横线")
        String requestNo
) {
}
