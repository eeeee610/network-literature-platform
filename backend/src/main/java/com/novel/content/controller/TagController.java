package com.novel.content.controller;

import com.novel.content.common.ApiResponse;
import com.novel.content.entity.Tag;
import com.novel.content.service.NovelQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class TagController {

    private final NovelQueryService novelQueryService;

    public TagController(NovelQueryService novelQueryService) {
        this.novelQueryService = novelQueryService;
    }

    @GetMapping("/tags")
    public ApiResponse<List<Tag>> tags() {
        return ApiResponse.success(novelQueryService.tags());
    }
}
