package com.novel.platform.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novel.platform.modules.user.entity.RewardTip;
import com.novel.platform.modules.user.model.Rows.RewardRow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RewardTipMapper extends BaseMapper<RewardTip> {

    @Select("SELECT COUNT(*) FROM reward_tip WHERE from_user_id = #{userId}")
    long countSent(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM reward_tip WHERE to_user_id = #{userId}")
    long countReceived(@Param("userId") Long userId);

    @Select("""
            SELECT t.tip_id       AS tipId,
                   t.novel_id     AS novelId,
                   n.title        AS novelTitle,
                   t.chapter_id   AS chapterId,
                   c.title        AS chapterTitle,
                   t.from_user_id AS fromUserId,
                   fu.nickname    AS fromNickname,
                   t.to_user_id   AS toUserId,
                   tu.nickname    AS toNickname,
                   t.amount       AS amount,
                   t.message      AS message,
                   t.status       AS status,
                   t.created_at   AS createdAt
            FROM reward_tip t
                     JOIN novel n ON n.novel_id = t.novel_id
                     LEFT JOIN chapter c ON c.chapter_id = t.chapter_id
                     JOIN app_user fu ON fu.user_id = t.from_user_id
                     JOIN app_user tu ON tu.user_id = t.to_user_id
            WHERE t.from_user_id = #{userId}
            ORDER BY t.created_at DESC
            LIMIT #{offset}, #{pageSize}
            """)
    List<RewardRow> selectSent(@Param("userId") Long userId,
                               @Param("offset") int offset,
                               @Param("pageSize") int pageSize);

    @Select("""
            SELECT t.tip_id       AS tipId,
                   t.novel_id     AS novelId,
                   n.title        AS novelTitle,
                   t.chapter_id   AS chapterId,
                   c.title        AS chapterTitle,
                   t.from_user_id AS fromUserId,
                   fu.nickname    AS fromNickname,
                   t.to_user_id   AS toUserId,
                   tu.nickname    AS toNickname,
                   t.amount       AS amount,
                   t.message      AS message,
                   t.status       AS status,
                   t.created_at   AS createdAt
            FROM reward_tip t
                     JOIN novel n ON n.novel_id = t.novel_id
                     LEFT JOIN chapter c ON c.chapter_id = t.chapter_id
                     JOIN app_user fu ON fu.user_id = t.from_user_id
                     JOIN app_user tu ON tu.user_id = t.to_user_id
            WHERE t.to_user_id = #{userId}
            ORDER BY t.created_at DESC
            LIMIT #{offset}, #{pageSize}
            """)
    List<RewardRow> selectReceived(@Param("userId") Long userId,
                                   @Param("offset") int offset,
                                   @Param("pageSize") int pageSize);
}
