package com.novel.platform.modules.novel.dto;

/**
 * sp_create_novel 调用参数，novelId 为 OUT 参数回填值。
 */
public class CreateNovelParam {

    private Long authorId;
    private String title;
    private String coverUrl;
    private String description;
    private String tagIdsJson;
    private Long novelId;

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

    public String getTagIdsJson() {
        return tagIdsJson;
    }

    public void setTagIdsJson(String tagIdsJson) {
        this.tagIdsJson = tagIdsJson;
    }

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }
}
