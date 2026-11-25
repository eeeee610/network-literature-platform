package com.novel.platform.modules.user.mapper;

import com.novel.platform.modules.user.dto.ShelfItemResponse;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface UserBookshelfMapper {

    @Select("""
            SELECT ub.novel_id AS novelId,
                   n.title AS title,
                   n.cover_url AS coverUrl,
                   n.author_id AS authorId,
                   u.nickname AS authorName,
                   ub.last_read_chapter_id AS lastReadChapterId,
                   c.title AS lastReadChapterTitle,
                   ub.last_read_at AS lastReadAt,
                   ub.is_top AS isTop,
                   ub.is_notify AS isNotify,
                   ub.created_at AS createdAt,
                   (SELECT COUNT(*) FROM chapter ch
                     WHERE ch.novel_id = n.novel_id
                       AND ch.status = 1
                       AND ch.audit_status = 1) AS chapterCount
            FROM user_bookshelf ub
            JOIN novel n ON n.novel_id = ub.novel_id
            JOIN app_user u ON u.user_id = n.author_id
            LEFT JOIN chapter c ON c.chapter_id = ub.last_read_chapter_id
            WHERE ub.user_id = #{userId}
            ORDER BY ub.is_top DESC, ub.updated_at DESC
            LIMIT #{offset}, #{limit}
            """)
    List<ShelfItemResponse> selectShelfPage(@Param("userId") Long userId,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM user_bookshelf WHERE user_id = #{userId}")
    long countShelf(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO user_bookshelf (user_id, novel_id, is_top, is_notify)
            VALUES (#{userId}, #{novelId}, 0, 1)
            """)
    int insertShelf(@Param("userId") Long userId, @Param("novelId") Long novelId);

    @Update("""
            UPDATE user_bookshelf
               SET is_top = COALESCE(#{isTop}, is_top),
                   is_notify = COALESCE(#{isNotify}, is_notify),
                   updated_at = CURRENT_TIMESTAMP(6)
             WHERE user_id = #{userId} AND novel_id = #{novelId}
            """)
    int updateFlags(@Param("userId") Long userId, @Param("novelId") Long novelId,
                    @Param("isTop") Boolean isTop, @Param("isNotify") Boolean isNotify);

    @Delete("DELETE FROM user_bookshelf WHERE user_id = #{userId} AND novel_id = #{novelId}")
    int deleteShelf(@Param("userId") Long userId, @Param("novelId") Long novelId);
}
