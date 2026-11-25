package com.novel.platform.modules.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novel.platform.modules.admin.dto.AdminTagRow;
import com.novel.platform.modules.novel.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AdminTagMapper extends BaseMapper<Tag> {

    List<AdminTagRow> selectTags(@Param("tagName") String tagName,
                                 @Param("isActive") Integer isActive,
                                 @Param("offset") int offset,
                                 @Param("size") int size);

    long countTags(@Param("tagName") String tagName, @Param("isActive") Integer isActive);

    @Select("SELECT COUNT(*) FROM novel_tag WHERE tag_id = #{tagId}")
    long countNovelReferences(@Param("tagId") Long tagId);
}
