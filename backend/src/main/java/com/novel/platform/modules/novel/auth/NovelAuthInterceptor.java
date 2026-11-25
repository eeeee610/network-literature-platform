package com.novel.platform.modules.novel.auth;

import com.novel.platform.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 临时鉴权：只拦截作者写接口与 /api/novel/author/**，公开 GET 不拦截。
 * 联调时替换为 A 模块的统一鉴权。
 */
@Component
public class NovelAuthInterceptor implements HandlerInterceptor {

    private final JwtTokenParser tokenParser;

    public NovelAuthInterceptor(JwtTokenParser tokenParser) {
        this.tokenParser = tokenParser;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        boolean writeRequest = !"GET".equalsIgnoreCase(request.getMethod());
        boolean authorArea = request.getRequestURI().startsWith("/api/novel/author");
        if (!writeRequest && !authorArea) {
            return true;
        }

        AuthorPrincipal principal = tokenParser.parse(request.getHeader("Authorization"));
        if (!"AUTHOR".equalsIgnoreCase(principal.role()) && !principal.isAdmin()) {
            throw new BusinessException(40300, "无权执行该操作", HttpStatus.FORBIDDEN);
        }
        AuthorContext.set(principal);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthorContext.clear();
    }
}
