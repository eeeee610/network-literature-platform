package com.novel.platform.modules.interaction.dto;

import java.time.LocalDateTime;

/**
 * 后台评论列表项，包含审核与状态字段。
 */
public record AdminCommentItem(
        String commentId,
        String novelId,
        String novelTitle,
        String chapterId,
        String userId,
        String nickname,
        String parentId,
        String content,
        int likeCount,
        int replyCount,
        int status,
        int auditStatus,
        LocalDateTime createdAt
) {
}
