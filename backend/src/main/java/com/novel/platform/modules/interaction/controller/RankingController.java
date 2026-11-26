package com.novel.platform.modules.interaction.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.modules.interaction.dto.RankingConfigResponse;
import com.novel.platform.modules.interaction.dto.RankingDetailResponse;
import com.novel.platform.modules.interaction.dto.RankingPeriodItem;
import com.novel.platform.modules.interaction.service.RankingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/interaction/ranking")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping
    public ApiResponse<List<RankingConfigResponse>> list() {
        return ApiResponse.success(rankingService.listActive());
    }

    @GetMapping("/{rankingCode}")
    public ApiResponse<RankingDetailResponse> detail(
            @PathVariable String rankingCode,
            @RequestParam(required = false) String periodNo,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(rankingService.detail(rankingCode, periodNo, limit));
    }

    @GetMapping("/{rankingCode}/period")
    public ApiResponse<List<RankingPeriodItem>> periods(
            @PathVariable String rankingCode,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(rankingService.periods(rankingCode, limit));
    }
}
