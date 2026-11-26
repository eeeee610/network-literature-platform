package com.novel.platform;

import com.novel.platform.common.api.PageResult;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.user.dto.AccountResponse;
import com.novel.platform.modules.user.dto.AccountTransactionItem;
import com.novel.platform.modules.user.entity.AccountTransaction;
import com.novel.platform.modules.user.entity.AppUser;
import com.novel.platform.modules.user.entity.UserAccount;
import com.novel.platform.modules.user.mapper.UserAccountMapper;
import com.novel.platform.modules.user.mapper.UserTradeMapper;
import com.novel.platform.modules.user.service.AccountService;
import com.novel.platform.modules.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private UserAccountMapper accountMapper;
    @Mock
    private UserTradeMapper tradeMapper;
    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldReturnCurrentUsersBalance() {
        when(userService.requireActiveUser()).thenReturn(activeUser());
        UserAccount account = new UserAccount();
        account.setUserId(101L);
        account.setBalance(new BigDecimal("88.50"));
        account.setUpdatedAt(LocalDateTime.of(2026, 11, 25, 10, 30));
        when(accountMapper.selectById(101L)).thenReturn(account);

        AccountResponse response = accountService.account();

        assertEquals("101", response.userId());
        assertEquals(new BigDecimal("88.50"), response.balance());
        assertEquals("2026-11-25T10:30:00+08:00", response.updatedAt());
    }

    @Test
    void shouldOnlyQueryCurrentUsersTransactionPage() {
        when(userService.requireActiveUser()).thenReturn(activeUser());
        when(tradeMapper.countTransactions(101L)).thenReturn(1L);
        AccountTransaction transaction = new AccountTransaction();
        transaction.setTxnId(9001L);
        transaction.setTxnNo("txn-9001");
        transaction.setUserId(101L);
        transaction.setBizType("SUBSCRIBE");
        transaction.setDirection("OUT");
        transaction.setAmount(new BigDecimal("12.00"));
        transaction.setBalanceBefore(new BigDecimal("100.00"));
        transaction.setBalanceAfter(new BigDecimal("88.00"));
        transaction.setRelatedType("CHAPTER");
        transaction.setRelatedId(501L);
        transaction.setRequestNo("SUB-test");
        transaction.setCreatedAt(LocalDateTime.of(2026, 11, 25, 11, 0));
        when(tradeMapper.selectTransactionPage(101L, 20L, 10)).thenReturn(List.of(transaction));

        PageResult<AccountTransactionItem> result = accountService.transactions(3, 10);

        verify(tradeMapper).selectTransactionPage(101L, 20L, 10);
        assertEquals(1L, result.total());
        assertEquals(3, result.page());
        assertEquals("9001", result.list().get(0).txnId());
        assertEquals(new BigDecimal("88.00"), result.list().get(0).balanceAfter());
    }

    @Test
    void shouldRejectMissingAccount() {
        when(userService.requireActiveUser()).thenReturn(activeUser());
        when(accountMapper.selectById(101L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> accountService.account());

        assertEquals(41008, exception.getCode());
    }

    private AppUser activeUser() {
        AppUser user = new AppUser();
        user.setUserId(101L);
        user.setStatus("ACTIVE");
        return user;
    }
}
