package com.novel.platform.modules.user.dto;

/**
 * 登录结果。userId 以字符串返回，避免 JavaScript 精度丢失。
 */
public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        String userId,
        String username,
        String nickname,
        String avatarUrl,
        String role
) {
}
