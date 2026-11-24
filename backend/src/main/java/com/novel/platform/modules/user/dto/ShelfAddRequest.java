package com.novel.platform.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ShelfAddRequest(
        @NotBlank(message = "novelId 不能为空") String novelId
) {
}
