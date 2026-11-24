package com.novel.platform.modules.user.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 只读 B 模块的 novel/chapter，用于 A 的书架、订阅、打赏展示与作者校验。 */
public interface CatalogQueryMapper {

    @Select("SELECT COUNT(*) FROM novel WHERE novel_id = #{novelId}")
    int countNovel(@Param("novelId") Long novelId);

    @Select("SELECT author_id FROM novel WHERE novel_id = #{novelId}")
    Long findNovelAuthor(@Param("novelId") Long novelId);

    @Select("""
            SELECT n.author_id
            FROM chapter c
            JOIN novel n ON n.novel_id = c.novel_id
            WHERE c.chapter_id = #{chapterId}
              AND c.novel_id = #{novelId}
            """)
    Long findChapterAuthor(@Param("chapterId") Long chapterId, @Param("novelId") Long novelId);

    @Select("SELECT COUNT(*) FROM chapter WHERE chapter_id = #{chapterId}")
    int countChapter(@Param("chapterId") Long chapterId);
}
