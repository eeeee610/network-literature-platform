package com.novel.platform.modules.novel.dto;

import java.math.BigDecimal;

/**
 * 作者章节详情（含正文），供创作台改稿回显，不受章节发布状态限制。
 */
public record AuthorChapterDetailVO(
        String chapterId,
        String novelId,
        Integer chapterNo,
        String title,
        String content,
        Integer isVip,
        BigDecimal vipPrice,
        Integer wordCount,
        Integer status,
        Integer auditStatus,
        String publishTime,
        String updatedAt
) {
}
