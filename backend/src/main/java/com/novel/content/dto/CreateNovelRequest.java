package com.novel.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreateNovelRequest {

    @NotBlank(message = "书名不能为空")
    @Size(max = 120, message = "书名长度不能超过 120")
    private String title;

    @Size(max = 255, message = "封面地址长度不能超过 255")
    private String coverUrl;

    @Size(max = 1000, message = "简介长度不能超过 1000")
    private String description;

    @Size(max = 10, message = "标签数量不能超过 10 个")
    private List<Long> tagIds;

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

    public List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds;
    }
}
