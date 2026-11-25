package com.novel.platform.modules.admin.dto;

import java.math.BigDecimal;

public class DashboardStatsRow {

    private long userCount;
    private long novelCount;
    private long commentCount;
    private long subscriptionCount;
    private BigDecimal subscriptionAmount;
    private long tipCount;
    private BigDecimal tipAmount;
    private long pendingNovelCount;
    private long pendingChapterCount;
    private long pendingCommentCount;
    private long pendingRatingCount;

    public long getUserCount() {
        return userCount;
    }

    public void setUserCount(long userCount) {
        this.userCount = userCount;
    }

    public long getNovelCount() {
        return novelCount;
    }

    public void setNovelCount(long novelCount) {
        this.novelCount = novelCount;
    }

    public long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(long commentCount) {
        this.commentCount = commentCount;
    }

    public long getSubscriptionCount() {
        return subscriptionCount;
    }

    public void setSubscriptionCount(long subscriptionCount) {
        this.subscriptionCount = subscriptionCount;
    }

    public BigDecimal getSubscriptionAmount() {
        return subscriptionAmount;
    }

    public void setSubscriptionAmount(BigDecimal subscriptionAmount) {
        this.subscriptionAmount = subscriptionAmount;
    }

    public long getTipCount() {
        return tipCount;
    }

    public void setTipCount(long tipCount) {
        this.tipCount = tipCount;
    }

    public BigDecimal getTipAmount() {
        return tipAmount;
    }

    public void setTipAmount(BigDecimal tipAmount) {
        this.tipAmount = tipAmount;
    }

    public long getPendingNovelCount() {
        return pendingNovelCount;
    }

    public void setPendingNovelCount(long pendingNovelCount) {
        this.pendingNovelCount = pendingNovelCount;
    }

    public long getPendingChapterCount() {
        return pendingChapterCount;
    }

    public void setPendingChapterCount(long pendingChapterCount) {
        this.pendingChapterCount = pendingChapterCount;
    }

    public long getPendingCommentCount() {
        return pendingCommentCount;
    }

    public void setPendingCommentCount(long pendingCommentCount) {
        this.pendingCommentCount = pendingCommentCount;
    }

    public long getPendingRatingCount() {
        return pendingRatingCount;
    }

    public void setPendingRatingCount(long pendingRatingCount) {
        this.pendingRatingCount = pendingRatingCount;
    }
}
