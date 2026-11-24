package com.novel.platform.modules.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增评论/回复。ID 以字符串传递，空串按 null 处理。
 */
public record CommentCreateRequest(
        @NotBlank(message = "novelId 不能为空")
        String novelId,

        String chapterId,

        String parentId,

        @NotBlank(message = "评论内容不能为空")
        @Size(max = 1000, message = "评论内容不能超过 1000 字")
        String content
) {
}
