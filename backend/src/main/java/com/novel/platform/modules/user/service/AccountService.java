package com.novel.platform.modules.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.novel.platform.common.api.PageParam;
import com.novel.platform.common.api.PageResult;
import com.novel.platform.common.auth.CurrentUser;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.common.exception.ErrorCodes;
import com.novel.platform.modules.user.entity.AccountTransaction;
import com.novel.platform.modules.user.entity.AppUser;
import com.novel.platform.modules.user.entity.UserAccount;
import com.novel.platform.modules.user.mapper.AccountTransactionMapper;
import com.novel.platform.modules.user.mapper.AppUserMapper;
import com.novel.platform.modules.user.mapper.UserAccountMapper;
import com.novel.platform.modules.user.model.Requests;
import com.novel.platform.modules.user.model.Views;
import com.novel.platform.utils.BizNo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class AccountService {

    private final UserAccountMapper userAccountMapper;
    private final AccountTransactionMapper transactionMapper;
    private final AppUserMapper appUserMapper;

    public AccountService(UserAccountMapper userAccountMapper, AccountTransactionMapper transactionMapper,
                          AppUserMapper appUserMapper) {
        this.userAccountMapper = userAccountMapper;
        this.transactionMapper = transactionMapper;
        this.appUserMapper = appUserMapper;
    }

    public Views.AccountInfo info() {
        UserAccount account = userAccountMapper.selectById(CurrentUser.userId());
        if (account == null) {
            throw new IllegalStateException("用户账户不存在，userId=" + CurrentUser.userId());
        }
        return new Views.AccountInfo(String.valueOf(account.getUserId()), account.getBalance(),
                account.getUpdatedAt());
    }

    /**
     * 课程设计范围内的模拟充值：直接写入 RECHARGE 流水，不接入第三方支付。
     */
    @Transactional(rollbackFor = Exception.class)
    public Views.RechargeResult recharge(Requests.Recharge request) {
        Long userId = CurrentUser.userId();
        requireActiveUser(userId);
        String requestNo = request.requestNo().trim();
        String bizKey = "RECHARGE:" + requestNo;
        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);

        UserAccount account = userAccountMapper.selectForUpdate(userId);
        if (account == null) {
            throw new IllegalStateException("用户账户不存在，userId=" + userId);
        }
        AccountTransaction existing = transactionMapper.selectOne(Wrappers.<AccountTransaction>lambdaQuery()
                .eq(AccountTransaction::getBizKey, bizKey));
        if (existing != null) {
            return new Views.RechargeResult(existing.getAmount(), account.getBalance(),
                    existing.getTxnNo(), existing.getCreatedAt());
        }

        BigDecimal before = account.getBalance();
        BigDecimal after = before.add(amount).setScale(2, RoundingMode.HALF_UP);
        String txnNo = BizNo.uuid32();
        LocalDateTime now = LocalDateTime.now();
        recordTransaction(userId, "RECHARGE", "IN", amount, before, after, bizKey, "SYSTEM", null,
                requestNo, "模拟充值", txnNo, now);

        account.setBalance(after);
        userAccountMapper.updateById(account);

        return new Views.RechargeResult(amount, after, txnNo, now);
    }

    public PageResult<Views.TransactionItem> transactions(Integer page, Integer pageSize,
                                                          String bizType, String direction) {
        PageParam param = PageParam.of(page, pageSize);
        Page<AccountTransaction> result = transactionMapper.selectPage(new Page<>(param.page(), param.pageSize()),
                Wrappers.<AccountTransaction>lambdaQuery()
                        .eq(AccountTransaction::getUserId, CurrentUser.userId())
                        .eq(bizType != null && !bizType.isBlank(), AccountTransaction::getBizType, bizType)
                        .eq(direction != null && !direction.isBlank(), AccountTransaction::getDirection, direction)
                        .orderByDesc(AccountTransaction::getCreatedAt));
        return PageResult.of(result.getRecords(), result.getTotal(), param.page(), param.pageSize(),
                txn -> new Views.TransactionItem(txn.getTxnNo(), txn.getBizType(), txn.getDirection(),
                        txn.getAmount(), txn.getBalanceBefore(), txn.getBalanceAfter(), txn.getBizKey(),
                        txn.getRelatedType(),
                        txn.getRelatedId() == null ? null : String.valueOf(txn.getRelatedId()),
                        txn.getRequestNo(), txn.getRemark(), txn.getCreatedAt()));
    }

    /**
     * 写入一条账户流水（只追加），由订阅、打赏、充值共用。
     */
    public void recordTransaction(Long userId, String bizType, String direction, BigDecimal amount,
                                  BigDecimal balanceBefore, BigDecimal balanceAfter, String bizKey,
                                  String relatedType, Long relatedId, String requestNo, String remark,
                                  String txnNo, LocalDateTime createdAt) {
        AccountTransaction txn = new AccountTransaction();
        txn.setTxnNo(txnNo);
        txn.setUserId(userId);
        txn.setBizType(bizType);
        txn.setDirection(direction);
        txn.setAmount(amount);
        txn.setBalanceBefore(balanceBefore);
        txn.setBalanceAfter(balanceAfter);
        txn.setBizKey(bizKey);
        txn.setRelatedType(relatedType);
        txn.setRelatedId(relatedId);
        txn.setRequestNo(requestNo);
        txn.setRemark(remark == null ? "" : remark);
        txn.setCreatedAt(createdAt);
        transactionMapper.insert(txn);
    }

    private void requireActiveUser(Long userId) {
        AppUser user = appUserMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.unauthorized("未登录或登录已失效");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCodes.ACCOUNT_RESTRICTED,
                    org.springframework.http.HttpStatus.FORBIDDEN, "账号已被锁定或禁用，无法进行交易");
        }
    }
}
