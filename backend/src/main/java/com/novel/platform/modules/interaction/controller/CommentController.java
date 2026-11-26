package com.novel.platform.modules.interaction.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.common.api.PageResult;
import com.novel.platform.common.security.UserContext;
import com.novel.platform.modules.interaction.dto.CommentCreateRequest;
import com.novel.platform.modules.interaction.dto.CommentCreateResponse;
import com.novel.platform.modules.interaction.dto.CommentItem;
import com.novel.platform.modules.interaction.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interaction/comment")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ApiResponse<PageResult<CommentItem>> list(
            @RequestParam String novelId,
            @RequestParam(required = false) String chapterId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(commentService.list(
                novelId, chapterId, page, pageSize, UserContext.currentUserId()));
    }

    @PostMapping
    public ApiResponse<CommentCreateResponse> create(@Valid @RequestBody CommentCreateRequest request) {
        return ApiResponse.success(commentService.create(request, UserContext.currentUserId()));
    }

    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> delete(@PathVariable Long commentId) {
        commentService.delete(commentId, UserContext.currentUserId());
        return ApiResponse.success();
    }

    @PostMapping("/{commentId}/like")
    public ApiResponse<Void> like(@PathVariable Long commentId) {
        commentService.like(commentId, UserContext.currentUserId());
        return ApiResponse.success();
    }

    @DeleteMapping("/{commentId}/like")
    public ApiResponse<Void> unlike(@PathVariable Long commentId) {
        commentService.unlike(commentId, UserContext.currentUserId());
        return ApiResponse.success();
    }
}
