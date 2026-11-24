package com.novel.platform.modules.user.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.common.api.PageResponse;
import com.novel.platform.common.security.UserContext;
import com.novel.platform.modules.user.dto.SubscribeRequest;
import com.novel.platform.modules.user.dto.SubscribeResult;
import com.novel.platform.modules.user.dto.SubscriptionItemResponse;
import com.novel.platform.modules.user.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public ApiResponse<PageResponse<SubscriptionItemResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(subscriptionService.list(UserContext.requireUserId(), page, pageSize));
    }

    @PostMapping
    public ApiResponse<SubscribeResult> subscribe(@Valid @RequestBody SubscribeRequest request) {
        return ApiResponse.success(subscriptionService.subscribe(
                UserContext.requireUserId(), request.chapterId(), request.requestNo()));
    }
}
