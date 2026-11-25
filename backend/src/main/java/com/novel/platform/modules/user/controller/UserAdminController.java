package com.novel.platform.modules.user.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.common.api.PageResult;
import com.novel.platform.common.auth.RequireRole;
import com.novel.platform.modules.user.model.Views;
import com.novel.platform.modules.user.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员专属：查看平台用户列表，用于验证 JWT 角色权限区分。
 */
@RestController
@RequestMapping("/api/user/admin")
@RequireRole("ADMIN")
public class UserAdminController {

    private final AuthService authService;

    public UserAdminController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/users")
    public ApiResponse<PageResult<Views.AdminUserItem>> users(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(authService.adminUsers(page, pageSize, keyword));
    }
}
