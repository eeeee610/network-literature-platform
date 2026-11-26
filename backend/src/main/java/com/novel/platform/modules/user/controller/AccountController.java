package com.novel.platform.modules.user.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.common.api.PageResult;
import com.novel.platform.modules.user.dto.AccountResponse;
import com.novel.platform.modules.user.dto.AccountTransactionItem;
import com.novel.platform.modules.user.service.AccountService;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ApiResponse<AccountResponse> account() {
        return ApiResponse.success(accountService.account());
    }

    @GetMapping("/transaction")
    public ApiResponse<PageResult<AccountTransactionItem>> transactions(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(accountService.transactions(page, pageSize));
    }
}
