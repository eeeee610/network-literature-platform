package com.novel.platform.modules.interaction.dto;

import java.time.LocalDateTime;

/**
 * 评分查询行：评分记录 + 用户/作品展示信息。
 */
public class RatingRow {

    private Long ratingId;
    private Long userId;
    private Long novelId;
    private Integer score;
    private String reviewContent;
    private Integer status;
    private Integer reviewAuditStatus;
    private String nickname;
    private String avatarUrl;
    private String novelTitle;
    private LocalDateTime createdAt;

    public Long getRatingId() {
        return ratingId;
    }

    public void setRatingId(Long ratingId) {
        this.ratingId = ratingId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getReviewContent() {
        return reviewContent;
    }

    public void setReviewContent(String reviewContent) {
        this.reviewContent = reviewContent;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getReviewAuditStatus() {
        return reviewAuditStatus;
    }

    public void setReviewAuditStatus(Integer reviewAuditStatus) {
        this.reviewAuditStatus = reviewAuditStatus;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getNovelTitle() {
        return novelTitle;
    }

    public void setNovelTitle(String novelTitle) {
        this.novelTitle = novelTitle;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
