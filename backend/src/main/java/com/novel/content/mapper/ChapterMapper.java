package com.novel.content.mapper;

import com.novel.content.dto.ChapterListItem;
import com.novel.content.entity.Chapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChapterMapper {

    @Select("SELECT chapter_id, chapter_no, title, is_vip, vip_price, word_count, publish_time "
            + "FROM chapter WHERE novel_id = #{novelId} AND status = 1 AND audit_status = 1 "
            + "AND publish_time IS NOT NULL ORDER BY chapter_no ASC")
    List<ChapterListItem> selectCatalog(@Param("novelId") Long novelId);

    @Select("SELECT COUNT(*) FROM chapter WHERE novel_id = #{novelId} AND status = 1 "
            + "AND audit_status = 1 AND publish_time IS NOT NULL")
    int countPublished(@Param("novelId") Long novelId);

    @Select("SELECT title FROM chapter WHERE novel_id = #{novelId} AND status = 1 "
            + "AND audit_status = 1 AND publish_time IS NOT NULL ORDER BY chapter_no DESC LIMIT 1")
    String selectLastPublishedTitle(@Param("novelId") Long novelId);

    @Select("SELECT c.chapter_id, c.novel_id, c.chapter_no, c.title, c.content, c.is_vip, c.vip_price, "
            + "c.word_count, c.publish_time FROM chapter c JOIN novel n ON n.novel_id = c.novel_id "
            + "WHERE c.chapter_id = #{chapterId} AND c.status = 1 AND c.audit_status = 1 "
            + "AND c.publish_time IS NOT NULL AND n.audit_status = 1 AND n.status IN (0, 1)")
    Chapter selectReadable(@Param("chapterId") Long chapterId);

    @Select("SELECT chapter_id FROM chapter WHERE novel_id = #{novelId} AND chapter_no < #{chapterNo} "
            + "AND status = 1 AND audit_status = 1 AND publish_time IS NOT NULL "
            + "ORDER BY chapter_no DESC LIMIT 1")
    Long selectPrevChapterId(@Param("novelId") Long novelId, @Param("chapterNo") Integer chapterNo);

    @Select("SELECT chapter_id FROM chapter WHERE novel_id = #{novelId} AND chapter_no > #{chapterNo} "
            + "AND status = 1 AND audit_status = 1 AND publish_time IS NOT NULL "
            + "ORDER BY chapter_no ASC LIMIT 1")
    Long selectNextChapterId(@Param("novelId") Long novelId, @Param("chapterNo") Integer chapterNo);

    @Select("SELECT chapter_id, novel_id, chapter_no, word_count, status FROM chapter "
            + "WHERE chapter_id = #{chapterId}")
    Chapter selectMeta(@Param("chapterId") Long chapterId);
}
