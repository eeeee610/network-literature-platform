package com.novel.platform.modules.user.service;

import com.novel.platform.common.api.PageResult;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.novel.service.NovelVisibilityService;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class TradeService {

    private final UserService userService;
    private final UserTradeMapper tradeMapper;
    private final UserAccountMapper accountMapper;
    private final TradeProcRepository procRepository;
    private final NovelVisibilityService visibilityService;

    public TradeService(UserService userService, UserTradeMapper tradeMapper,
                        UserAccountMapper accountMapper, TradeProcRepository procRepository,
                        NovelVisibilityService visibilityService) {
        this.userService = userService;
        this.tradeMapper = tradeMapper;
        this.accountMapper = accountMapper;
        this.procRepository = procRepository;
        this.visibilityService = visibilityService;
    }

    public SubscribeResponse subscribe(SubscribeRequest request) {
        AppUser user = userService.requireActiveUser();
        long chapterId = UserSupport.id(request.chapterId(), "chapterId");
        String requestNo = request.requestNo().trim();
        if (tradeMapper.selectSubscriptionByRequestNo(user.getUserId(), requestNo) != null) {
            return duplicateSubscription(user.getUserId(), chapterId, requestNo);
        }
        SubscriptionItem existing = tradeMapper.selectSubscription(user.getUserId(), chapterId);
        if (existing != null) {
            return alreadySubscribed(user.getUserId(), existing);
        }
        if (!visibilityService.isPublicChapter(chapterId)) {
            throw new BusinessException(41004, "作品或章节不存在或未公开", HttpStatus.NOT_FOUND);
        }
        SubscribeParam param = new SubscribeParam();
        param.setUserId(user.getUserId());
        param.setChapterId(chapterId);
        param.setRequestNo(requestNo);
        try {
            procRepository.subscribe(param);
        } catch (DuplicateKeyException exception) {
            return duplicateSubscription(user.getUserId(), chapterId, param.getRequestNo());
        }
        SubscriptionItem saved = tradeMapper.selectSubscription(user.getUserId(), chapterId);
        if (saved == null || param.getTxnNo() == null || param.getBalanceAfter() == null) {
            throw new IllegalStateException("订阅过程未返回完整结果");
        }
        return new SubscribeResponse(saved.chapterId(), param.getResultCode(), param.getTxnNo(),
                saved.paidAmount(), param.getBalanceAfter(), "ALREADY_SUBSCRIBED".equals(param.getResultCode()));
    }

    public PageResult<SubscriptionItem> subscriptions(Integer pageRaw, Integer pageSizeRaw) {
        AppUser user = userService.requireActiveUser();
        int page = UserSupport.page(pageRaw);
        int pageSize = UserSupport.pageSize(pageSizeRaw);
        long total = tradeMapper.countSubscriptions(user.getUserId());
        List<SubscriptionItem> items = total == 0 ? List.of() : tradeMapper.selectSubscriptionPage(
                user.getUserId(), UserSupport.offset(page, pageSize), pageSize);
        return new PageResult<>(items, total, page, pageSize);
    }

    @Transactional
    public SubscriptionItem cancelSubscription(String chapterIdRaw) {
        AppUser user = userService.requireActiveUser();
        long chapterId = UserSupport.id(chapterIdRaw, "chapterId");
        UserAccount account = requireAccountForUpdate(user.getUserId());
        ChapterSubscription subscription = tradeMapper.selectSubscriptionForUpdate(user.getUserId(), chapterId);
        if (subscription == null) {
            throw new BusinessException(41010, "订阅记录不存在", HttpStatus.NOT_FOUND);
        }
        if ("REFUNDED".equals(subscription.getStatus())) {
            SubscriptionItem existing = tradeMapper.selectSubscription(user.getUserId(), chapterId);
            return withRefund(existing, account.getBalance(), true);
        }

        BigDecimal before = account.getBalance();
        BigDecimal after = before.add(subscription.getPaidAmount());
        if (tradeMapper.markSubscriptionRefunded(user.getUserId(), chapterId) != 1) {
            throw new IllegalStateException("订阅状态更新失败");
        }
        UserAccount update = new UserAccount();
        update.setUserId(user.getUserId());
        update.setBalance(after);
        accountMapper.updateById(update);

        AccountTransaction refund = new AccountTransaction();
        refund.setTxnNo(UUID.randomUUID().toString().replace("-", ""));
        refund.setUserId(user.getUserId());
        refund.setBizType("REFUND");
        refund.setDirection("IN");
        refund.setAmount(subscription.getPaidAmount());
        refund.setBalanceBefore(before);
        refund.setBalanceAfter(after);
        refund.setBizKey("REFUND:SUB:" + user.getUserId() + ":" + chapterId);
        refund.setRelatedType("CHAPTER");
        refund.setRelatedId(chapterId);
        refund.setRequestNo("REFUND:SUB:" + user.getUserId() + ":" + chapterId);
        refund.setRemark("取消订阅章节 " + chapterId);
        tradeMapper.insert(refund);

        return withRefund(tradeMapper.selectSubscription(user.getUserId(), chapterId), after, true);
    }

    public TipResponse tip(TipRequest request) {
        AppUser user = userService.requireActiveUser();
        long novelId = UserSupport.id(request.novelId(), "novelId");
        Long chapterId = UserSupport.nullableId(request.chapterId(), "chapterId");
        String requestNo = request.requestNo().trim();
        if (tradeMapper.selectTipByRequestNo(user.getUserId(), requestNo) != null) {
            return duplicateTip(user.getUserId(), novelId, chapterId, request.amount(), requestNo);
        }
        Long authorId = tradeMapper.selectNovelAuthorId(novelId);
        if (authorId == null || !visibilityService.isPublicNovel(novelId)) {
            throw new BusinessException(41004, "作品不存在或未公开", HttpStatus.NOT_FOUND);
        }

        TipParam param = new TipParam();
        param.setFromUserId(user.getUserId());
        param.setToUserId(authorId);
        param.setNovelId(novelId);
        param.setChapterId(chapterId);
        param.setAmount(request.amount());
        param.setMessage(request.message() == null ? "" : request.message().trim());
        param.setRequestNo(requestNo);
        try {
            procRepository.tip(param);
        } catch (DuplicateKeyException exception) {
            return duplicateTip(user.getUserId(), novelId, chapterId, request.amount(), param.getRequestNo());
        }
        if (param.getTipId() == null || param.getTipNo() == null || param.getFromBalanceAfter() == null) {
            throw new IllegalStateException("打赏过程未返回完整结果");
        }
        return new TipResponse(String.valueOf(param.getTipId()), param.getTipNo(), param.getResultCode(),
                request.amount(), param.getFromBalanceAfter(), "DUPLICATE_REQUEST".equals(param.getResultCode()));
    }

    public PageResult<TipItem> tips(String directionRaw, Integer pageRaw, Integer pageSizeRaw) {
        AppUser user = userService.requireActiveUser();
        String direction = directionRaw == null || directionRaw.isBlank()
                ? "OUT" : directionRaw.trim().toUpperCase();
        if (!"OUT".equals(direction) && !"IN".equals(direction)) {
            throw UserSupport.badRequest("direction: 仅支持 OUT 或 IN");
        }
        int page = UserSupport.page(pageRaw);
        int pageSize = UserSupport.pageSize(pageSizeRaw);
        long total = tradeMapper.countTips(user.getUserId(), direction);
        List<TipItem> items = total == 0 ? List.of() : tradeMapper.selectTipPage(
                user.getUserId(), direction, UserSupport.offset(page, pageSize), pageSize);
        return new PageResult<>(items, total, page, pageSize);
    }

    private SubscribeResponse duplicateSubscription(Long userId, long chapterId, String requestNo) {
        SubscriptionItem existing = tradeMapper.selectSubscriptionByRequestNo(userId, requestNo);
        if (existing == null || !String.valueOf(chapterId).equals(existing.chapterId())) {
            throw new BusinessException(41006, "同一请求号的参数与已有订阅不一致", HttpStatus.CONFLICT);
        }
        return alreadySubscribed(userId, existing);
    }

    private SubscribeResponse alreadySubscribed(Long userId, SubscriptionItem existing) {
        UserAccount account = accountMapper.selectById(userId);
        return new SubscribeResponse(existing.chapterId(), "ALREADY_SUBSCRIBED", existing.txnNo(),
                existing.paidAmount(), account.getBalance(), true);
    }

    private TipResponse duplicateTip(Long userId, long novelId, Long chapterId,
                                     BigDecimal amount, String requestNo) {
        TipItem existing = tradeMapper.selectTipByRequestNo(userId, requestNo);
        String expectedChapterId = chapterId == null ? null : String.valueOf(chapterId);
        if (existing == null || !String.valueOf(novelId).equals(existing.novelId())
                || !java.util.Objects.equals(expectedChapterId, existing.chapterId())
                || existing.amount().compareTo(amount) != 0) {
            throw new BusinessException(41006, "同一请求号的参数与已有打赏不一致", HttpStatus.CONFLICT);
        }
        UserAccount account = accountMapper.selectById(userId);
        return new TipResponse(existing.tipId(), existing.tipNo(), "DUPLICATE_REQUEST",
                existing.amount(), account.getBalance(), true);
    }

    private UserAccount requireAccountForUpdate(Long userId) {
        UserAccount account = tradeMapper.selectAccountForUpdate(userId);
        if (account == null) {
            throw new BusinessException(41008, "用户账户不存在", HttpStatus.NOT_FOUND);
        }
        return account;
    }

    private SubscriptionItem withRefund(SubscriptionItem item, BigDecimal balance, boolean refunded) {
        return new SubscriptionItem(item.chapterId(), item.novelId(), item.novelTitle(), item.chapterNo(),
                item.chapterTitle(), item.unitPrice(), item.paidAmount(), item.txnNo(), item.requestNo(),
                item.status(), item.subscribedAt(), item.refundedAt(), balance, refunded);
    }
}
