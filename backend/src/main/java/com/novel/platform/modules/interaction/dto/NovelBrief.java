package com.novel.platform.modules.interaction.dto;

/**
 * 作品最小信息，用于校验作品状态与展示书名。
 */
public class NovelBrief {

    private Long novelId;
    private String title;
    private Integer status;

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
