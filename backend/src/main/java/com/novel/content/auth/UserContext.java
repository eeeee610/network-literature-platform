package com.novel.content.auth;

import com.novel.content.common.BizException;

public final class UserContext {

    private static final ThreadLocal<AuthUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(long userId, String role) {
        HOLDER.set(new AuthUser(userId, role));
    }

    public static Long getUserId() {
        AuthUser user = HOLDER.get();
        return user == null ? null : user.userId();
    }

    public static String getRole() {
        AuthUser user = HOLDER.get();
        return user == null ? null : user.role();
    }

    public static long requireUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw BizException.unauthorized("未登录或令牌失效");
        }
        return userId;
    }

    public static void clear() {
        HOLDER.remove();
    }

    private record AuthUser(long userId, String role) {
    }
}
