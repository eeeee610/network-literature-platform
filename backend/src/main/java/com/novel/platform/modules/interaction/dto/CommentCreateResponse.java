package com.novel.platform.modules.interaction.dto;

/**
 * 新增评论结果。评论统一初始为待审核。
 */
public record CommentCreateResponse(String commentId, int auditStatus) {
}
