package com.novel.platform.modules.admin.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.common.api.PageResult;
import com.novel.platform.common.security.UserContext;
import com.novel.platform.modules.admin.dto.AuditLogItem;
import com.novel.platform.modules.admin.dto.ChapterAuditDetail;
import com.novel.platform.modules.admin.dto.ChapterAuditItem;
import com.novel.platform.modules.admin.dto.ContentAuditRequest;
import com.novel.platform.modules.admin.dto.NovelAuditItem;
import com.novel.platform.modules.admin.service.AdminContentAuditService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/audit")
public class AdminContentAuditController {

    private final AdminContentAuditService auditService;

    public AdminContentAuditController(AdminContentAuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/novel")
    public ApiResponse<PageResult<NovelAuditItem>> novels(
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String author,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(auditService.novels(auditStatus, keyword, author, page, pageSize));
    }

    @PostMapping("/novel/{novelId}")
    public ApiResponse<Void> auditNovel(@PathVariable Long novelId,
                                        @Valid @RequestBody ContentAuditRequest request) {
        auditService.auditNovel(novelId, request, UserContext.currentUserId());
        return ApiResponse.success();
    }

    @GetMapping("/chapter")
    public ApiResponse<PageResult<ChapterAuditItem>> chapters(
            @RequestParam(required = false) String novelId,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(auditService.chapters(novelId, auditStatus, keyword, page, pageSize));
    }

    @GetMapping("/chapter/{chapterId}")
    public ApiResponse<ChapterAuditDetail> chapterDetail(@PathVariable Long chapterId) {
        return ApiResponse.success(auditService.chapterDetail(chapterId));
    }

    @PostMapping("/chapter/{chapterId}")
    public ApiResponse<Void> auditChapter(@PathVariable Long chapterId,
                                          @Valid @RequestBody ContentAuditRequest request) {
        auditService.auditChapter(chapterId, request, UserContext.currentUserId());
        return ApiResponse.success();
    }

    @GetMapping("/log")
    public ApiResponse<PageResult<AuditLogItem>> logs(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String adminId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(auditService.logs(
                targetType, targetId, adminId, startTime, endTime, page, pageSize));
    }
}
