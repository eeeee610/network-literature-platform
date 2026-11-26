package com.novel.platform.modules.admin.dto;

import java.time.LocalDateTime;

public record AdminUserItem(
        String userId,
        String username,
        String nickname,
        String avatarUrl,
        String email,
        String phone,
        String role,
        String status,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
}
