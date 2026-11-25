package com.novel.platform;

import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.user.dto.SubscribeParam;
import com.novel.platform.modules.user.dto.SubscribeRequest;
import com.novel.platform.modules.user.dto.SubscribeResponse;
import com.novel.platform.modules.user.dto.SubscriptionItem;
import com.novel.platform.modules.user.dto.TipItem;
import com.novel.platform.modules.user.dto.TipParam;
import com.novel.platform.modules.user.dto.TipRequest;
import com.novel.platform.modules.user.dto.TipResponse;
import com.novel.platform.modules.user.entity.AccountTransaction;
import com.novel.platform.modules.user.entity.AppUser;
import com.novel.platform.modules.user.entity.ChapterSubscription;
import com.novel.platform.modules.user.entity.UserAccount;
import com.novel.platform.modules.user.mapper.UserAccountMapper;
import com.novel.platform.modules.user.mapper.UserTradeMapper;
import com.novel.platform.modules.user.service.TradeProcRepository;
import com.novel.platform.modules.user.service.TradeService;
import com.novel.platform.modules.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private UserTradeMapper tradeMapper;
    @Mock
    private UserAccountMapper accountMapper;
    @Mock
    private TradeProcRepository procRepository;
    @InjectMocks
    private TradeService tradeService;

    @Test
    void shouldSubscribeThroughProcedureAndReturnChargedBalance() {
        when(userService.requireActiveUser()).thenReturn(activeUser());
        doAnswer(invocation -> {
            SubscribeParam param = invocation.getArgument(0);
            param.setResultCode("SUCCESS");
            param.setTxnNo("subscribe-txn");
            param.setBalanceAfter(new BigDecimal("87.50"));
            return null;
        }).when(procRepository).subscribe(any(SubscribeParam.class));
        when(tradeMapper.selectSubscription(101L, 501L))
                .thenReturn(subscriptionItem("ACTIVE", new BigDecimal("12.50"), null));

        SubscribeResponse response = tradeService.subscribe(new SubscribeRequest("501", "SUB-test-1"));

        ArgumentCaptor<SubscribeParam> paramCaptor = ArgumentCaptor.forClass(SubscribeParam.class);
        verify(procRepository).subscribe(paramCaptor.capture());
        assertEquals(101L, paramCaptor.getValue().getUserId());
        assertEquals(501L, paramCaptor.getValue().getChapterId());
        assertEquals("SUB-test-1", paramCaptor.getValue().getRequestNo());
        assertEquals("SUCCESS", response.resultCode());
        assertEquals(new BigDecimal("87.50"), response.balanceAfter());
        assertFalse(response.duplicate());
    }

    @Test
    void shouldNotChargeAgainWhenSubscriptionProcedureReturnsDuplicate() {
        when(userService.requireActiveUser()).thenReturn(activeUser());
        doAnswer(invocation -> {
            SubscribeParam param = invocation.getArgument(0);
            param.setResultCode("ALREADY_SUBSCRIBED");
            param.setTxnNo("original-txn");
            param.setBalanceAfter(new BigDecimal("87.50"));
            return null;
        }).when(procRepository).subscribe(any(SubscribeParam.class));
        when(tradeMapper.selectSubscription(101L, 501L))
                .thenReturn(subscriptionItem("ACTIVE", new BigDecimal("12.50"), null));

        SubscribeResponse response = tradeService.subscribe(new SubscribeRequest("501", "SUB-test-1"));

        assertEquals("original-txn", response.txnNo());
        assertEquals(new BigDecimal("87.50"), response.balanceAfter());
        assertTrue(response.duplicate());
        verify(accountMapper, never()).updateById(any(UserAccount.class));
        verify(tradeMapper, never()).insert(any(AccountTransaction.class));
    }

    @Test
    void shouldRefundOnceAndWriteConsistentLedger() {
        when(userService.requireActiveUser()).thenReturn(activeUser());
        UserAccount account = account(new BigDecimal("87.50"));
        when(tradeMapper.selectAccountForUpdate(101L)).thenReturn(account);
        ChapterSubscription subscription = new ChapterSubscription();
        subscription.setUserId(101L);
        subscription.setChapterId(501L);
        subscription.setPaidAmount(new BigDecimal("12.50"));
        subscription.setStatus("ACTIVE");
        when(tradeMapper.selectSubscriptionForUpdate(101L, 501L)).thenReturn(subscription);
        when(tradeMapper.markSubscriptionRefunded(101L, 501L)).thenReturn(1);
        when(tradeMapper.selectSubscription(101L, 501L)).thenReturn(
                subscriptionItem("REFUNDED", new BigDecimal("12.50"), "2026-11-25T12:00:00+08:00"));

        SubscriptionItem result = tradeService.cancelSubscription("501");

        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(accountMapper).updateById(accountCaptor.capture());
        assertEquals(new BigDecimal("100.00"), accountCaptor.getValue().getBalance());

        ArgumentCaptor<AccountTransaction> txnCaptor = ArgumentCaptor.forClass(AccountTransaction.class);
        verify(tradeMapper).insert(txnCaptor.capture());
        assertEquals("REFUND", txnCaptor.getValue().getBizType());
        assertEquals("IN", txnCaptor.getValue().getDirection());
        assertEquals(new BigDecimal("87.50"), txnCaptor.getValue().getBalanceBefore());
        assertEquals(new BigDecimal("100.00"), txnCaptor.getValue().getBalanceAfter());
        assertEquals("REFUND:SUB:101:501", txnCaptor.getValue().getRequestNo());
        assertEquals(new BigDecimal("100.00"), result.balanceAfter());
        assertTrue(result.refunded());
    }

    @Test
    void shouldNotRefundTwice() {
        when(userService.requireActiveUser()).thenReturn(activeUser());
        when(tradeMapper.selectAccountForUpdate(101L)).thenReturn(account(new BigDecimal("100.00")));
        ChapterSubscription subscription = new ChapterSubscription();
        subscription.setStatus("REFUNDED");
        when(tradeMapper.selectSubscriptionForUpdate(101L, 501L)).thenReturn(subscription);
        when(tradeMapper.selectSubscription(101L, 501L)).thenReturn(
                subscriptionItem("REFUNDED", new BigDecimal("12.50"), "2026-11-25T12:00:00+08:00"));

        SubscriptionItem result = tradeService.cancelSubscription("501");

        assertEquals(new BigDecimal("100.00"), result.balanceAfter());
        verify(tradeMapper, never()).markSubscriptionRefunded(any(), any());
        verify(accountMapper, never()).updateById(any(UserAccount.class));
        verify(tradeMapper, never()).insert(any(AccountTransaction.class));
    }

    @Test
    void shouldTipResolvedAuthorThroughProcedure() {
        when(userService.requireActiveUser()).thenReturn(activeUser());
        when(tradeMapper.selectNovelAuthorId(301L)).thenReturn(202L);
        doAnswer(invocation -> {
            TipParam param = invocation.getArgument(0);
            param.setResultCode("SUCCESS");
            param.setTipId(7001L);
            param.setTipNo("tip-no-1");
            param.setFromBalanceAfter(new BigDecimal("68.50"));
            param.setToBalanceAfter(new BigDecimal("19.00"));
            return null;
        }).when(procRepository).tip(any(TipParam.class));

        TipResponse response = tradeService.tip(new TipRequest(
                "301", "501", new BigDecimal("19.00"), "写得很好", "TIP-test-1"));

        ArgumentCaptor<TipParam> paramCaptor = ArgumentCaptor.forClass(TipParam.class);
        verify(procRepository).tip(paramCaptor.capture());
        assertEquals(101L, paramCaptor.getValue().getFromUserId());
        assertEquals(202L, paramCaptor.getValue().getToUserId());
        assertEquals(new BigDecimal("19.00"), paramCaptor.getValue().getAmount());
        assertEquals("7001", response.tipId());
        assertEquals(new BigDecimal("68.50"), response.balanceAfter());
        assertFalse(response.duplicate());
    }

    @Test
    void shouldReturnOriginalTipForDuplicateRequest() {
        when(userService.requireActiveUser()).thenReturn(activeUser());
        when(tradeMapper.selectNovelAuthorId(301L)).thenReturn(202L);
        doAnswer(invocation -> {
            TipParam param = invocation.getArgument(0);
            param.setResultCode("DUPLICATE_REQUEST");
            param.setTipId(7001L);
            param.setTipNo("tip-no-1");
            param.setFromBalanceAfter(new BigDecimal("68.50"));
            return null;
        }).when(procRepository).tip(any(TipParam.class));

        TipResponse response = tradeService.tip(new TipRequest(
                "301", null, new BigDecimal("19.00"), null, "TIP-test-1"));

        assertTrue(response.duplicate());
        assertEquals("tip-no-1", response.tipNo());
        assertEquals(new BigDecimal("68.50"), response.balanceAfter());
        verify(accountMapper, never()).updateById(any(UserAccount.class));
        verify(tradeMapper, never()).insert(any(AccountTransaction.class));
    }

    @Test
    void shouldRejectDuplicateKeyWithDifferentTipParameters() {
        when(userService.requireActiveUser()).thenReturn(activeUser());
        when(tradeMapper.selectNovelAuthorId(301L)).thenReturn(202L);
        doThrow(new DuplicateKeyException("duplicate")).when(procRepository).tip(any(TipParam.class));
        when(tradeMapper.selectTipByRequestNo(101L, "TIP-test-1")).thenReturn(tipItem("501"));

        BusinessException exception = assertThrows(BusinessException.class, () -> tradeService.tip(
                new TipRequest("301", "502", new BigDecimal("19.00"), null, "TIP-test-1")));

        assertEquals(41006, exception.getCode());
        verify(accountMapper, never()).updateById(any(UserAccount.class));
    }

    private AppUser activeUser() {
        AppUser user = new AppUser();
        user.setUserId(101L);
        user.setStatus("ACTIVE");
        return user;
    }

    private UserAccount account(BigDecimal balance) {
        UserAccount account = new UserAccount();
        account.setUserId(101L);
        account.setBalance(balance);
        return account;
    }

    private SubscriptionItem subscriptionItem(String status, BigDecimal paidAmount, String refundedAt) {
        return new SubscriptionItem("501", "301", "测试作品", 1, "第一章",
                paidAmount, paidAmount, "subscribe-txn", "SUB-test-1", status,
                "2026-11-25T11:00:00+08:00", refundedAt, null, null);
    }

    private TipItem tipItem(String chapterId) {
        return new TipItem("7001", "tip-no-1", "TIP-test-1", "101", "202", "作者",
                "301", "测试作品", chapterId, "第一章", new BigDecimal("19.00"), "", "SUCCESS",
                "out-txn", "in-txn", "2026-11-25T11:00:00+08:00", null, "OUT");
    }
}
