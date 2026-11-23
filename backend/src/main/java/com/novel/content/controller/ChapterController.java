package com.novel.content.controller;

import com.novel.content.auth.UserContext;
import com.novel.content.common.ApiResponse;
import com.novel.content.dto.ChapterVO;
import com.novel.content.dto.SaveChapterRequest;
import com.novel.content.service.AuthorWriteService;
import com.novel.content.service.NovelQueryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/chapters")
public class ChapterController {

    private final NovelQueryService novelQueryService;
    private final AuthorWriteService authorWriteService;

    public ChapterController(NovelQueryService novelQueryService, AuthorWriteService authorWriteService) {
        this.novelQueryService = novelQueryService;
        this.authorWriteService = authorWriteService;
    }

    @GetMapping("/{chapterId}")
    public ApiResponse<ChapterVO> read(@PathVariable Long chapterId) {
        return ApiResponse.success(novelQueryService.readChapter(chapterId));
    }

    @PutMapping("/{chapterId}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long chapterId,
                                                   @Valid @RequestBody SaveChapterRequest request) {
        return ApiResponse.success(
                authorWriteService.updateChapter(UserContext.requireUserId(), chapterId, request));
    }

    @PostMapping("/{chapterId}/publish")
    public ApiResponse<Void> publish(@PathVariable Long chapterId) {
        authorWriteService.publishChapter(UserContext.requireUserId(), chapterId);
        return ApiResponse.success();
    }

    @PostMapping("/{chapterId}/offline")
    public ApiResponse<Void> offline(@PathVariable Long chapterId) {
        authorWriteService.offlineChapter(UserContext.requireUserId(), chapterId);
        return ApiResponse.success();
    }
}
