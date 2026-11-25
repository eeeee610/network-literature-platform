package com.novel.platform.modules.admin.dto;

import java.time.LocalDateTime;

public class AdminTagRow {

    private Long tagId;
    private String tagName;
    private Integer isActive;
    private long novelCount;
    private LocalDateTime createdAt;

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public long getNovelCount() {
        return novelCount;
    }

    public void setNovelCount(long novelCount) {
        this.novelCount = novelCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
