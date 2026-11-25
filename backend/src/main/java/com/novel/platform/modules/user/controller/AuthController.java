package com.novel.platform.modules.user.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.modules.user.dto.LoginRequest;
import com.novel.platform.modules.user.dto.LoginResponse;
import com.novel.platform.modules.user.dto.RegisterRequest;
import com.novel.platform.modules.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success();
    }
}
