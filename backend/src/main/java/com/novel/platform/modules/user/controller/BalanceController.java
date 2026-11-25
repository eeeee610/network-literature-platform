package com.novel.platform.modules.user.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.modules.user.dto.PageResponse;
import com.novel.platform.modules.user.security.UserContext;
import com.novel.platform.modules.user.dto.BalanceResponse;
import com.novel.platform.modules.user.dto.TransactionItemResponse;
import com.novel.platform.modules.user.service.BalanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/balance")
    public ApiResponse<BalanceResponse> balance() {
        return ApiResponse.success(balanceService.getBalance(UserContext.requireUserId()));
    }

    @GetMapping("/transactions")
    public ApiResponse<PageResponse<TransactionItemResponse>> transactions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(balanceService.listTransactions(UserContext.requireUserId(), page, pageSize));
    }
}
