package com.novel.platform.common.security;

import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.user.entity.AppUser;
import com.novel.platform.modules.user.mapper.AppUserMapper;
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
    private final AppUserMapper userMapper;

    public AuthInterceptor(JwtTokenProvider tokenProvider, AppUserMapper userMapper) {
        this.tokenProvider = tokenProvider;
        this.userMapper = userMapper;
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
        AuthUser user;
        try {
            user = tokenProvider.parseToken(token);
        } catch (RuntimeException exception) {
            return null;
        }
        AppUser account = userMapper.selectById(user.userId());
        if (account == null) {
            return null;
        }
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new BusinessException(40301, "账号已被锁定或停用", HttpStatus.FORBIDDEN);
        }
        return user;
    }

    private boolean isPublic(String path, String method) {
        if ("POST".equalsIgnoreCase(method)
                && ("/api/user/auth/login".equals(path) || "/api/user/auth/register".equals(path))) {
            return true;
        }
        if (path.startsWith("/api/interaction/ranking")) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method)) {
            return "/api/interaction/comment".equals(path)
                    || path.startsWith("/api/interaction/rating/")
                    || isPublicNovelGet(path);
        }
        return false;
    }

    private boolean isPublicNovelGet(String path) {
        return "/api/novel/list".equals(path)
                || "/api/novel/search".equals(path)
                || "/api/novel/tag/list".equals(path)
                || path.startsWith("/api/novel/chapter/")
                || path.matches("/api/novel/\\d+")
                || path.matches("/api/novel/\\d+/chapters");
    }
}
