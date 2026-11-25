package com.novel.platform.modules.user.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 数据库查询结果行：字段与 SQL 别名一一对应，仅供 Mapper 与 Service 内部使用。
 */
public final class Rows {

    private Rows() {
    }

    /** 作品只读快照（B 模块冻结表，A 模块只读） */
    public static class NovelBrief {
        public Long novelId;
        public Long authorId;
        public String title;
        public String coverUrl;
        public Integer status;
        public Integer auditStatus;
    }

    /** 章节只读快照（含所属作品可见性） */
    public static class ChapterBrief {
        public Long chapterId;
        public Long novelId;
        public Integer chapterNo;
        public String title;
        public Integer isVip;
        public BigDecimal vipPrice;
        public Integer status;
        public Integer auditStatus;
        public LocalDateTime publishTime;
        public String novelTitle;
        public Integer novelStatus;
        public Integer novelAuditStatus;
        public Long authorId;
    }

    /** 书架列表行 */
    public static class BookshelfRow {
        public Long novelId;
        public String title;
        public String coverUrl;
        public Integer status;
        public Integer totalWords;
        public Long latestChapterId;
        public String latestChapterTitle;
        public Long lastReadChapterId;
        public String lastReadTitle;
        public LocalDateTime lastReadAt;
        public Boolean isTop;
        public Boolean isNotify;
        public LocalDateTime updatedAt;
    }

    /** 订阅记录行 */
    public static class SubscriptionRow {
        public Long chapterId;
        public Long novelId;
        public String novelTitle;
        public String chapterTitle;
        public Integer chapterNo;
        public BigDecimal unitPrice;
        public BigDecimal paidAmount;
        public String status;
        public LocalDateTime subscribedAt;
    }

    /** 打赏记录行 */
    public static class RewardRow {
        public Long tipId;
        public Long novelId;
        public String novelTitle;
        public Long chapterId;
        public String chapterTitle;
        public Long fromUserId;
        public String fromNickname;
        public Long toUserId;
        public String toNickname;
        public BigDecimal amount;
        public String message;
        public String status;
        public LocalDateTime createdAt;
    }

    /** 账户流水行 */
    public static class TransactionRow {
        public String txnNo;
        public String bizType;
        public String direction;
        public BigDecimal amount;
        public BigDecimal balanceBefore;
        public BigDecimal balanceAfter;
        public String bizKey;
        public String relatedType;
        public Long relatedId;
        public String requestNo;
        public String remark;
        public LocalDateTime createdAt;
    }

    /** 后台用户列表行 */
    public static class AdminUserRow {
        public Long userId;
        public String username;
        public String nickname;
        public String email;
        public String role;
        public String status;
        public LocalDateTime createdAt;
    }
}
