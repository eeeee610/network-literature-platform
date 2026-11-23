package com.novel.content.auth;

import com.novel.content.common.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Pattern;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Pattern NOVEL_DETAIL = Pattern.compile("^/api/v1/novels/\\d+$");
    private static final Pattern NOVEL_CATALOG = Pattern.compile("^/api/v1/novels/\\d+/chapters$");
    private static final Pattern CHAPTER_READ = Pattern.compile("^/api/v1/chapters/\\d+$");

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        String path = normalize(request.getRequestURI().substring(request.getContextPath().length()));
        if (isPublic(method, path)) {
            return true;
        }
        // 开发令牌接口自身不要求令牌，是否启用由控制器判断
        if ("GET".equalsIgnoreCase(method) && "/api/v1/dev/token".equals(path)) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw BizException.unauthorized("未登录或令牌失效");
        }
        JwtUtil.Payload payload = jwtUtil.verify(authorization.substring(7).trim());
        if (!"AUTHOR".equals(payload.role()) && !"ADMIN".equals(payload.role())) {
            throw BizException.forbidden("当前账号无创作权限");
        }
        UserContext.set(payload.userId(), payload.role());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean isPublic(String method, String path) {
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }
        return "/api/v1/novels".equals(path)
                || "/api/v1/novels/search".equals(path)
                || "/api/v1/tags".equals(path)
                || NOVEL_DETAIL.matcher(path).matches()
                || NOVEL_CATALOG.matcher(path).matches()
                || CHAPTER_READ.matcher(path).matches();
    }

    private String normalize(String path) {
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }
}
