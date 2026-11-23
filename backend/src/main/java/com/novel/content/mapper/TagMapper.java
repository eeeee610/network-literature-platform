package com.novel.content.mapper;

import com.novel.content.dto.NovelDetailVO;
import com.novel.content.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface TagMapper {

    @Select("SELECT tag_id, tag_name, is_active, created_at FROM tag WHERE is_active = 1 ORDER BY tag_id ASC")
    List<Tag> selectActive();

    @Select("SELECT t.tag_id, t.tag_name, nt.sort_order FROM novel_tag nt "
            + "JOIN tag t ON t.tag_id = nt.tag_id WHERE nt.novel_id = #{novelId} "
            + "ORDER BY nt.sort_order ASC, t.tag_id ASC")
    List<NovelDetailVO.TagItem> selectNovelTags(@Param("novelId") Long novelId);

    @Select("<script>"
            + "SELECT nt.novel_id AS novelId, t.tag_name AS tagName FROM novel_tag nt "
            + "JOIN tag t ON t.tag_id = nt.tag_id WHERE nt.novel_id IN "
            + "<foreach collection='novelIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "ORDER BY nt.novel_id ASC, nt.sort_order ASC, t.tag_id ASC"
            + "</script>")
    List<Map<String, Object>> selectTagNames(@Param("novelIds") List<Long> novelIds);
}
