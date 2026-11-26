package com.novel.platform.common.security;

/**
 * 当前登录用户，来源于 JWT 载荷。
 */
public record AuthUser(Long userId, String username, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
