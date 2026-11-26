package com.novel.platform.modules.admin.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.common.api.PageResult;
import com.novel.platform.common.security.UserContext;
import com.novel.platform.modules.admin.dto.AdminUserItem;
import com.novel.platform.modules.admin.dto.UserStatusRequest;
import com.novel.platform.modules.admin.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    private final AdminUserService userService;

    public AdminUserController(AdminUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminUserItem>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(userService.list(keyword, role, status, page, pageSize));
    }

    @PatchMapping("/{userId}/status")
    public ApiResponse<Void> changeStatus(@PathVariable Long userId,
                                          @Valid @RequestBody UserStatusRequest request) {
        userService.changeStatus(userId, request, UserContext.currentUserId());
        return ApiResponse.success();
    }
}
