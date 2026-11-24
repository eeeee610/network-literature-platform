package com.novel.platform.modules.novel.mapper;

import com.novel.platform.modules.novel.dto.TagRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TagMapper {

    List<TagRow> selectActiveTags();

    long countActiveTagsByIds(@Param("ids") List<Long> ids);
}
