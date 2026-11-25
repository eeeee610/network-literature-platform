package com.novel.platform.modules.interaction.dto;

import java.time.LocalDateTime;

/**
 * 榜单期次查询行。
 */
public class RankingPeriodRow {

    private String periodNo;
    private Long itemCount;
    private LocalDateTime createdAt;

    public String getPeriodNo() {
        return periodNo;
    }

    public void setPeriodNo(String periodNo) {
        this.periodNo = periodNo;
    }

    public Long getItemCount() {
        return itemCount;
    }

    public void setItemCount(Long itemCount) {
        this.itemCount = itemCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
