package com.novel.platform.modules.interaction.mapper.param;

/**
 * sp_generate_ranking 调用参数，itemCount 为 OUT 参数。
 */
public class RankingGenerateParam {

    private Long rankingId;
    private String periodNo;
    private Integer allowHistoryOverwrite;
    private Integer itemCount;

    public Long getRankingId() {
        return rankingId;
    }

    public void setRankingId(Long rankingId) {
        this.rankingId = rankingId;
    }

    public String getPeriodNo() {
        return periodNo;
    }

    public void setPeriodNo(String periodNo) {
        this.periodNo = periodNo;
    }

    public Integer getAllowHistoryOverwrite() {
        return allowHistoryOverwrite;
    }

    public void setAllowHistoryOverwrite(Integer allowHistoryOverwrite) {
        this.allowHistoryOverwrite = allowHistoryOverwrite;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }
}
