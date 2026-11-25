package com.novel.platform.modules.user.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.common.api.PageResult;
import com.novel.platform.modules.user.model.Requests;
import com.novel.platform.modules.user.model.Views;
import com.novel.platform.modules.user.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/chapter/{chapterId}")
    public ApiResponse<Views.ChapterPurchase> purchaseInfo(@PathVariable String chapterId) {
        return ApiResponse.success(subscriptionService.purchaseInfo(chapterId));
    }

    @PostMapping
    public ApiResponse<Views.SubscribeResult> subscribe(@Valid @RequestBody Requests.Subscribe request) {
        return ApiResponse.success(subscriptionService.subscribe(request));
    }

    @GetMapping
    public ApiResponse<PageResult<Views.SubscriptionItem>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(subscriptionService.list(page, pageSize));
    }

    @GetMapping("/check")
    public ApiResponse<Views.SubscriptionCheck> check(@RequestParam String chapterId) {
        return ApiResponse.success(subscriptionService.check(chapterId));
    }
}
