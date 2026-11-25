package com.novel.platform.modules.interaction.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论盖楼展示节点：一级评论带直接回复列表，回复节点不再嵌套。
 */
public record CommentItem(
        String commentId,
        String novelId,
        String chapterId,
        String parentId,
        String replyToUserId,
        String replyToNickname,
        String content,
        int likeCount,
        int replyCount,
        boolean top,
        boolean liked,
        LocalDateTime createdAt,
        UserBrief user,
        List<CommentItem> replies
) {
}
