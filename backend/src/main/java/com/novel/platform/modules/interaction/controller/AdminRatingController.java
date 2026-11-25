package com.novel.platform.modules.interaction.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.common.api.PageResult;
import com.novel.platform.common.security.UserContext;
import com.novel.platform.modules.interaction.dto.RatingAuditRequest;
import com.novel.platform.modules.interaction.dto.RatingReviewItem;
import com.novel.platform.modules.interaction.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台评分短评审核。URL 使用 /api/admin 前缀，业务归属 C 模块。
 */
@RestController
@RequestMapping("/api/admin/rating")
public class AdminRatingController {

    private final RatingService ratingService;

    public AdminRatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @GetMapping("/review")
    public ApiResponse<PageResult<RatingReviewItem>> list(
            @RequestParam(required = false) Integer reviewAuditStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(ratingService.adminReviews(reviewAuditStatus, page, pageSize));
    }

    @PostMapping("/{ratingId}/audit")
    public ApiResponse<Void> audit(@PathVariable Long ratingId,
                                   @Valid @RequestBody RatingAuditRequest request) {
        ratingService.auditReview(ratingId, request, UserContext.currentUserId());
        return ApiResponse.success();
    }
}
