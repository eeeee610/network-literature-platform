package com.novel.platform.modules.novel.service;

import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.common.security.UserContext;
import com.novel.platform.modules.novel.dto.ChapterCatalogItem;
import com.novel.platform.modules.novel.dto.ChapterCatalogResponse;
import com.novel.platform.modules.novel.dto.ChapterReadResponse;
import com.novel.platform.modules.novel.dto.ChapterRow;
import com.novel.platform.modules.novel.dto.LatestChapterInfo;
import com.novel.platform.modules.novel.dto.NovelDetailResponse;
import com.novel.platform.modules.novel.dto.NovelListItem;
import com.novel.platform.modules.novel.dto.NovelRow;
import com.novel.platform.modules.novel.dto.PageResult;
import com.novel.platform.modules.novel.dto.TagBrief;
import com.novel.platform.modules.novel.dto.TagDetail;
import com.novel.platform.modules.novel.dto.TagRow;
import com.novel.platform.modules.novel.mapper.ChapterMapper;
import com.novel.platform.modules.novel.mapper.NovelMapper;
import com.novel.platform.modules.novel.mapper.NovelTagMapper;
import com.novel.platform.modules.novel.mapper.TagMapper;
import com.novel.platform.modules.user.service.ChapterEntitlementService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 公开查询：书城列表、搜索、作品详情、公开目录、章节正文、标签列表。
 */
@Service
public class NovelQueryService {

    private static final Set<String> ALLOWED_SORTS = Set.of("update", "words", "new");

    private final NovelMapper novelMapper;
    private final ChapterMapper chapterMapper;
    private final TagMapper tagMapper;
    private final NovelTagMapper novelTagMapper;
    private final ChapterEntitlementService entitlementService;

    public NovelQueryService(NovelMapper novelMapper,
                             ChapterMapper chapterMapper,
                             TagMapper tagMapper,
                             NovelTagMapper novelTagMapper,
                             ChapterEntitlementService entitlementService) {
        this.novelMapper = novelMapper;
        this.chapterMapper = chapterMapper;
        this.tagMapper = tagMapper;
        this.novelTagMapper = novelTagMapper;
        this.entitlementService = entitlementService;
    }

