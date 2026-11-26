package com.novel.platform.modules.admin.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.common.api.PageResult;
import com.novel.platform.modules.admin.dto.AdminTagItem;
import com.novel.platform.modules.admin.dto.TagRequest;
import com.novel.platform.modules.admin.service.AdminTagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tag")
public class AdminTagController {

    private final AdminTagService tagService;

    public AdminTagController(AdminTagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminTagItem>> list(
            @RequestParam(required = false) String tagName,
            @RequestParam(required = false) Integer isActive,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(tagService.list(tagName, isActive, page, pageSize));
    }

    @PostMapping
    public ApiResponse<AdminTagItem> create(@Valid @RequestBody TagRequest request) {
        return ApiResponse.success(tagService.create(request));
    }

    @PutMapping("/{tagId}")
    public ApiResponse<AdminTagItem> update(@PathVariable Long tagId,
                                            @Valid @RequestBody TagRequest request) {
        return ApiResponse.success(tagService.update(tagId, request));
    }

    @DeleteMapping("/{tagId}")
    public ApiResponse<Void> delete(@PathVariable Long tagId) {
        tagService.delete(tagId);
        return ApiResponse.success();
    }
}
