package com.novel.platform.modules.novel.auth;

/**
 * 临时鉴权上下文：令牌解析后的用户身份。
 */
public record AuthorPrincipal(long userId, String role) {

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
