package com.novel.platform.modules.user.security;

import com.novel.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** 请求级当前用户上下文；由 JwtAuthInterceptor 写入并在请求结束时清理。 */
public final class UserContext {

    private static final ThreadLocal<JwtUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(JwtUser user) {
        HOLDER.set(user);
    }

    public static JwtUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static JwtUser requireUser() {
        JwtUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(40100, "未登录或登录已失效", HttpStatus.UNAUTHORIZED);
        }
        return user;
    }

    public static Long requireUserId() {
        return requireUser().userId();
    }

    public static void requireAdmin() {
        if (!"ADMIN".equals(requireUser().role())) {
            throw new BusinessException(40300, "无权执行该操作", HttpStatus.FORBIDDEN);
        }
    }
}
