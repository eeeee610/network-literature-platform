package com.novel.platform.modules.admin.service;

import com.novel.platform.common.api.PageResult;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.common.util.IdUtils;
import com.novel.platform.modules.admin.dto.AuditLogItem;
import com.novel.platform.modules.admin.dto.AuditLogRow;
import com.novel.platform.modules.admin.dto.ChapterAuditDetail;
import com.novel.platform.modules.admin.dto.ChapterAuditItem;
import com.novel.platform.modules.admin.dto.ChapterAuditRow;
import com.novel.platform.modules.admin.dto.ContentAuditRequest;
import com.novel.platform.modules.admin.dto.NovelAuditItem;
import com.novel.platform.modules.admin.dto.NovelAuditRow;
import com.novel.platform.modules.admin.mapper.AdminContentAuditMapper;
import com.novel.platform.modules.interaction.mapper.AuditLogMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AdminContentAuditService {

    private static final Set<String> TARGET_TYPES = Set.of("NOVEL", "CHAPTER", "COMMENT", "RATING");

    private final AdminContentAuditMapper auditMapper;
    private final AuditLogMapper auditLogMapper;

    public AdminContentAuditService(AdminContentAuditMapper auditMapper, AuditLogMapper auditLogMapper) {
        this.auditMapper = auditMapper;
        this.auditLogMapper = auditLogMapper;
    }

    public PageResult<NovelAuditItem> novels(Integer auditStatus, String keyword, String author,
                                             int pageParam, int pageSizeParam) {
        int page = AdminPagination.page(pageParam);
        int pageSize = AdminPagination.size(pageSizeParam);
        String normalizedKeyword = trimToNull(keyword);
        String normalizedAuthor = trimToNull(author);
        List<NovelAuditItem> items = auditMapper.selectNovels(
                        auditStatus, normalizedKeyword, normalizedAuthor,
                        AdminPagination.offset(page, pageSize), pageSize)
                .stream().map(this::toNovelItem).toList();
        long total = auditMapper.countNovels(auditStatus, normalizedKeyword, normalizedAuthor);
        return new PageResult<>(items, total, page, pageSize);
    }

    public PageResult<ChapterAuditItem> chapters(String novelIdValue, Integer auditStatus, String keyword,
                                                 int pageParam, int pageSizeParam) {
        Long novelId = IdUtils.parseNullable(novelIdValue, "novelId");
        int page = AdminPagination.page(pageParam);
        int pageSize = AdminPagination.size(pageSizeParam);
        String normalizedKeyword = trimToNull(keyword);
        List<ChapterAuditItem> items = auditMapper.selectChapters(
                        novelId, auditStatus, normalizedKeyword,
                        AdminPagination.offset(page, pageSize), pageSize)
                .stream().map(this::toChapterItem).toList();
        long total = auditMapper.countChapters(novelId, auditStatus, normalizedKeyword);
        return new PageResult<>(items, total, page, pageSize);
    }

    public ChapterAuditDetail chapterDetail(Long chapterId) {
        ChapterAuditRow row = auditMapper.selectChapterDetail(chapterId);
        if (row == null) {
            throw new BusinessException(40401, "章节不存在", HttpStatus.NOT_FOUND);
        }
        return new ChapterAuditDetail(toChapterItem(row), row.getContent());
    }

    @Transactional
    public void auditNovel(Long novelId, ContentAuditRequest request, Long adminId) {
        validateReason(request);
        if (auditMapper.countNovelById(novelId) == 0) {
            throw new BusinessException(40401, "作品不存在", HttpStatus.NOT_FOUND);
        }
        auditMapper.updateNovelAuditStatus(novelId, auditStatus(request));
        auditLogMapper.insert(adminId, "NOVEL", novelId, request.action(), trimToNull(request.reason()));
    }

    @Transactional
    public void auditChapter(Long chapterId, ContentAuditRequest request, Long adminId) {
        validateReason(request);
        if (auditMapper.countChapterById(chapterId) == 0) {
            throw new BusinessException(40401, "章节不存在", HttpStatus.NOT_FOUND);
        }
        auditMapper.updateChapterAuditStatus(chapterId, auditStatus(request));
        auditLogMapper.insert(adminId, "CHAPTER", chapterId, request.action(), trimToNull(request.reason()));
    }

    public PageResult<AuditLogItem> logs(String targetTypeValue, String targetIdValue, String adminIdValue,
                                         LocalDateTime startTime, LocalDateTime endTime,
                                         int pageParam, int pageSizeParam) {
        String targetType = normalizeTargetType(targetTypeValue);
        Long targetId = IdUtils.parseNullable(targetIdValue, "targetId");
        Long adminId = IdUtils.parseNullable(adminIdValue, "adminId");
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new BusinessException(40000, "开始时间不能晚于结束时间");
        }
        int page = AdminPagination.page(pageParam);
        int pageSize = AdminPagination.size(pageSizeParam);
        List<AuditLogItem> items = auditMapper.selectAuditLogs(
                        targetType, targetId, adminId, startTime, endTime,
                        AdminPagination.offset(page, pageSize), pageSize)
                .stream().map(this::toLogItem).toList();
        long total = auditMapper.countAuditLogs(targetType, targetId, adminId, startTime, endTime);
        return new PageResult<>(items, total, page, pageSize);
    }

    private void validateReason(ContentAuditRequest request) {
        if (request.action() == 2 && (request.reason() == null || request.reason().isBlank())) {
            throw new BusinessException(40000, "驳回时必须填写审核原因");
        }
    }

    private int auditStatus(ContentAuditRequest request) {
        return request.action() == 1 ? 1 : 2;
    }

    private String normalizeTargetType(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!TARGET_TYPES.contains(normalized)) {
            throw new BusinessException(40000, "targetType 只能为 NOVEL、CHAPTER、COMMENT 或 RATING");
        }
        return normalized;
    }

    private NovelAuditItem toNovelItem(NovelAuditRow row) {
        return new NovelAuditItem(
                String.valueOf(row.getNovelId()), String.valueOf(row.getAuthorId()), row.getTitle(),
                row.getAuthorName(), row.getCoverUrl(), row.getDescription(), nvl(row.getStatus()),
                nvl(row.getAuditStatus()), nvl(row.getTotalWords()), row.getCreatedAt(), row.getUpdatedAt());
    }

    private ChapterAuditItem toChapterItem(ChapterAuditRow row) {
        return new ChapterAuditItem(
                String.valueOf(row.getChapterId()), String.valueOf(row.getNovelId()), row.getNovelTitle(),
                row.getAuthorName(), nvl(row.getChapterNo()), row.getTitle(), nvl(row.getWordCount()),
                nvl(row.getIsVip()), nvl(row.getStatus()), row.getAuditStatus(),
                row.getCreatedAt(), row.getUpdatedAt());
    }

    private AuditLogItem toLogItem(AuditLogRow row) {
        return new AuditLogItem(
                String.valueOf(row.getAuditId()), String.valueOf(row.getAdminId()), row.getAdminName(),
                row.getTargetType(), String.valueOf(row.getTargetId()), nvl(row.getAuditAction()),
                row.getAuditReason(), row.getCreatedAt());
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
