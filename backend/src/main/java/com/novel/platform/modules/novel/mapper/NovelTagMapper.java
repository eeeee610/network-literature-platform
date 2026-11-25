package com.novel.platform.modules.novel.mapper;

import com.novel.platform.modules.novel.dto.TagRow;
import com.novel.platform.modules.novel.entity.NovelTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NovelTagMapper {

    List<TagRow> selectByNovelIds(@Param("novelIds") List<Long> novelIds);

    List<TagRow> selectDetailByNovelId(@Param("novelId") long novelId);

    int deleteByNovelId(@Param("novelId") long novelId);

    int insertBatch(@Param("items") List<NovelTag> items);
}
