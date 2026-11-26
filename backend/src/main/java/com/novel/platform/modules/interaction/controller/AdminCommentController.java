package com.novel.platform.modules.interaction.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.common.api.PageResult;
import com.novel.platform.common.security.UserContext;
import com.novel.platform.modules.interaction.dto.AdminCommentItem;
import com.novel.platform.modules.interaction.dto.CommentAuditRequest;
import com.novel.platform.modules.interaction.dto.CommentStatusRequest;
import com.novel.platform.modules.interaction.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台评论审核与状态管理。URL 使用 /api/admin 前缀，业务归属 C 模块。
 */
@RestController
@RequestMapping("/api/admin/comment")
public class AdminCommentController {

    private final CommentService commentService;

    public AdminCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminCommentItem>> list(
            @RequestParam(required = false) String novelId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(commentService.adminList(
                novelId, status, auditStatus, keyword, page, pageSize));
    }

    @PostMapping("/{commentId}/audit")
    public ApiResponse<Void> audit(@PathVariable Long commentId,
                                   @Valid @RequestBody CommentAuditRequest request) {
        commentService.audit(commentId, request, UserContext.currentUserId());
        return ApiResponse.success();
    }

    @PostMapping("/{commentId}/status")
    public ApiResponse<Void> changeStatus(@PathVariable Long commentId,
                                          @Valid @RequestBody CommentStatusRequest request) {
        commentService.changeStatus(commentId, request);
        return ApiResponse.success();
    }
}
