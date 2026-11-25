package com.novel.platform.modules.interaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 榜单配置表 ranking_config。
 */
@TableName("ranking_config")
public class RankingConfig {

    @TableId(value = "ranking_id", type = IdType.AUTO)
    private Long rankingId;

    private String rankingCode;

    private String rankingName;

    private String rankingType;

    private String periodType;

    private Integer topN;

    /** 数据库 JSON 字段，应用层按 JSON 文本读写。 */
    private String weightConfig;

    private Integer isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getRankingId() {
        return rankingId;
    }

    public void setRankingId(Long rankingId) {
        this.rankingId = rankingId;
    }

    public String getRankingCode() {
        return rankingCode;
    }

    public void setRankingCode(String rankingCode) {
        this.rankingCode = rankingCode;
    }

    public String getRankingName() {
        return rankingName;
    }

    public void setRankingName(String rankingName) {
        this.rankingName = rankingName;
    }

    public String getRankingType() {
        return rankingType;
    }

    public void setRankingType(String rankingType) {
        this.rankingType = rankingType;
    }

    public String getPeriodType() {
        return periodType;
    }

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public Integer getTopN() {
        return topN;
    }

    public void setTopN(Integer topN) {
        this.topN = topN;
    }

    public String getWeightConfig() {
        return weightConfig;
    }

    public void setWeightConfig(String weightConfig) {
        this.weightConfig = weightConfig;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
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
