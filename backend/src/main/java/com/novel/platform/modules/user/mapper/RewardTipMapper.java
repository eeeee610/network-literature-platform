package com.novel.platform.modules.user.mapper;

import com.novel.platform.modules.user.dto.TipItemResponse;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RewardTipMapper {

    @Select("""
            SELECT t.tip_id AS tipId,
                   t.tip_no AS tipNo,
                   t.novel_id AS novelId,
                   n.title AS novelTitle,
                   t.chapter_id AS chapterId,
                   c.title AS chapterTitle,
                   t.to_user_id AS toUserId,
                   u.nickname AS authorName,
                   t.amount AS amount,
                   t.message AS message,
                   t.status AS status,
                   t.created_at AS createdAt
            FROM reward_tip t
            JOIN novel n ON n.novel_id = t.novel_id
            JOIN app_user u ON u.user_id = t.to_user_id
            LEFT JOIN chapter c ON c.chapter_id = t.chapter_id
            WHERE t.from_user_id = #{userId}
            ORDER BY t.created_at DESC, t.tip_id DESC
            LIMIT #{offset}, #{limit}
            """)
    List<TipItemResponse> selectTipPage(@Param("userId") Long userId,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM reward_tip WHERE from_user_id = #{userId}")
    long countTip(@Param("userId") Long userId);
}
