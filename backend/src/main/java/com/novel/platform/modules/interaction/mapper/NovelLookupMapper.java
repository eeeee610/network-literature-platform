package com.novel.platform.modules.interaction.mapper;

import com.novel.platform.modules.interaction.dto.NovelBrief;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 只读查询 B 模块 novel 表，用于校验作品状态与展示书名。
 */
@Mapper
public interface NovelLookupMapper {

    @Select("SELECT novel_id AS novelId, title, status FROM novel WHERE novel_id = #{novelId}")
    NovelBrief selectBrief(@Param("novelId") Long novelId);
}
