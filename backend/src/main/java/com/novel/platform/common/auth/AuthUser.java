package com.novel.platform.common.auth;

/**
 * 从 JWT 解析出的登录用户信息，不包含任何敏感字段。
 */
public record AuthUser(Long userId, String username, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
