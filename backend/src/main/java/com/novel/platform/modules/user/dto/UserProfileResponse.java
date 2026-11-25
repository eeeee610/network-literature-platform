package com.novel.platform.modules.user.dto;

public record UserProfileResponse(
        String userId,
        String username,
        String nickname,
        String avatarUrl,
        String email,
        String phone,
        String role,
        String status,
        String lastLoginAt,
        String createdAt,
        String updatedAt
) {
}
