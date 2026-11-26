package com.novel.platform.modules.novel.dto;

import java.math.BigDecimal;

/**
 * sp_save_chapter_draft 调用参数；chapterId 为 null 表示新建，chapterIdOut 为 OUT 参数回填值。
 */
public class SaveChapterDraftParam {

    private Long chapterId;
    private Long novelId;
    private String title;
    private String content;
    private Integer isVip;
    private BigDecimal vipPrice;
    private Long chapterIdOut;

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getIsVip() {
        return isVip;
    }

    public void setIsVip(Integer isVip) {
        this.isVip = isVip;
    }

    public BigDecimal getVipPrice() {
        return vipPrice;
    }

    public void setVipPrice(BigDecimal vipPrice) {
        this.vipPrice = vipPrice;
    }

    public Long getChapterIdOut() {
        return chapterIdOut;
    }

    public void setChapterIdOut(Long chapterIdOut) {
        this.chapterIdOut = chapterIdOut;
    }
}
