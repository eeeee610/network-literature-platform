package com.novel.platform.modules.interaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 小说评分表 novel_rating。评分统计由数据库触发器维护。
 */
@TableName("novel_rating")
public class NovelRating {

    @TableId(value = "rating_id", type = IdType.AUTO)
    private Long ratingId;

    private Long userId;

    private Long novelId;

    private Integer score;

    private String reviewContent;

    private Integer status;

    private Integer reviewAuditStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
