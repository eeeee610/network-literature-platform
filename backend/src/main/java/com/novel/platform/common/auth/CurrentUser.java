package com.novel.platform.common.auth;

import com.novel.platform.common.exception.BusinessException;

/**
 * 当前请求的登录用户上下文，由 AuthInterceptor 写入并在请求结束后清理。
 */
public final class CurrentUser {

    private static final ThreadLocal<AuthUser> HOLDER = new ThreadLocal<>();

    private CurrentUser() {
    }

    public static void set(AuthUser user) {
        HOLDER.set(user);
    }

    public static AuthUser get() {
        AuthUser user = HOLDER.get();
        if (user == null) {
            throw BusinessException.unauthorized("未登录或登录已失效");
        }
        return user;
    }

    public static Long userId() {
        return get().userId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
