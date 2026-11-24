package com.novel.platform.modules.user.dto;

import java.time.LocalDateTime;

public class ShelfItemResponse {

    private String novelId;
    private String title;
    private String coverUrl;
    private String authorId;
    private String authorName;
    private String lastReadChapterId;
    private String lastReadChapterTitle;
    private LocalDateTime lastReadAt;
    private Boolean isTop;
    private Boolean isNotify;
    private Integer chapterCount;
    private LocalDateTime createdAt;

    public String getNovelId() {
        return novelId;
    }

    public void setNovelId(String novelId) {
        this.novelId = novelId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getLastReadChapterId() {
        return lastReadChapterId;
    }

    public void setLastReadChapterId(String lastReadChapterId) {
        this.lastReadChapterId = lastReadChapterId;
    }

    public String getLastReadChapterTitle() {
        return lastReadChapterTitle;
    }

    public void setLastReadChapterTitle(String lastReadChapterTitle) {
        this.lastReadChapterTitle = lastReadChapterTitle;
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

    public Integer getChapterCount() {
        return chapterCount;
    }

    public void setChapterCount(Integer chapterCount) {
        this.chapterCount = chapterCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
