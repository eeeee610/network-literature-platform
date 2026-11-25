package com.novel.platform.modules.interaction.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 评论点赞。点赞数与取消点赞后的计数由数据库触发器维护。
 */
@Mapper
public interface CommentLikeMapper {

    @Insert("INSERT IGNORE INTO comment_like (comment_id, user_id) VALUES (#{commentId}, #{userId})")
    int insertIgnore(@Param("commentId") Long commentId, @Param("userId") Long userId);

    @Delete("DELETE FROM comment_like WHERE comment_id = #{commentId} AND user_id = #{userId}")
    int deleteLike(@Param("commentId") Long commentId, @Param("userId") Long userId);
}
