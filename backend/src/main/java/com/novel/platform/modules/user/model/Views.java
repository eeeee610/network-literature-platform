package com.novel.platform.modules.user.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 接口响应视图：BIGINT ID 一律以字符串返回，金额保留两位小数。
 */
public final class Views {

    private Views() {
    }

    public record UserInfo(String userId, String username, String nickname, String avatarUrl, String role) {
    }

    public record LoginResult(String token, String tokenType, long expiresIn, UserInfo user) {
    }

    public record RegisterResult(String userId, String username, String nickname, String role) {
    }

    public record BookshelfItem(String novelId, String title, String coverUrl, Integer status,
                                Integer totalWords, String latestChapterId, String latestChapterTitle,
                                String lastReadChapterId, String lastReadTitle, LocalDateTime lastReadAt,
                                Boolean isTop, Boolean isNotify, LocalDateTime updatedAt) {
    }

    public record ShelfStatus(boolean inShelf, Boolean isTop, Boolean isNotify, String lastReadChapterId) {
    }

    public record ChapterPurchase(String chapterId, String novelId, String novelTitle, String chapterTitle,
                                  Integer chapterNo, Integer isVip, BigDecimal vipPrice,
                                  boolean subscribed, LocalDateTime subscribedAt) {
    }

    public record SubscriptionItem(String chapterId, String novelId, String novelTitle, String chapterTitle,
                                   Integer chapterNo, BigDecimal unitPrice, BigDecimal paidAmount,
                                   String status, LocalDateTime subscribedAt) {
    }

    public record SubscribeResult(String chapterId, String novelId, BigDecimal paidAmount,
                                  BigDecimal balance, LocalDateTime subscribedAt) {
    }

    public record SubscriptionCheck(String chapterId, boolean subscribed, LocalDateTime subscribedAt) {
    }

    public record RewardTarget(String novelId, String novelTitle, String coverUrl,
                               String authorId, String authorName) {
    }

    public record RewardResult(String tipId, BigDecimal amount, BigDecimal balance, LocalDateTime createdAt) {
    }

    public record RewardItem(String tipId, String novelId, String novelTitle, String chapterId,
                             String chapterTitle, String fromUserId, String fromNickname,
                             String toUserId, String toNickname, BigDecimal amount, String message,
                             String status, LocalDateTime createdAt) {
    }

    public record AccountInfo(String userId, BigDecimal balance, LocalDateTime updatedAt) {
    }

    public record RechargeResult(BigDecimal amount, BigDecimal balance, String txnNo, LocalDateTime createdAt) {
    }

    public record TransactionItem(String txnNo, String bizType, String direction, BigDecimal amount,
                                  BigDecimal balanceBefore, BigDecimal balanceAfter, String bizKey,
                                  String relatedType, String relatedId, String requestNo, String remark,
                                  LocalDateTime createdAt) {
    }

    public record AdminUserItem(String userId, String username, String nickname, String email,
                                String role, String status, LocalDateTime createdAt) {
    }
}
