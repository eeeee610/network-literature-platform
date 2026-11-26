package com.novel.platform.modules.user.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.common.api.PageResult;
import com.novel.platform.modules.user.dto.SubscribeRequest;
import com.novel.platform.modules.user.dto.SubscribeResponse;
import com.novel.platform.modules.user.dto.SubscriptionItem;
import com.novel.platform.modules.user.dto.TipItem;
import com.novel.platform.modules.user.dto.TipRequest;
import com.novel.platform.modules.user.dto.TipResponse;
import com.novel.platform.modules.user.service.TradeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @PostMapping("/subscription")
    public ApiResponse<SubscribeResponse> subscribe(@Valid @RequestBody SubscribeRequest request) {
        return ApiResponse.success(tradeService.subscribe(request));
    }

    @GetMapping("/subscription")
    public ApiResponse<PageResult<SubscriptionItem>> subscriptions(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(tradeService.subscriptions(page, pageSize));
    }

    @DeleteMapping("/subscription/{chapterId}")
    public ApiResponse<SubscriptionItem> cancel(@PathVariable String chapterId) {
        return ApiResponse.success(tradeService.cancelSubscription(chapterId));
    }

    @PostMapping("/tip")
    public ApiResponse<TipResponse> tip(@Valid @RequestBody TipRequest request) {
        return ApiResponse.success(tradeService.tip(request));
    }

    @GetMapping("/tip")
    public ApiResponse<PageResult<TipItem>> tips(@RequestParam(required = false) String direction,
                                                 @RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(tradeService.tips(direction, page, pageSize));
    }
}
