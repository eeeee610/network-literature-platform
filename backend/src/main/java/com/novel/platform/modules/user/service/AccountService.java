package com.novel.platform.modules.user.service;

import com.novel.platform.common.api.PageResult;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.user.dto.AccountResponse;
import com.novel.platform.modules.user.dto.AccountTransactionItem;
import com.novel.platform.modules.user.entity.AccountTransaction;
import com.novel.platform.modules.user.entity.AppUser;
import com.novel.platform.modules.user.entity.UserAccount;
import com.novel.platform.modules.user.mapper.UserAccountMapper;
import com.novel.platform.modules.user.mapper.UserTradeMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {

    private final UserService userService;
    private final UserAccountMapper accountMapper;
    private final UserTradeMapper tradeMapper;

    public AccountService(UserService userService, UserAccountMapper accountMapper,
                          UserTradeMapper tradeMapper) {
        this.userService = userService;
        this.accountMapper = accountMapper;
        this.tradeMapper = tradeMapper;
    }

    public AccountResponse account() {
        AppUser user = userService.requireActiveUser();
        UserAccount account = accountMapper.selectById(user.getUserId());
        if (account == null) {
            throw new BusinessException(41008, "用户账户不存在", HttpStatus.NOT_FOUND);
        }
        return new AccountResponse(String.valueOf(account.getUserId()), account.getBalance(),
                UserSupport.iso(account.getUpdatedAt()));
    }

    public PageResult<AccountTransactionItem> transactions(Integer pageRaw, Integer pageSizeRaw) {
        AppUser user = userService.requireActiveUser();
        int page = UserSupport.page(pageRaw);
        int pageSize = UserSupport.pageSize(pageSizeRaw);
        long total = tradeMapper.countTransactions(user.getUserId());
        List<AccountTransactionItem> items = total == 0 ? List.of() : tradeMapper.selectTransactionPage(
                        user.getUserId(), UserSupport.offset(page, pageSize), pageSize)
                .stream().map(this::toItem).toList();
        return new PageResult<>(items, total, page, pageSize);
    }

    private AccountTransactionItem toItem(AccountTransaction row) {
        return new AccountTransactionItem(
                String.valueOf(row.getTxnId()), row.getTxnNo(), row.getBizType(), row.getDirection(),
                row.getAmount(), row.getBalanceBefore(), row.getBalanceAfter(), row.getRelatedType(),
                row.getRelatedId() == null ? null : String.valueOf(row.getRelatedId()), row.getRequestNo(),
                row.getRemark(), UserSupport.iso(row.getCreatedAt()));
    }
}
