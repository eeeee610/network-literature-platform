package com.novel.platform.modules.user.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novel.platform.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * A 模块 JWT 拦截器：只挂载到 /api/user/** 和 /api/auth/me。
 * D 的 /api/admin/**、现有 WebConfig 与 Mock 接口完全不受影响。
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthInterceptor(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeError(response, 401, 40100, "未登录或登录已失效");
            return false;
        }

        try {
            JwtUser user = jwtService.parse(authorization.substring(7).trim());
            UserContext.set(user);
            return true;
        } catch (IllegalArgumentException exception) {
            writeError(response, 401, 40100, exception.getMessage());
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception exception) {
        UserContext.clear();
    }

    private void writeError(HttpServletResponse response, int status, int code, String message) throws Exception {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(code, message));
    }
}
