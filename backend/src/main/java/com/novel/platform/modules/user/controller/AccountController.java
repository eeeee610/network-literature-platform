package com.novel.platform.modules.user.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.common.api.PageResult;
import com.novel.platform.modules.user.model.Requests;
import com.novel.platform.modules.user.model.Views;
import com.novel.platform.modules.user.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ApiResponse<Views.AccountInfo> info() {
        return ApiResponse.success(accountService.info());
    }

    @PostMapping("/recharge")
    public ApiResponse<Views.RechargeResult> recharge(@Valid @RequestBody Requests.Recharge request) {
        return ApiResponse.success(accountService.recharge(request));
    }

    @GetMapping("/transactions")
    public ApiResponse<PageResult<Views.TransactionItem>> transactions(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String direction) {
        return ApiResponse.success(accountService.transactions(page, pageSize, bizType, direction));
    }
}
