package com.novel.platform.modules.novel.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.modules.novel.dto.ChapterCatalogResponse;
import com.novel.platform.modules.novel.dto.ChapterReadResponse;
import com.novel.platform.modules.novel.dto.NovelDetailResponse;
import com.novel.platform.modules.novel.dto.NovelListItem;
import com.novel.platform.modules.novel.dto.PageResult;
import com.novel.platform.modules.novel.dto.TagBrief;
import com.novel.platform.modules.novel.service.NovelQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开接口：书城列表、搜索、作品详情、公开目录、章节正文、标签列表。
 */
@RestController
@RequestMapping("/api/novel")
public class NovelPublicController {

    private final NovelQueryService queryService;

    public NovelPublicController(NovelQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/list")
    public ApiResponse<PageResult<NovelListItem>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String tagId) {
        return ApiResponse.success(queryService.listPublic(page, pageSize, sort, status, tagId));
    }

    @GetMapping("/search")
    public ApiResponse<PageResult<NovelListItem>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tagId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.success(queryService.search(keyword, tagId, page, pageSize));
    }

    @GetMapping("/chapter/{chapterId}")
    public ApiResponse<ChapterReadResponse> chapter(@PathVariable String chapterId) {
        return ApiResponse.success(queryService.readChapter(chapterId));
    }

    @GetMapping("/tag/list")
    public ApiResponse<List<TagBrief>> tags() {
        return ApiResponse.success(queryService.listTags());
    }

    @GetMapping("/{novelId}")
    public ApiResponse<NovelDetailResponse> detail(@PathVariable String novelId) {
        return ApiResponse.success(queryService.detail(novelId));
    }

    @GetMapping("/{novelId}/chapters")
    public ApiResponse<ChapterCatalogResponse> catalog(@PathVariable String novelId) {
        return ApiResponse.success(queryService.catalog(novelId));
    }
}
