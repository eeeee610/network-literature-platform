package com.novel.platform.modules.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novel.platform.modules.interaction.dto.RankingConfigRow;
import com.novel.platform.modules.interaction.entity.RankingConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RankingConfigMapper extends BaseMapper<RankingConfig> {

    List<RankingConfigRow> selectActiveWithLatest();

    List<RankingConfigRow> selectAllWithLatest();

    RankingConfigRow selectRowByCode(@Param("rankingCode") String rankingCode);
}
