package com.novel.content.controller;

import com.novel.content.auth.JwtUtil;
import com.novel.content.common.ApiResponse;
import com.novel.content.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 仅本地联调使用，app.dev-token-enabled=false 时关闭。
 */
@RestController
@RequestMapping("/api/v1")
public class DevTokenController {

    private static final long TTL_SECONDS = 86400;
    private static final Set<String> ROLES = Set.of("AUTHOR", "ADMIN", "READER");

    private final JwtUtil jwtUtil;
    private final boolean devTokenEnabled;

    public DevTokenController(JwtUtil jwtUtil,
                              @Value("${app.dev-token-enabled:false}") boolean devTokenEnabled) {
        this.jwtUtil = jwtUtil;
        this.devTokenEnabled = devTokenEnabled;
    }

    @GetMapping("/dev/token")
    public ApiResponse<Map<String, Object>> token(@RequestParam long userId,
                                                  @RequestParam(defaultValue = "AUTHOR") String role) {
        if (!devTokenEnabled) {
            throw BizException.notFound("开发令牌接口未启用");
        }
        if (userId <= 0) {
            throw BizException.badRequest("userId 必须为正整数");
        }
        String normalizedRole = role.trim().toUpperCase();
        if (!ROLES.contains(normalizedRole)) {
            throw BizException.badRequest("role 只能为 AUTHOR/ADMIN/READER");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", jwtUtil.sign(userId, normalizedRole, TTL_SECONDS));
        data.put("token_type", "Bearer");
        data.put("expires_in", TTL_SECONDS);
        data.put("user_id", userId);
        data.put("role", normalizedRole);
        return ApiResponse.success(data);
    }
}
