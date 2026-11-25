package com.novel.platform.modules.user.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.common.api.PageResult;
import com.novel.platform.modules.user.model.Requests;
import com.novel.platform.modules.user.model.Views;
import com.novel.platform.modules.user.service.RewardService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/reward")
public class RewardController {

    private final RewardService rewardService;

    public RewardController(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    @GetMapping("/novel/{novelId}")
    public ApiResponse<Views.RewardTarget> target(@PathVariable String novelId) {
        return ApiResponse.success(rewardService.target(novelId));
    }

    @PostMapping
    public ApiResponse<Views.RewardResult> reward(@Valid @RequestBody Requests.Reward request) {
        return ApiResponse.success(rewardService.reward(request));
    }

    @GetMapping("/sent")
    public ApiResponse<PageResult<Views.RewardItem>> sent(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(rewardService.sent(page, pageSize));
    }

    @GetMapping("/received")
    public ApiResponse<PageResult<Views.RewardItem>> received(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(rewardService.received(page, pageSize));
    }
}
