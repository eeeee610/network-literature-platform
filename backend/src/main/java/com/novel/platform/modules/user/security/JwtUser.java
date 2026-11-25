package com.novel.platform.modules.user.security;

/** 从 JWT 解析出的当前登录用户。 */
public record JwtUser(Long userId, String username, String role) {
}
