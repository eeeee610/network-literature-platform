package com.novel.platform.modules.user.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.modules.user.dto.PageResponse;
import com.novel.platform.modules.user.security.UserContext;
import com.novel.platform.modules.user.dto.TipItemResponse;
import com.novel.platform.modules.user.dto.TipRequest;
import com.novel.platform.modules.user.dto.TipResult;
import com.novel.platform.modules.user.service.TipService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/tips")
public class TipController {

    private final TipService tipService;

    public TipController(TipService tipService) {
        this.tipService = tipService;
    }

    @GetMapping
    public ApiResponse<PageResponse<TipItemResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(tipService.list(UserContext.requireUserId(), page, pageSize));
    }

    @PostMapping
    public ApiResponse<TipResult> tip(@Valid @RequestBody TipRequest request) {
        return ApiResponse.success(tipService.tip(UserContext.requireUserId(), request));
    }
}
