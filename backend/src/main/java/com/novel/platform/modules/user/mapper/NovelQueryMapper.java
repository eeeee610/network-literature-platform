package com.novel.platform.modules.user.mapper;

import com.novel.platform.modules.user.model.Rows.ChapterBrief;
import com.novel.platform.modules.user.model.Rows.NovelBrief;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 对 B 模块冻结表 novel、chapter 的只读查询，用于订阅价格校验与打赏对象校验。
 * 本 Mapper 只允许 SELECT，禁止写入 B 模块表。
 */
public interface NovelQueryMapper {

    @Select("""
            SELECT novel_id AS novelId, author_id AS authorId, title, cover_url AS coverUrl,
                   status, audit_status AS auditStatus
            FROM novel
            WHERE novel_id = #{novelId}
            """)
    NovelBrief selectNovel(@Param("novelId") Long novelId);

    @Select("""
            SELECT c.chapter_id        AS chapterId,
                   c.novel_id          AS novelId,
                   c.chapter_no        AS chapterNo,
                   c.title             AS title,
                   c.is_vip            AS isVip,
                   c.vip_price         AS vipPrice,
                   c.status            AS status,
                   c.audit_status      AS auditStatus,
                   c.publish_time      AS publishTime,
                   n.title             AS novelTitle,
                   n.status            AS novelStatus,
                   n.audit_status      AS novelAuditStatus,
                   n.author_id         AS authorId
            FROM chapter c
                     JOIN novel n ON n.novel_id = c.novel_id
            WHERE c.chapter_id = #{chapterId}
            """)
    ChapterBrief selectChapter(@Param("chapterId") Long chapterId);

    @Select("SELECT nickname FROM app_user WHERE user_id = #{userId}")
    String selectNickname(@Param("userId") Long userId);
}
