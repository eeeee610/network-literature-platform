package com.novel.platform.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

public record BookshelfRequest(@NotBlank(message = "作品 ID 不能为空") String novelId) {
}
