package com.novel.platform.modules.novel.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.modules.novel.dto.AuthorChapterDetailVO;
import com.novel.platform.modules.novel.dto.AuthorChapterItem;
import com.novel.platform.modules.novel.dto.AuthorNovelItem;
import com.novel.platform.modules.novel.dto.ChapterDraftResponse;
import com.novel.platform.modules.novel.dto.ChapterUpdateResponse;
import com.novel.platform.modules.novel.dto.CreateChapterRequest;
import com.novel.platform.modules.novel.dto.CreateNovelRequest;
import com.novel.platform.modules.novel.dto.CreateNovelResponse;
import com.novel.platform.modules.novel.dto.OfflineChapterResponse;
import com.novel.platform.modules.novel.dto.PageResult;
import com.novel.platform.modules.novel.dto.PublishChapterResponse;
import com.novel.platform.modules.novel.dto.UpdateChapterRequest;
import com.novel.platform.modules.novel.dto.UpdateNovelRequest;
import com.novel.platform.modules.novel.dto.UpdateNovelResponse;
import com.novel.platform.modules.novel.service.AuthorWriteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 作者接口：作品创建/编辑、我的作品、章节管理与发布下架。
 * 作者身份只取自令牌，请求体中的 authorId 一律忽略。
 */
@RestController
@RequestMapping("/api/novel")
public class NovelAuthorController {

    private final AuthorWriteService authorWriteService;

    public NovelAuthorController(AuthorWriteService authorWriteService) {
        this.authorWriteService = authorWriteService;
    }

    @PostMapping
    public ApiResponse<CreateNovelResponse> create(@RequestBody(required = false) CreateNovelRequest request) {
        return ApiResponse.success(authorWriteService.createNovel(request));
    }

    @PutMapping("/{novelId}")
    public ApiResponse<UpdateNovelResponse> update(@PathVariable String novelId,
                                                   @RequestBody(required = false) UpdateNovelRequest request) {
        return ApiResponse.success(authorWriteService.updateNovel(novelId, request));
    }

    @GetMapping("/author/list")
    public ApiResponse<PageResult<AuthorNovelItem>> myNovels(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.success(authorWriteService.myNovels(page, pageSize));
    }

    @GetMapping("/author/{novelId}/chapters")
    public ApiResponse<PageResult<AuthorChapterItem>> myChapters(
            @PathVariable String novelId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.success(authorWriteService.myChapters(novelId, page, pageSize));
    }

    @GetMapping("/author/chapter/{chapterId}")
    public ApiResponse<AuthorChapterDetailVO> authorChapterDetail(@PathVariable String chapterId) {
        return ApiResponse.success(authorWriteService.authorChapterDetail(chapterId));
    }

    @PostMapping("/chapter")
    public ApiResponse<ChapterDraftResponse> createChapter(
            @RequestBody(required = false) CreateChapterRequest request) {
        return ApiResponse.success(authorWriteService.createChapter(request));
    }

    @PutMapping("/chapter/{chapterId}")
    public ApiResponse<ChapterUpdateResponse> updateChapter(
            @PathVariable String chapterId,
            @RequestBody(required = false) UpdateChapterRequest request) {
        return ApiResponse.success(authorWriteService.updateChapter(chapterId, request));
    }

    @PostMapping("/chapter/{chapterId}/publish")
    public ApiResponse<PublishChapterResponse> publish(@PathVariable String chapterId) {
        return ApiResponse.success(authorWriteService.publishChapter(chapterId));
    }

    @PostMapping("/chapter/{chapterId}/offline")
    public ApiResponse<OfflineChapterResponse> offline(@PathVariable String chapterId) {
        return ApiResponse.success(authorWriteService.offlineChapter(chapterId));
    }
}
