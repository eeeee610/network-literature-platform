package com.novel.platform.common.security;

import com.novel.platform.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 鉴权拦截器。
 * <p>
 * 公开接口：登录、榜单查询、评论与评分只读查询。
 * 需要登录：评论/评分的写操作；/api/admin/** 全部要求管理员身份。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    public AuthInterceptor(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        AuthUser user = resolveUser(request);
        if (user != null) {
            UserContext.set(user);
        }

        if (isPublic(path, request.getMethod())) {
            return true;
        }

        if (user == null) {
            throw new BusinessException(40100, "未登录或登录已失效", HttpStatus.UNAUTHORIZED);
        }
        if (path.startsWith("/api/admin/") && !user.isAdmin()) {
            throw new BusinessException(40300, "无权执行该操作", HttpStatus.FORBIDDEN);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        UserContext.clear();
    }

    private AuthUser resolveUser(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            return null;
        }
        try {
            return tokenProvider.parseToken(token);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private boolean isPublic(String path, String method) {
        if ("POST".equalsIgnoreCase(method) && "/api/user/auth/login".equals(path)) {
            return true;
        }
        if (path.startsWith("/api/interaction/ranking")) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method)) {
            return "/api/interaction/comment".equals(path)
                    || path.startsWith("/api/interaction/rating/");
        }
        return false;
    }
}
