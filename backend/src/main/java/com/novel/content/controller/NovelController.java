package com.novel.content.controller;

import com.novel.content.common.ApiResponse;
import com.novel.content.common.PageResult;
import com.novel.content.dto.NovelDetailVO;
import com.novel.content.dto.NovelListItem;
import com.novel.content.dto.PageQuery;
import com.novel.content.service.NovelQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class NovelController {

    private final NovelQueryService novelQueryService;

    public NovelController(NovelQueryService novelQueryService) {
        this.novelQueryService = novelQueryService;
    }

    @GetMapping("/novels")
    public ApiResponse<PageResult<NovelListItem>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(name = "page_size", required = false) Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(name = "tag_id", required = false) Long tagId,
            @RequestParam(required = false) String sort) {
        return ApiResponse.success(novelQueryService.bookstore(PageQuery.of(page, pageSize), status, tagId, sort));
    }

    @GetMapping("/novels/search")
    public ApiResponse<PageResult<NovelListItem>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "tag_id", required = false) Long tagId,
            @RequestParam(required = false) Integer page,
            @RequestParam(name = "page_size", required = false) Integer pageSize) {
        return ApiResponse.success(novelQueryService.search(keyword, tagId, PageQuery.of(page, pageSize)));
    }

    @GetMapping("/novels/{novelId}")
    public ApiResponse<NovelDetailVO> detail(@PathVariable Long novelId) {
        return ApiResponse.success(novelQueryService.detail(novelId));
    }

    @GetMapping("/novels/{novelId}/chapters")
    public ApiResponse<Map<String, Object>> catalog(@PathVariable Long novelId) {
        return ApiResponse.success(novelQueryService.catalog(novelId));
    }
}
