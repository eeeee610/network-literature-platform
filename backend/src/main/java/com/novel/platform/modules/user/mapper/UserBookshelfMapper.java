package com.novel.platform.modules.user.mapper;

import com.novel.platform.modules.user.entity.UserBookshelf;
import com.novel.platform.modules.user.model.Rows.BookshelfRow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 书架使用联合主键 (user_id, novel_id)，MyBatis-Plus 无法直接映射复合主键，故手写 SQL。
 */
public interface UserBookshelfMapper {

    @Select("SELECT COUNT(*) FROM user_bookshelf WHERE user_id = #{userId}")
    long countByUser(@Param("userId") Long userId);

    @Select("""
            SELECT b.novel_id              AS novelId,
                   n.title                 AS title,
                   n.cover_url             AS coverUrl,
                   n.status                AS status,
                   n.total_words           AS totalWords,
                   n.latest_chapter_id     AS latestChapterId,
                   lc.title                AS latestChapterTitle,
                   b.last_read_chapter_id  AS lastReadChapterId,
                   rc.title                AS lastReadTitle,
                   b.last_read_at          AS lastReadAt,
                   b.is_top                AS isTop,
                   b.is_notify             AS isNotify,
                   b.updated_at            AS updatedAt
            FROM user_bookshelf b
                     JOIN novel n ON n.novel_id = b.novel_id
                     LEFT JOIN chapter lc ON lc.chapter_id = n.latest_chapter_id
                     LEFT JOIN chapter rc ON rc.chapter_id = b.last_read_chapter_id
            WHERE b.user_id = #{userId}
            ORDER BY b.is_top DESC, b.updated_at DESC
            LIMIT #{offset}, #{pageSize}
            """)
    List<BookshelfRow> selectPage(@Param("userId") Long userId,
                                  @Param("offset") int offset,
                                  @Param("pageSize") int pageSize);

    @Select("""
            SELECT user_id, novel_id, last_read_chapter_id, last_read_at, is_top, is_notify, created_at, updated_at
            FROM user_bookshelf
            WHERE user_id = #{userId} AND novel_id = #{novelId}
            """)
    UserBookshelf selectOne(@Param("userId") Long userId, @Param("novelId") Long novelId);

    @Insert("INSERT INTO user_bookshelf (user_id, novel_id, is_top, is_notify) VALUES (#{userId}, #{novelId}, 0, 1)")
    int insertOne(@Param("userId") Long userId, @Param("novelId") Long novelId);

    @Update("""
            UPDATE user_bookshelf
            SET is_top    = COALESCE(#{isTop,jdbcType=BOOLEAN}, is_top),
                is_notify = COALESCE(#{isNotify,jdbcType=BOOLEAN}, is_notify)
            WHERE user_id = #{userId} AND novel_id = #{novelId}
            """)
    int updateFlags(@Param("userId") Long userId, @Param("novelId") Long novelId,
                    @Param("isTop") Boolean isTop, @Param("isNotify") Boolean isNotify);

    @Update("""
            UPDATE user_bookshelf
            SET last_read_chapter_id = #{chapterId}, last_read_at = NOW(6)
            WHERE user_id = #{userId} AND novel_id = #{novelId}
            """)
    int updateReading(@Param("userId") Long userId, @Param("novelId") Long novelId,
                      @Param("chapterId") Long chapterId);

    @Delete("DELETE FROM user_bookshelf WHERE user_id = #{userId} AND novel_id = #{novelId}")
    int deleteOne(@Param("userId") Long userId, @Param("novelId") Long novelId);
}
