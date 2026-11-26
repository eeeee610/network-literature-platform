package com.novel.platform.modules.novel.auth;

import com.novel.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 临时鉴权：保存当前请求的令牌身份，联调时替换为 A 模块的鉴权实现。
 */
public final class AuthorContext {

    private static final ThreadLocal<AuthorPrincipal> HOLDER = new ThreadLocal<>();

    private AuthorContext() {
    }

    public static void set(AuthorPrincipal principal) {
        HOLDER.set(principal);
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static AuthorPrincipal require() {
        AuthorPrincipal principal = HOLDER.get();
        if (principal == null) {
            throw new BusinessException(40100, "未登录或登录已失效", HttpStatus.UNAUTHORIZED);
        }
        return principal;
    }

    public static AuthorPrincipal requireWriter() {
        AuthorPrincipal principal = require();
        if (!"AUTHOR".equalsIgnoreCase(principal.role()) && !principal.isAdmin()) {
            throw new BusinessException(40300, "无权执行该操作", HttpStatus.FORBIDDEN);
        }
        return principal;
    }
}
