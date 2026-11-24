package com.novel.platform.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubscribeRequest(
        @NotBlank(message = "chapterId 不能为空") String chapterId,
        @Size(max = 64, message = "requestNo 不能超过 64 个字符") String requestNo
) {
}
