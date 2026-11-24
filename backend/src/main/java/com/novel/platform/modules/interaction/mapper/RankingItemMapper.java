package com.novel.platform.modules.interaction.mapper;

import com.novel.platform.modules.interaction.dto.RankingPeriodRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RankingItemMapper {

    List<RankingPeriodRow> selectPeriods(@Param("rankingCode") String rankingCode,
                                         @Param("limit") int limit);
}
