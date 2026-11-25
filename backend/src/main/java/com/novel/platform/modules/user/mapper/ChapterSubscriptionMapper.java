package com.novel.platform.modules.user.mapper;

import com.novel.platform.modules.user.entity.ChapterSubscription;
import com.novel.platform.modules.user.model.Rows.SubscriptionRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 章节订阅使用联合主键 (user_id, chapter_id)，手写 SQL 维护。
 */
public interface ChapterSubscriptionMapper {

    @Select("""
            SELECT user_id, chapter_id, novel_id, unit_price, paid_amount, txn_no, request_no,
                   status, subscribed_at, refunded_at
            FROM chapter_subscription
            WHERE user_id = #{userId} AND chapter_id = #{chapterId}
            """)
    ChapterSubscription selectOne(@Param("userId") Long userId, @Param("chapterId") Long chapterId);

    @Select("""
            SELECT user_id, chapter_id, novel_id, unit_price, paid_amount, txn_no, request_no,
                   status, subscribed_at, refunded_at
            FROM chapter_subscription
            WHERE request_no = #{requestNo}
            """)
    ChapterSubscription selectByRequestNo(@Param("requestNo") String requestNo);

    @Insert("""
            INSERT INTO chapter_subscription
                (user_id, chapter_id, novel_id, unit_price, paid_amount, txn_no, request_no, status, subscribed_at)
            VALUES (#{userId}, #{chapterId}, #{novelId}, #{unitPrice}, #{paidAmount}, #{txnNo}, #{requestNo},
                    #{status}, #{subscribedAt})
            """)
    int insertOne(ChapterSubscription subscription);

    @Select("SELECT COUNT(*) FROM chapter_subscription WHERE user_id = #{userId}")
    long countByUser(@Param("userId") Long userId);

    @Select("""
            SELECT s.chapter_id    AS chapterId,
                   s.novel_id      AS novelId,
                   n.title         AS novelTitle,
                   c.title         AS chapterTitle,
                   c.chapter_no    AS chapterNo,
                   s.unit_price    AS unitPrice,
                   s.paid_amount   AS paidAmount,
                   s.status        AS status,
                   s.subscribed_at AS subscribedAt
            FROM chapter_subscription s
                     JOIN novel n ON n.novel_id = s.novel_id
                     JOIN chapter c ON c.chapter_id = s.chapter_id
            WHERE s.user_id = #{userId}
            ORDER BY s.subscribed_at DESC
            LIMIT #{offset}, #{pageSize}
            """)
    List<SubscriptionRow> selectPage(@Param("userId") Long userId,
                                     @Param("offset") int offset,
                                     @Param("pageSize") int pageSize);
}
