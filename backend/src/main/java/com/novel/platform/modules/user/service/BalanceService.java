package com.novel.platform.modules.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.novel.platform.common.api.PageResponse;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.user.dto.BalanceResponse;
import com.novel.platform.modules.user.dto.TransactionItemResponse;
import com.novel.platform.modules.user.entity.UserAccount;
import com.novel.platform.modules.user.mapper.AccountTransactionMapper;
import com.novel.platform.modules.user.mapper.UserAccountMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BalanceService {

    private final UserAccountMapper accountMapper;
    private final AccountTransactionMapper transactionMapper;

    public BalanceService(UserAccountMapper accountMapper, AccountTransactionMapper transactionMapper) {
        this.accountMapper = accountMapper;
        this.transactionMapper = transactionMapper;
    }

    public BalanceResponse getBalance(Long userId) {
        UserAccount account = accountMapper.selectById(userId);
        if (account == null) {
            throw new BusinessException(40430, "用户账户不存在", HttpStatus.NOT_FOUND);
        }
        BigDecimal income = transactionMapper.sumIncome(userId);
        BigDecimal consumption = transactionMapper.sumConsumption(userId);
        long count = transactionMapper.countByUser(userId);
        return new BalanceResponse(
                account.getBalance(),
                income == null ? BigDecimal.ZERO : income,
                consumption == null ? BigDecimal.ZERO : consumption,
                count
        );
    }

    public PageResponse<TransactionItemResponse> listTransactions(Long userId, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        Page<TransactionItemResponse> pageRequest = new Page<>(safePage, safeSize);
        return PageResponse.from(transactionMapper.selectTransactionPage(pageRequest, userId));
    }
}
