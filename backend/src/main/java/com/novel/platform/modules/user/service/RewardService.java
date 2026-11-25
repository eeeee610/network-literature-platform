package com.novel.platform.modules.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.novel.platform.common.api.PageParam;
import com.novel.platform.common.api.PageResult;
import com.novel.platform.common.auth.CurrentUser;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.common.exception.ErrorCodes;
import com.novel.platform.modules.user.entity.AppUser;
import com.novel.platform.modules.user.entity.RewardTip;
import com.novel.platform.modules.user.entity.UserAccount;
import com.novel.platform.modules.user.mapper.AppUserMapper;
import com.novel.platform.modules.user.mapper.NovelQueryMapper;
import com.novel.platform.modules.user.mapper.RewardTipMapper;
import com.novel.platform.modules.user.mapper.UserAccountMapper;
import com.novel.platform.modules.user.model.Requests;
import com.novel.platform.modules.user.model.Rows;
import com.novel.platform.modules.user.model.Views;
import com.novel.platform.utils.BizNo;
import com.novel.platform.utils.Ids;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RewardService {

    private final RewardTipMapper rewardTipMapper;
    private final NovelQueryMapper novelQueryMapper;
    private final AppUserMapper appUserMapper;
    private final UserAccountMapper userAccountMapper;
    private final AccountService accountService;

    public RewardService(RewardTipMapper rewardTipMapper, NovelQueryMapper novelQueryMapper,
                         AppUserMapper appUserMapper, UserAccountMapper userAccountMapper,
                         AccountService accountService) {
        this.rewardTipMapper = rewardTipMapper;
        this.novelQueryMapper = novelQueryMapper;
        this.appUserMapper = appUserMapper;
        this.userAccountMapper = userAccountMapper;
        this.accountService = accountService;
    }

    public Views.RewardTarget target(String novelIdRaw) {
        Long novelId = Ids.parse(novelIdRaw, "novelId");
        Rows.NovelBrief novel = novelQueryMapper.selectNovel(novelId);
        if (!isNovelPublic(novel)) {
            throw BusinessException.notFound(ErrorCodes.NOVEL_NOT_FOUND, "作品不存在或已下架");
        }
        String authorName = novelQueryMapper.selectNickname(novel.authorId);
        return new Views.RewardTarget(String.valueOf(novel.novelId), novel.title, novel.coverUrl,
                String.valueOf(novel.authorId), authorName == null ? "未知作者" : authorName);
    }

    @Transactional(rollbackFor = Exception.class)
    public Views.RewardResult reward(Requests.Reward request) {
        Long fromUserId = CurrentUser.userId();
        Long novelId = Ids.parse(request.novelId(), "novelId");
        String requestNo = request.requestNo().trim();
        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        String message = request.message() == null ? "" : request.message().trim();

        Rows.NovelBrief novel = novelQueryMapper.selectNovel(novelId);
        if (!isNovelPublic(novel)) {
            throw BusinessException.notFound(ErrorCodes.NOVEL_NOT_FOUND, "作品不存在或已下架");
        }
        Long toUserId = novel.authorId;
        if (toUserId.equals(fromUserId)) {
            throw new BusinessException(ErrorCodes.REWARD_SELF,
                    org.springframework.http.HttpStatus.BAD_REQUEST, "不能打赏自己的作品");
        }

        Long chapterId = null;
        if (request.chapterId() != null && !request.chapterId().isBlank()) {
            chapterId = Ids.parse(request.chapterId(), "chapterId");
            Rows.ChapterBrief chapter = novelQueryMapper.selectChapter(chapterId);
            if (chapter == null || !chapter.novelId.equals(novelId)) {
                throw BusinessException.notFound(ErrorCodes.CHAPTER_NOT_AVAILABLE, "章节不存在或不属于该作品");
            }
        }

        RewardTip sameRequest = rewardTipMapper.selectOne(Wrappers.<RewardTip>lambdaQuery()
                .eq(RewardTip::getRequestNo, requestNo));
        if (sameRequest != null) {
            if (!sameRequest.getFromUserId().equals(fromUserId)) {
                throw BusinessException.conflict(ErrorCodes.REQUEST_NO_CONFLICT, "requestNo 已被其他打赏请求使用");
            }
            return replayResult(sameRequest);
        }

        requireActiveUser(fromUserId);

        UserAccount firstLocked = userAccountMapper.selectForUpdate(Math.min(fromUserId, toUserId));
        UserAccount secondLocked = userAccountMapper.selectForUpdate(Math.max(fromUserId, toUserId));
        UserAccount fromAccount = fromUserId < toUserId ? firstLocked : secondLocked;
        UserAccount toAccount = fromUserId < toUserId ? secondLocked : firstLocked;
        if (fromAccount == null) {
            throw new IllegalStateException("打赏者账户不存在，userId=" + fromUserId);
        }
        if (toAccount == null) {
            throw new IllegalStateException("作者账户不存在，userId=" + toUserId);
        }
        BigDecimal fromBefore = fromAccount.getBalance();
        if (fromBefore.compareTo(amount) < 0) {
            throw new BusinessException(ErrorCodes.BALANCE_INSUFFICIENT,
                    org.springframework.http.HttpStatus.BAD_REQUEST, "余额不足，请先充值");
        }
        BigDecimal fromAfter = fromBefore.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal toBefore = toAccount.getBalance();
        BigDecimal toAfter = toBefore.add(amount).setScale(2, RoundingMode.HALF_UP);

        String tipNo = BizNo.uuid32();
        String outTxnNo = BizNo.uuid32();
        String inTxnNo = BizNo.uuid32();
        LocalDateTime now = LocalDateTime.now();

        RewardTip tip = new RewardTip();
        tip.setTipNo(tipNo);
        tip.setRequestNo(requestNo);
        tip.setFromUserId(fromUserId);
        tip.setToUserId(toUserId);
        tip.setNovelId(novelId);
        tip.setChapterId(chapterId);
        tip.setAmount(amount);
        tip.setMessage(message);
        tip.setStatus("SUCCESS");
        tip.setOutTxnNo(outTxnNo);
        tip.setInTxnNo(inTxnNo);
        tip.setCreatedAt(now);
        rewardTipMapper.insert(tip);

        String remark = "打赏《" + novel.title + "》";
        accountService.recordTransaction(fromUserId, "TIP_OUT", "OUT", amount, fromBefore, fromAfter,
                "TIP_OUT:" + tip.getTipId(), "TIP", tip.getTipId(), requestNo, remark, outTxnNo, now);
        accountService.recordTransaction(toUserId, "TIP_IN", "IN", amount, toBefore, toAfter,
                "TIP_IN:" + tip.getTipId(), "TIP", tip.getTipId(), requestNo, "收到" + remark, inTxnNo, now);

        fromAccount.setBalance(fromAfter);
        userAccountMapper.updateById(fromAccount);
        toAccount.setBalance(toAfter);
        userAccountMapper.updateById(toAccount);

        return new Views.RewardResult(String.valueOf(tip.getTipId()), amount, fromAfter, now);
    }

    public PageResult<Views.RewardItem> sent(Integer page, Integer pageSize) {
        PageParam param = PageParam.of(page, pageSize);
        Long userId = CurrentUser.userId();
        long total = rewardTipMapper.countSent(userId);
        List<Rows.RewardRow> rows = rewardTipMapper.selectSent(userId, param.offset(), param.pageSize());
        return PageResult.of(rows, total, param.page(), param.pageSize(), this::toItem);
    }

    public PageResult<Views.RewardItem> received(Integer page, Integer pageSize) {
        PageParam param = PageParam.of(page, pageSize);
        Long userId = CurrentUser.userId();
        long total = rewardTipMapper.countReceived(userId);
        List<Rows.RewardRow> rows = rewardTipMapper.selectReceived(userId, param.offset(), param.pageSize());
        return PageResult.of(rows, total, param.page(), param.pageSize(), this::toItem);
    }

    private Views.RewardResult replayResult(RewardTip tip) {
        UserAccount account = userAccountMapper.selectById(tip.getFromUserId());
        BigDecimal balance = account == null ? BigDecimal.ZERO : account.getBalance();
        return new Views.RewardResult(String.valueOf(tip.getTipId()), tip.getAmount(), balance,
                tip.getCreatedAt());
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

    private boolean isNovelPublic(Rows.NovelBrief novel) {
        return novel != null && novel.auditStatus != null && novel.auditStatus == 1
                && novel.status != null && (novel.status == 0 || novel.status == 1);
    }

    private Views.RewardItem toItem(Rows.RewardRow row) {
        return new Views.RewardItem(String.valueOf(row.tipId), String.valueOf(row.novelId), row.novelTitle,
                row.chapterId == null ? null : String.valueOf(row.chapterId), row.chapterTitle,
                String.valueOf(row.fromUserId), row.fromNickname, String.valueOf(row.toUserId),
                row.toNickname, row.amount, row.message, row.status, row.createdAt);
    }
}
