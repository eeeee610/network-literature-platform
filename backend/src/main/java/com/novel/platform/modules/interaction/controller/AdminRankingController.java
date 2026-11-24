package com.novel.platform.modules.interaction.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.modules.interaction.dto.RankingConfigCreateRequest;
import com.novel.platform.modules.interaction.dto.RankingConfigResponse;
import com.novel.platform.modules.interaction.dto.RankingConfigUpdateRequest;
import com.novel.platform.modules.interaction.dto.RankingGenerateRequest;
import com.novel.platform.modules.interaction.dto.RankingGenerateResponse;
import com.novel.platform.modules.interaction.service.RankingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台榜单配置与期次生成。URL 使用 /api/admin 前缀，业务归属 C 模块。
 */
@RestController
@RequestMapping("/api/admin/ranking")
public class AdminRankingController {

    private final RankingService rankingService;

    public AdminRankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping
    public ApiResponse<List<RankingConfigResponse>> list() {
        return ApiResponse.success(rankingService.listAll());
    }

    @PostMapping
    public ApiResponse<RankingConfigResponse> create(
            @Valid @RequestBody RankingConfigCreateRequest request) {
        return ApiResponse.success(rankingService.create(request));
    }

    @PutMapping("/{rankingId}")
    public ApiResponse<RankingConfigResponse> update(
            @PathVariable Long rankingId,
            @Valid @RequestBody RankingConfigUpdateRequest request) {
        return ApiResponse.success(rankingService.update(rankingId, request));
    }

    @PostMapping("/{rankingId}/generate")
    public ApiResponse<RankingGenerateResponse> generate(
            @PathVariable Long rankingId,
            @Valid @RequestBody RankingGenerateRequest request) {
        return ApiResponse.success(rankingService.generate(rankingId, request));
    }
}
