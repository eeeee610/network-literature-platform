package com.novel.platform.modules.user.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.modules.user.dto.UpdateProfileRequest;
import com.novel.platform.modules.user.dto.UserProfileResponse;
import com.novel.platform.modules.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/profile")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<UserProfileResponse> profile() {
        return ApiResponse.success(userService.profile());
    }

    @PutMapping
    public ApiResponse<UserProfileResponse> update(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(request));
    }
}
