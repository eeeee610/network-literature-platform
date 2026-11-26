package com.novel.platform.modules.novel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("novel_tag")
public class NovelTag {

    @TableId(value = "novel_id", type = IdType.INPUT)
    private Long novelId;

    private Long tagId;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    public NovelTag() {
    }

    public NovelTag(Long novelId, Long tagId, Integer sortOrder) {
        this.novelId = novelId;
        this.tagId = tagId;
        this.sortOrder = sortOrder;
    }

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
