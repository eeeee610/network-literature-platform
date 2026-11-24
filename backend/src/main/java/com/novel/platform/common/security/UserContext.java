package com.novel.platform.common.security;

/**
 * 请求级登录用户上下文，由 AuthInterceptor 写入并在请求结束后清理。
 */
public final class UserContext {

    private static final ThreadLocal<AuthUser> CURRENT = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(AuthUser user) {
        CURRENT.set(user);
    }

    public static AuthUser get() {
        return CURRENT.get();
    }

    public static Long currentUserId() {
        AuthUser user = CURRENT.get();
        return user == null ? null : user.userId();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