    public PageResult<NovelListItem> listPublic(Integer pageRaw, Integer pageSizeRaw, String sortRaw,
                                                Integer status, String tagIdRaw) {
        int page = NovelSupport.normalizePage(pageRaw);
        int pageSize = NovelSupport.checkPageSize(pageSizeRaw);
        String sort = (sortRaw == null || sortRaw.isBlank()) ? "update" : sortRaw.trim();
        if (!ALLOWED_SORTS.contains(sort)) {
            throw NovelSupport.badRequest("sort: 仅支持 update/words/new");
        }
        if (status != null && status != 0 && status != 1) {
            throw NovelSupport.badRequest("status: 仅支持 0 或 1");
        }
        Long tagId = parseOptionalTagId(tagIdRaw);

        long total = novelMapper.countPublicList(status, tagId);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, page, pageSize);
        }
        List<NovelRow> rows = novelMapper.selectPublicList(status, tagId, sort,
                NovelSupport.offset(page, pageSize), pageSize);
        Map<Long, List<TagRow>> tags = loadTags(rows);
        List<NovelListItem> items = new ArrayList<>(rows.size());
        for (NovelRow row : rows) {
            items.add(toListItem(row, tags.getOrDefault(row.getNovelId(), List.of())));
        }
        return new PageResult<>(items, total, page, pageSize);
    }

    public PageResult<NovelListItem> search(String keywordRaw, String tagIdRaw,
                                            Integer pageRaw, Integer pageSizeRaw) {
        int page = NovelSupport.normalizePage(pageRaw);
        int pageSize = NovelSupport.checkPageSize(pageSizeRaw);
        String keyword = keywordRaw == null ? "" : keywordRaw.trim();
        if (keyword.isEmpty()) {
            throw NovelSupport.badRequest("keyword: 不能为空");
        }
        if (keyword.length() > 50) {
            throw NovelSupport.badRequest("keyword: 长度不能超过 50");
        }
        Long tagId = parseOptionalTagId(tagIdRaw);

        long total = novelMapper.countSearch(keyword, tagId);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, page, pageSize);
        }
        List<NovelRow> rows = novelMapper.searchPublic(keyword, tagId,
                NovelSupport.offset(page, pageSize), pageSize);
        Map<Long, List<TagRow>> tags = loadTags(rows);
        List<NovelListItem> items = new ArrayList<>(rows.size());
        for (NovelRow row : rows) {
            items.add(toListItem(row, tags.getOrDefault(row.getNovelId(), List.of())));
        }
        return new PageResult<>(items, total, page, pageSize);
    }

    public NovelDetailResponse detail(String novelIdRaw) {
        long novelId = NovelSupport.parseId(novelIdRaw, "novelId");
        NovelRow row = novelMapper.selectPublicDetail(novelId);
        if (row == null) {
            throw new BusinessException(40901, "作品不存在", HttpStatus.NOT_FOUND);
        }
        List<TagDetail> tags = new ArrayList<>();
        for (TagRow tagRow : novelTagMapper.selectDetailByNovelId(novelId)) {
            tags.add(new TagDetail(String.valueOf(tagRow.getTagId()), tagRow.getTagName(), tagRow.getSortOrder()));
        }
        LatestChapterInfo latestChapter = null;
        if (row.getLatestChapterId() != null && row.getLatestChapterTitle() != null) {
            latestChapter = new LatestChapterInfo(String.valueOf(row.getLatestChapterId()),
                    row.getLatestChapterNo(), row.getLatestChapterTitle(),
                    NovelSupport.iso(row.getLatestChapterPublishTime()));
        }
        return new NovelDetailResponse(
                String.valueOf(row.getNovelId()),
                row.getTitle(),
                row.getCoverUrl(),
                row.getDescription(),
                row.getStatus(),
                row.getTotalWords(),
                row.getChapterCount() == null ? 0 : row.getChapterCount(),
                latestChapter,
                String.valueOf(row.getAuthorId()),
                row.getAuthorName(),
                row.getAuthorAvatarUrl(),
                tags,
                NovelSupport.iso(row.getCreatedAt()),
                NovelSupport.iso(row.getUpdatedAt()));
    }

    public ChapterCatalogResponse catalog(String novelIdRaw) {
        long novelId = NovelSupport.parseId(novelIdRaw, "novelId");
        if (novelMapper.selectPublicDetail(novelId) == null) {
            throw new BusinessException(40901, "作品不存在", HttpStatus.NOT_FOUND);
        }
        List<ChapterRow> rows = chapterMapper.selectPublicCatalog(novelId);
        List<ChapterCatalogItem> items = new ArrayList<>(rows.size());
        for (ChapterRow row : rows) {
            items.add(new ChapterCatalogItem(
                    String.valueOf(row.getChapterId()),
                    row.getChapterNo(),
                    row.getTitle(),
                    row.getIsVip(),
                    row.getVipPrice(),
                    row.getWordCount(),
                    NovelSupport.iso(row.getPublishTime())));
        }
        return new ChapterCatalogResponse(String.valueOf(novelId), items.size(), items);
    }

    public ChapterReadResponse readChapter(String chapterIdRaw) {
        long chapterId = NovelSupport.parseId(chapterIdRaw, "chapterId");
        ChapterRow row = chapterMapper.selectChapterForRead(chapterId);
        if (row == null) {
            throw new BusinessException(40903, "章节不存在", HttpStatus.NOT_FOUND);
        }
        boolean canRead = !Integer.valueOf(1).equals(row.getIsVip())
                || entitlementService.hasActiveSubscription(UserContext.currentUserId(), chapterId);
        ChapterRow prev = chapterMapper.selectPrevChapter(row.getNovelId(), row.getChapterNo());
        ChapterRow next = chapterMapper.selectNextChapter(row.getNovelId(), row.getChapterNo());
        return new ChapterReadResponse(
                String.valueOf(row.getChapterId()),
                String.valueOf(row.getNovelId()),
                row.getNovelTitle(),
                row.getChapterNo(),
                row.getTitle(),
                canRead ? row.getContent() : null,
                canRead,
                row.getIsVip(),
                row.getVipPrice(),
                row.getWordCount(),
                NovelSupport.iso(row.getPublishTime()),
                prev == null ? null : String.valueOf(prev.getChapterId()),
                prev == null ? null : prev.getTitle(),
                next == null ? null : String.valueOf(next.getChapterId()),
                next == null ? null : next.getTitle());
    }

    public List<TagBrief> listTags() {
        List<TagRow> rows = tagMapper.selectActiveTags();
        List<TagBrief> tags = new ArrayList<>(rows.size());
        for (TagRow row : rows) {
            tags.add(new TagBrief(String.valueOf(row.getTagId()), row.getTagName()));
        }
        return tags;
    }

    private Long parseOptionalTagId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return NovelSupport.parseId(raw, "tagId");
    }

    private Map<Long, List<TagRow>> loadTags(List<NovelRow> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        List<Long> novelIds = new ArrayList<>(rows.size());
        for (NovelRow row : rows) {
            novelIds.add(row.getNovelId());
        }
        Map<Long, List<TagRow>> grouped = new HashMap<>();
        for (TagRow tagRow : novelTagMapper.selectByNovelIds(novelIds)) {
            grouped.computeIfAbsent(tagRow.getNovelId(), key -> new ArrayList<>()).add(tagRow);
        }
        return grouped;
    }

    private NovelListItem toListItem(NovelRow row, List<TagRow> tagRows) {
        List<TagBrief> tags = new ArrayList<>(tagRows.size());
        for (TagRow tagRow : tagRows) {
            tags.add(new TagBrief(String.valueOf(tagRow.getTagId()), tagRow.getTagName()));
        }
        return new NovelListItem(
                String.valueOf(row.getNovelId()),
                row.getTitle(),
                row.getCoverUrl(),
                row.getDescription(),
                row.getStatus(),
                row.getTotalWords(),
                row.getLatestChapterId() == null ? null : String.valueOf(row.getLatestChapterId()),
                row.getLatestChapterTitle(),
                String.valueOf(row.getAuthorId()),
                row.getAuthorName(),
                tags,
                NovelSupport.iso(row.getUpdatedAt()));
    }
}
