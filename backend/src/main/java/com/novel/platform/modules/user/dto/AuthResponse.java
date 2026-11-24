package com.novel.platform.modules.user.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserProfile user
) {
}
