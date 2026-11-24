package com.novel.platform.modules.novel.dto;

import java.time.LocalDateTime;

/**
 * novel 表查询行：列表、详情、作者视角与写操作前校验共用。
 * 列表 SQL 只填充部分字段，未查询字段为 null。
 */
public class NovelRow {

    private Long novelId;
    private Long authorId;
    private String title;
    private String coverUrl;
    private String description;
    private Integer status;
    private Integer auditStatus;
    private Integer totalWords;
    private Long latestChapterId;
    private Integer latestChapterNo;
    private String latestChapterTitle;
    private LocalDateTime latestChapterPublishTime;
    private Integer chapterCount;
    private String authorName;
    private String authorAvatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(Integer auditStatus) {
        this.auditStatus = auditStatus;
    }

    public Integer getTotalWords() {
        return totalWords;
    }

    public void setTotalWords(Integer totalWords) {
        this.totalWords = totalWords;
    }

    public Long getLatestChapterId() {
        return latestChapterId;
    }

    public void setLatestChapterId(Long latestChapterId) {
        this.latestChapterId = latestChapterId;
    }

    public Integer getLatestChapterNo() {
        return latestChapterNo;
    }

    public void setLatestChapterNo(Integer latestChapterNo) {
        this.latestChapterNo = latestChapterNo;
    }

    public String getLatestChapterTitle() {
        return latestChapterTitle;
    }

    public void setLatestChapterTitle(String latestChapterTitle) {
        this.latestChapterTitle = latestChapterTitle;
    }

    public LocalDateTime getLatestChapterPublishTime() {
        return latestChapterPublishTime;
    }

    public void setLatestChapterPublishTime(LocalDateTime latestChapterPublishTime) {
        this.latestChapterPublishTime = latestChapterPublishTime;
    }

    public Integer getChapterCount() {
        return chapterCount;
    }

    public void setChapterCount(Integer chapterCount) {
        this.chapterCount = chapterCount;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public void setAuthorAvatarUrl(String authorAvatarUrl) {
        this.authorAvatarUrl = authorAvatarUrl;
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
