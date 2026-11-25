package com.novel.platform.modules.interaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 榜单条目查询行（来自 sp_get_ranking 结果集）。
 */
public class RankingDetailRow {

    private Integer rankNo;
    private Long novelId;
    private String novelTitle;
    private BigDecimal score;
    private String metricSnapshot;
    private String periodNo;
    private LocalDateTime createdAt;

    public Integer getRankNo() {
        return rankNo;
    }

    public void setRankNo(Integer rankNo) {
        this.rankNo = rankNo;
    }

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }

    public String getNovelTitle() {
        return novelTitle;
    }

    public void setNovelTitle(String novelTitle) {
        this.novelTitle = novelTitle;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getMetricSnapshot() {
        return metricSnapshot;
    }

    public void setMetricSnapshot(String metricSnapshot) {
        this.metricSnapshot = metricSnapshot;
    }

    public String getPeriodNo() {
        return periodNo;
    }

    public void setPeriodNo(String periodNo) {
        this.periodNo = periodNo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
