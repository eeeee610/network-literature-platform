package com.novel.platform.modules.interaction.dto;

import java.time.LocalDateTime;

/**
 * 榜单配置查询行：配置信息 + 最新期次与条目数。
 */
public class RankingConfigRow {

    private Long rankingId;
    private String rankingCode;
    private String rankingName;
    private String rankingType;
    private String periodType;
    private Integer topN;
    private String weightConfig;
    private Integer isActive;
    private String latestPeriodNo;
    private Long latestItemCount;
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

    public String getLatestPeriodNo() {
        return latestPeriodNo;
    }

    public void setLatestPeriodNo(String latestPeriodNo) {
        this.latestPeriodNo = latestPeriodNo;
    }

    public Long getLatestItemCount() {
        return latestItemCount;
    }

    public void setLatestItemCount(Long latestItemCount) {
        this.latestItemCount = latestItemCount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
