package com.novel.platform.modules.user.dto;

public record UserProfile(
        String userId,
        String username,
        String nickname,
        String avatarUrl,
        String role,
        String status
) {
}
