package com.novel.platform.modules.user.mapper;

import com.novel.platform.modules.user.dto.SubscriptionItemResponse;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ChapterSubscriptionMapper {

    @Select("""
            SELECT s.chapter_id AS chapterId,
                   c.title AS chapterTitle,
                   c.chapter_no AS chapterNo,
                   s.novel_id AS novelId,
                   n.title AS novelTitle,
                   s.unit_price AS unitPrice,
                   s.paid_amount AS paidAmount,
                   s.status AS status,
                   s.subscribed_at AS subscribedAt
            FROM chapter_subscription s
            JOIN chapter c ON c.chapter_id = s.chapter_id
            JOIN novel n ON n.novel_id = s.novel_id
            WHERE s.user_id = #{userId}
            ORDER BY s.subscribed_at DESC
            LIMIT #{offset}, #{limit}
            """)
    List<SubscriptionItemResponse> selectSubscriptionPage(@Param("userId") Long userId,
                                                          @Param("offset") int offset,
                                                          @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM chapter_subscription WHERE user_id = #{userId}")
    long countSubscription(@Param("userId") Long userId);
}
