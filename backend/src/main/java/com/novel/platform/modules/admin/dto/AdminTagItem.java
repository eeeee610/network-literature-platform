package com.novel.platform.modules.admin.dto;

import java.time.LocalDateTime;

public record AdminTagItem(
        String tagId,
        String tagName,
        int isActive,
        long novelCount,
        LocalDateTime createdAt
) {
}
