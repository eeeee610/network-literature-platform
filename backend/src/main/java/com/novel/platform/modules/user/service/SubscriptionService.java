package com.novel.platform.modules.user.service;

import com.novel.platform.common.api.PageParam;
import com.novel.platform.common.api.PageResult;
import com.novel.platform.common.auth.CurrentUser;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.common.exception.ErrorCodes;
import com.novel.platform.modules.user.entity.AppUser;
import com.novel.platform.modules.user.entity.ChapterSubscription;
import com.novel.platform.modules.user.entity.UserAccount;
import com.novel.platform.modules.user.mapper.AppUserMapper;
import com.novel.platform.modules.user.mapper.ChapterSubscriptionMapper;
import com.novel.platform.modules.user.mapper.NovelQueryMapper;
import com.novel.platform.modules.user.mapper.UserAccountMapper;
import com.novel.platform.modules.user.model.Requests;
import com.novel.platform.modules.user.model.Rows;
import com.novel.platform.modules.user.model.Views;
import com.novel.platform.utils.BizNo;
import com.novel.platform.utils.Ids;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SubscriptionService {

    private final ChapterSubscriptionMapper subscriptionMapper;
    private final UserAccountMapper userAccountMapper;
    private final AppUserMapper appUserMapper;
    private final NovelQueryMapper novelQueryMapper;
    private final AccountService accountService;

    public SubscriptionService(ChapterSubscriptionMapper subscriptionMapper, UserAccountMapper userAccountMapper,
                               AppUserMapper appUserMapper, NovelQueryMapper novelQueryMapper,
                               AccountService accountService) {
        this.subscriptionMapper = subscriptionMapper;
        this.userAccountMapper = userAccountMapper;
        this.appUserMapper = appUserMapper;
        this.novelQueryMapper = novelQueryMapper;
        this.accountService = accountService;
    }

    public Views.ChapterPurchase purchaseInfo(String chapterIdRaw) {
        Long chapterId = Ids.parse(chapterIdRaw, "chapterId");
        Rows.ChapterBrief chapter = novelQueryMapper.selectChapter(chapterId);
        if (!isChapterVisible(chapter)) {
            throw BusinessException.notFound(ErrorCodes.CHAPTER_NOT_AVAILABLE, "章节不存在或暂不可购买");
        }
        ChapterSubscription subscription = subscriptionMapper.selectOne(CurrentUser.userId(), chapterId);
        return new Views.ChapterPurchase(
                String.valueOf(chapter.chapterId), String.valueOf(chapter.novelId), chapter.novelTitle,
                chapter.title, chapter.chapterNo, chapter.isVip, chapter.vipPrice,
                subscription != null,
                subscription == null ? null : subscription.getSubscribedAt());
    }

    @Transactional(rollbackFor = Exception.class)
    public Views.SubscribeResult subscribe(Requests.Subscribe request) {
        Long userId = CurrentUser.userId();
        Long novelId = Ids.parse(request.novelId(), "novelId");
        Long chapterId = Ids.parse(request.chapterId(), "chapterId");
        String requestNo = request.requestNo().trim();

        ChapterSubscription sameRequest = subscriptionMapper.selectByRequestNo(requestNo);
        if (sameRequest != null) {
            if (!sameRequest.getUserId().equals(userId) || !sameRequest.getChapterId().equals(chapterId)) {
                throw BusinessException.conflict(ErrorCodes.REQUEST_NO_CONFLICT, "requestNo 已被其他订阅请求使用");
            }
            return replayResult(sameRequest);
        }

        ChapterSubscription existing = subscriptionMapper.selectOne(userId, chapterId);
        if (existing != null) {
            return replayResult(existing);
        }

        Rows.ChapterBrief chapter = novelQueryMapper.selectChapter(chapterId);
        if (!isChapterVisible(chapter) || !chapter.novelId.equals(novelId)) {
            throw BusinessException.notFound(ErrorCodes.CHAPTER_NOT_AVAILABLE, "章节不存在或暂不可购买");
        }
        BigDecimal price = chapter.vipPrice;
        if (chapter.isVip == null || chapter.isVip != 1 || price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCodes.CHAPTER_FREE,
                    org.springframework.http.HttpStatus.BAD_REQUEST, "该章节为免费章节，无需订阅");
        }

        requireActiveUser(userId);

        UserAccount account = userAccountMapper.selectForUpdate(userId);
        if (account == null) {
            throw new IllegalStateException("用户账户不存在，userId=" + userId);
        }
        BigDecimal before = account.getBalance();
        if (before.compareTo(price) < 0) {
            throw new BusinessException(ErrorCodes.BALANCE_INSUFFICIENT,
                    org.springframework.http.HttpStatus.BAD_REQUEST, "余额不足，请先充值");
        }
        BigDecimal after = before.subtract(price).setScale(2, java.math.RoundingMode.HALF_UP);
        String txnNo = BizNo.uuid32();
        LocalDateTime now = LocalDateTime.now();

        ChapterSubscription subscription = new ChapterSubscription();
        subscription.setUserId(userId);
        subscription.setChapterId(chapterId);
        subscription.setNovelId(novelId);
        subscription.setUnitPrice(price);
        subscription.setPaidAmount(price);
        subscription.setTxnNo(txnNo);
        subscription.setRequestNo(requestNo);
        subscription.setStatus("ACTIVE");
        subscription.setSubscribedAt(now);
        subscriptionMapper.insertOne(subscription);

        accountService.recordTransaction(userId, "SUBSCRIBE", "OUT", price, before, after,
                "SUB:" + userId + ":" + chapterId, "CHAPTER", chapterId, requestNo,
                "订阅《" + chapter.novelTitle + "》第" + chapter.chapterNo + "章", txnNo, now);

        account.setBalance(after);
        userAccountMapper.updateById(account);

        return new Views.SubscribeResult(String.valueOf(chapterId), String.valueOf(novelId),
                price, after, now);
    }

    public PageResult<Views.SubscriptionItem> list(Integer page, Integer pageSize) {
        PageParam param = PageParam.of(page, pageSize);
        Long userId = CurrentUser.userId();
        long total = subscriptionMapper.countByUser(userId);
        List<Rows.SubscriptionRow> rows = subscriptionMapper.selectPage(userId, param.offset(), param.pageSize());
        return PageResult.of(rows, total, param.page(), param.pageSize(),
                row -> new Views.SubscriptionItem(String.valueOf(row.chapterId), String.valueOf(row.novelId),
                        row.novelTitle, row.chapterTitle, row.chapterNo, row.unitPrice, row.paidAmount,
                        row.status, row.subscribedAt));
    }

    public Views.SubscriptionCheck check(String chapterIdRaw) {
        Long chapterId = Ids.parse(chapterIdRaw, "chapterId");
        ChapterSubscription subscription = subscriptionMapper.selectOne(CurrentUser.userId(), chapterId);
        return new Views.SubscriptionCheck(String.valueOf(chapterId), subscription != null,
                subscription == null ? null : subscription.getSubscribedAt());
    }

    private Views.SubscribeResult replayResult(ChapterSubscription subscription) {
        UserAccount account = userAccountMapper.selectById(subscription.getUserId());
        BigDecimal balance = account == null ? BigDecimal.ZERO : account.getBalance();
        return new Views.SubscribeResult(String.valueOf(subscription.getChapterId()),
                String.valueOf(subscription.getNovelId()), subscription.getPaidAmount(), balance,
                subscription.getSubscribedAt());
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

    private boolean isChapterVisible(Rows.ChapterBrief chapter) {
        if (chapter == null) {
            return false;
        }
        boolean novelVisible = chapter.novelAuditStatus != null && chapter.novelAuditStatus == 1
                && chapter.novelStatus != null && (chapter.novelStatus == 0 || chapter.novelStatus == 1);
        boolean chapterVisible = chapter.status != null && chapter.status == 1
                && chapter.auditStatus != null && chapter.auditStatus == 1
                && chapter.publishTime != null;
        return novelVisible && chapterVisible;
    }
}
