package com.novel.platform.modules.user.entity;

import java.time.LocalDateTime;

/**
 * 书架表使用联合主键 (user_id, novel_id)，由 UserBookshelfMapper 手写 SQL 维护。
 */
public class UserBookshelf {

    private Long userId;
    private Long novelId;
    private Long lastReadChapterId;
    private LocalDateTime lastReadAt;
    private Boolean isTop;
    private Boolean isNotify;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public Long getLastReadChapterId() {
        return lastReadChapterId;
    }

    public void setLastReadChapterId(Long lastReadChapterId) {
        this.lastReadChapterId = lastReadChapterId;
    }

    public LocalDateTime getLastReadAt() {
        return lastReadAt;
    }

    public void setLastReadAt(LocalDateTime lastReadAt) {
        this.lastReadAt = lastReadAt;
    }

    public Boolean getIsTop() {
        return isTop;
    }

    public void setIsTop(Boolean isTop) {
        this.isTop = isTop;
    }

    public Boolean getIsNotify() {
        return isNotify;
    }

    public void setIsNotify(Boolean isNotify) {
        this.isNotify = isNotify;
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
