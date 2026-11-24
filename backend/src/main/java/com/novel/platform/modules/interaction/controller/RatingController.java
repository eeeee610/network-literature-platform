package com.novel.platform.modules.interaction.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.common.api.PageResult;
import com.novel.platform.common.security.UserContext;
import com.novel.platform.modules.interaction.dto.RatingReviewItem;
import com.novel.platform.modules.interaction.dto.RatingSummaryResponse;
import com.novel.platform.modules.interaction.dto.RatingUpsertRequest;
import com.novel.platform.modules.interaction.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interaction/rating")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @GetMapping("/summary/{novelId}")
    public ApiResponse<RatingSummaryResponse> summary(@PathVariable String novelId) {
        return ApiResponse.success(ratingService.summary(novelId, UserContext.currentUserId()));
    }

    @GetMapping("/review")
    public ApiResponse<PageResult<RatingReviewItem>> reviews(
            @RequestParam String novelId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(ratingService.publicReviews(novelId, page, pageSize));
    }

    @PostMapping
    public ApiResponse<RatingSummaryResponse> save(@Valid @RequestBody RatingUpsertRequest request) {
        return ApiResponse.success(ratingService.save(request, UserContext.currentUserId()));
    }
}
