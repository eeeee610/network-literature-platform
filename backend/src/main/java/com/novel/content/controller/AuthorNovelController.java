package com.novel.content.controller;

import com.novel.content.auth.UserContext;
import com.novel.content.common.ApiResponse;
import com.novel.content.common.PageResult;
import com.novel.content.dto.CreateNovelRequest;
import com.novel.content.dto.NovelListItem;
import com.novel.content.dto.PageQuery;
import com.novel.content.dto.SaveChapterRequest;
import com.novel.content.service.AuthorWriteService;
import com.novel.content.service.NovelQueryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AuthorNovelController {

    private final NovelQueryService novelQueryService;
    private final AuthorWriteService authorWriteService;

    public AuthorNovelController(NovelQueryService novelQueryService, AuthorWriteService authorWriteService) {
        this.novelQueryService = novelQueryService;
        this.authorWriteService = authorWriteService;
    }

    @PostMapping("/novels")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateNovelRequest request) {
        return ApiResponse.success(authorWriteService.createNovel(UserContext.requireUserId(), request));
    }

    @GetMapping("/author/novels")
    public ApiResponse<PageResult<NovelListItem>> myNovels(
            @RequestParam(required = false) Integer page,
            @RequestParam(name = "page_size", required = false) Integer pageSize,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(novelQueryService.authorNovels(
                UserContext.requireUserId(), status, PageQuery.of(page, pageSize)));
    }

    @PostMapping("/novels/{novelId}/chapters")
    public ApiResponse<Map<String, Object>> createChapter(@PathVariable Long novelId,
                                                          @Valid @RequestBody SaveChapterRequest request) {
        return ApiResponse.success(
                authorWriteService.createChapter(UserContext.requireUserId(), novelId, request));
    }
}
