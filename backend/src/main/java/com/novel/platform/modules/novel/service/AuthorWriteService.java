package com.novel.platform.modules.novel.service;

import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.novel.auth.AuthorContext;
import com.novel.platform.modules.novel.auth.AuthorPrincipal;
import com.novel.platform.modules.novel.dto.AuthorChapterDetailVO;
import com.novel.platform.modules.novel.dto.AuthorChapterItem;
import com.novel.platform.modules.novel.dto.AuthorNovelItem;
import com.novel.platform.modules.novel.dto.ChapterDraftResponse;
import com.novel.platform.modules.novel.dto.ChapterRow;
import com.novel.platform.modules.novel.dto.ChapterUpdateResponse;
import com.novel.platform.modules.novel.dto.CreateChapterRequest;
import com.novel.platform.modules.novel.dto.CreateNovelParam;
import com.novel.platform.modules.novel.dto.CreateNovelRequest;
import com.novel.platform.modules.novel.dto.CreateNovelResponse;
import com.novel.platform.modules.novel.dto.NovelRow;
import com.novel.platform.modules.novel.dto.OfflineChapterResponse;
import com.novel.platform.modules.novel.dto.PageResult;
import com.novel.platform.modules.novel.dto.PublishChapterResponse;
import com.novel.platform.modules.novel.dto.SaveChapterDraftParam;
import com.novel.platform.modules.novel.dto.TagBrief;
import com.novel.platform.modules.novel.dto.TagRow;
import com.novel.platform.modules.novel.dto.UpdateChapterRequest;
import com.novel.platform.modules.novel.dto.UpdateNovelRequest;
import com.novel.platform.modules.novel.dto.UpdateNovelResponse;
import com.novel.platform.modules.novel.entity.NovelTag;
import com.novel.platform.modules.novel.mapper.ChapterMapper;
import com.novel.platform.modules.novel.mapper.NovelMapper;
import com.novel.platform.modules.novel.mapper.NovelTagMapper;
import com.novel.platform.modules.novel.mapper.TagMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 作者写操作：创建/编辑作品、我的作品、章节草稿、发布与下架。
 * 调用存储过程的方法不加 @Transactional（过程自管事务）；编辑作品由后端单一事务完成。
 */
@Service
public class AuthorWriteService {

    private final NovelMapper novelMapper;
    private final ChapterMapper chapterMapper;
    private final TagMapper tagMapper;
    private final NovelTagMapper novelTagMapper;
    private final NovelProcRepository procRepository;

    public AuthorWriteService(NovelMapper novelMapper,
                              ChapterMapper chapterMapper,
                              TagMapper tagMapper,
                              NovelTagMapper novelTagMapper,
                              NovelProcRepository procRepository) {
        this.novelMapper = novelMapper;
        this.chapterMapper = chapterMapper;
        this.tagMapper = tagMapper;
        this.novelTagMapper = novelTagMapper;
        this.procRepository = procRepository;
    }

    public CreateNovelResponse createNovel(CreateNovelRequest request) {
        AuthorPrincipal principal = AuthorContext.requireWriter();
        if (request == null) {
            throw NovelSupport.badRequest("请求体不能为空");
        }
        String title = requireTitle(request.title());
        String coverUrl = checkCoverUrl(request.coverUrl());
        String description = checkDescription(request.description());
        List<Long> tagIds = parseTagIds(request.tagIds(), false);

        CreateNovelParam param = new CreateNovelParam();
        param.setAuthorId(principal.userId());
        param.setTitle(title);
        param.setCoverUrl(coverUrl);
        param.setDescription(description);
        param.setTagIdsJson(toJsonArray(tagIds));
        procRepository.createNovel(param);
        if (param.getNovelId() == null) {
            throw new IllegalStateException("创建作品未返回主键");
        }
        return new CreateNovelResponse(String.valueOf(param.getNovelId()), 0, 0);
    }

    @Transactional
    public UpdateNovelResponse updateNovel(String novelIdRaw, UpdateNovelRequest request) {
        AuthorPrincipal principal = AuthorContext.requireWriter();
        long novelId = NovelSupport.parseId(novelIdRaw, "novelId");
        if (request == null) {
            throw NovelSupport.badRequest("请求体不能为空");
        }
        NovelRow novel = novelMapper.selectNovelForWrite(novelId);
        if (novel == null) {
            throw new BusinessException(40901, "作品不存在", HttpStatus.NOT_FOUND);
        }
        ensureOwner(novel, principal);

        String title = requireTitle(request.title());
        String coverUrl = checkCoverUrl(request.coverUrl());
        String description = checkDescription(request.description());
        if (request.tagIds() == null) {
            throw NovelSupport.badRequest("tagIds: 不能为空");
        }
        List<Long> tagIds = parseTagIds(request.tagIds(), true);
        if (!tagIds.isEmpty() && tagMapper.countActiveTagsByIds(tagIds) != tagIds.size()) {
            throw new BusinessException(40905, "标签不存在或已停用", HttpStatus.BAD_REQUEST);
        }

        novelMapper.updateForEdit(novelId, title, coverUrl, description);
        novelTagMapper.deleteByNovelId(novelId);
        if (!tagIds.isEmpty()) {
            List<NovelTag> links = new ArrayList<>(tagIds.size());
            for (int index = 0; index < tagIds.size(); index++) {
                links.add(new NovelTag(novelId, tagIds.get(index), index));
            }
            novelTagMapper.insertBatch(links);
        }
        NovelRow updated = novelMapper.selectNovelForWrite(novelId);
        return new UpdateNovelResponse(String.valueOf(novelId), 0, NovelSupport.iso(updated.getUpdatedAt()));
    }

    public PageResult<AuthorNovelItem> myNovels(Integer pageRaw, Integer pageSizeRaw) {
        AuthorPrincipal principal = AuthorContext.requireWriter();
        int page = NovelSupport.normalizePage(pageRaw);
        int pageSize = NovelSupport.checkPageSize(pageSizeRaw);
        long authorId = principal.userId();

        long total = novelMapper.countAuthorNovels(authorId);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, page, pageSize);
        }
        List<NovelRow> rows = novelMapper.selectAuthorNovels(authorId,
                NovelSupport.offset(page, pageSize), pageSize);
        Map<Long, List<TagRow>> tagMap = new HashMap<>();
        List<Long> novelIds = new ArrayList<>(rows.size());
        for (NovelRow row : rows) {
            novelIds.add(row.getNovelId());
        }
        for (TagRow tagRow : novelTagMapper.selectByNovelIds(novelIds)) {
            tagMap.computeIfAbsent(tagRow.getNovelId(), key -> new ArrayList<>()).add(tagRow);
        }
        List<AuthorNovelItem> items = new ArrayList<>(rows.size());
        for (NovelRow row : rows) {
            List<TagBrief> tags = new ArrayList<>();
            for (TagRow tagRow : tagMap.getOrDefault(row.getNovelId(), List.of())) {
                tags.add(new TagBrief(String.valueOf(tagRow.getTagId()), tagRow.getTagName()));
            }
            items.add(new AuthorNovelItem(
                    String.valueOf(row.getNovelId()),
                    row.getTitle(),
                    row.getCoverUrl(),
                    row.getStatus(),
                    row.getAuditStatus(),
                    row.getTotalWords(),
                    row.getChapterCount() == null ? 0 : row.getChapterCount(),
                    row.getLatestChapterId() == null ? null : String.valueOf(row.getLatestChapterId()),
                    tags,
                    NovelSupport.iso(row.getCreatedAt()),
                    NovelSupport.iso(row.getUpdatedAt())));
        }
        return new PageResult<>(items, total, page, pageSize);
    }

    public PageResult<AuthorChapterItem> myChapters(String novelIdRaw, Integer pageRaw, Integer pageSizeRaw) {
        AuthorPrincipal principal = AuthorContext.requireWriter();
        long novelId = NovelSupport.parseId(novelIdRaw, "novelId");
        int page = NovelSupport.normalizePage(pageRaw);
        int pageSize = NovelSupport.checkPageSize(pageSizeRaw);

        NovelRow novel = novelMapper.selectNovelForWrite(novelId);
        if (novel == null) {
            throw new BusinessException(40901, "作品不存在", HttpStatus.NOT_FOUND);
        }
        ensureOwner(novel, principal);

        long total = chapterMapper.countAuthorChapters(novelId);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, page, pageSize);
        }
        List<ChapterRow> rows = chapterMapper.selectAuthorChapters(novelId,
                NovelSupport.offset(page, pageSize), pageSize);
        List<AuthorChapterItem> items = new ArrayList<>(rows.size());
        for (ChapterRow row : rows) {
            items.add(new AuthorChapterItem(
                    String.valueOf(row.getChapterId()),
                    row.getChapterNo(),
                    row.getTitle(),
                    row.getIsVip(),
                    row.getVipPrice(),
                    row.getWordCount(),
                    row.getStatus(),
                    row.getAuditStatus(),
                    NovelSupport.iso(row.getPublishTime()),
                    NovelSupport.iso(row.getCreatedAt()),
                    NovelSupport.iso(row.getUpdatedAt())));
        }
        return new PageResult<>(items, total, page, pageSize);
    }

    public AuthorChapterDetailVO authorChapterDetail(String chapterIdRaw) {
        AuthorPrincipal principal = AuthorContext.requireWriter();
        long chapterId = NovelSupport.parseId(chapterIdRaw, "chapterId");
        ChapterRow chapter = chapterMapper.selectAuthorChapterDetail(chapterId);
        if (chapter == null) {
            throw new BusinessException(40903, "章节不存在", HttpStatus.NOT_FOUND);
        }
        NovelRow novel = novelMapper.selectNovelForWrite(chapter.getNovelId());
        if (novel == null) {
            throw new BusinessException(40901, "作品不存在", HttpStatus.NOT_FOUND);
        }
        ensureOwner(novel, principal);
        return new AuthorChapterDetailVO(
                String.valueOf(chapter.getChapterId()),
                String.valueOf(chapter.getNovelId()),
                chapter.getChapterNo(),
                chapter.getTitle(),
                chapter.getContent(),
                chapter.getIsVip(),
                chapter.getVipPrice(),
                chapter.getWordCount(),
                chapter.getStatus(),
                chapter.getAuditStatus(),
                NovelSupport.iso(chapter.getPublishTime()),
                NovelSupport.iso(chapter.getUpdatedAt()));
    }

    public ChapterDraftResponse createChapter(CreateChapterRequest request) {
        AuthorPrincipal principal = AuthorContext.requireWriter();
        if (request == null) {
            throw NovelSupport.badRequest("请求体不能为空");
        }
        long novelId = NovelSupport.parseId(request.novelId(), "novelId");
        NovelRow novel = novelMapper.selectNovelForWrite(novelId);
        if (novel == null) {
            throw new BusinessException(40901, "作品不存在", HttpStatus.NOT_FOUND);
        }
        ensureOwner(novel, principal);

        ChapterInput input = validateChapter(request.title(), request.content(),
                request.isVip(), request.vipPrice());
        SaveChapterDraftParam param = new SaveChapterDraftParam();
        param.setChapterId(null);
        param.setNovelId(novelId);
        param.setTitle(input.title());
        param.setContent(input.content());
        param.setIsVip(input.isVip());
        param.setVipPrice(input.vipPrice());
        procRepository.saveChapterDraft(param);

        ChapterRow saved = requireSavedChapter(param.getChapterIdOut());
        return new ChapterDraftResponse(
                String.valueOf(saved.getChapterId()),
                String.valueOf(saved.getNovelId()),
                saved.getChapterNo(),
                saved.getWordCount(),
                saved.getStatus(),
                saved.getAuditStatus());
    }

    public ChapterUpdateResponse updateChapter(String chapterIdRaw, UpdateChapterRequest request) {
        AuthorPrincipal principal = AuthorContext.requireWriter();
        long chapterId = NovelSupport.parseId(chapterIdRaw, "chapterId");
        if (request == null) {
            throw NovelSupport.badRequest("请求体不能为空");
        }
        ChapterRow chapter = chapterMapper.selectChapterById(chapterId);
        if (chapter == null) {
            throw new BusinessException(40903, "章节不存在", HttpStatus.NOT_FOUND);
        }
        NovelRow novel = novelMapper.selectNovelForWrite(chapter.getNovelId());
        if (novel == null) {
            throw new BusinessException(40901, "作品不存在", HttpStatus.NOT_FOUND);
        }
        ensureOwner(novel, principal);
        if (chapter.getStatus() != null && chapter.getStatus() == 2) {
            throw new BusinessException(40904, "已下架章节不可编辑，请新建章节", HttpStatus.CONFLICT);
        }

        ChapterInput input = validateChapter(request.title(), request.content(),
                request.isVip(), request.vipPrice());
        SaveChapterDraftParam param = new SaveChapterDraftParam();
        param.setChapterId(chapterId);
        param.setNovelId(chapter.getNovelId());
        param.setTitle(input.title());
        param.setContent(input.content());
        param.setIsVip(input.isVip());
        param.setVipPrice(input.vipPrice());
        procRepository.saveChapterDraft(param);

        ChapterRow saved = requireSavedChapter(chapterId);
        return new ChapterUpdateResponse(
                String.valueOf(saved.getChapterId()),
                saved.getChapterNo(),
                saved.getWordCount(),
                saved.getStatus(),
                saved.getAuditStatus());
    }

    public PublishChapterResponse publishChapter(String chapterIdRaw) {
        AuthorPrincipal principal = AuthorContext.requireWriter();
        long chapterId = NovelSupport.parseId(chapterIdRaw, "chapterId");
        ChapterRow chapter = requireOwnedChapter(chapterId, principal);

        procRepository.publishChapter(chapterId);
        ChapterRow saved = chapterMapper.selectChapterById(chapterId);
        if (saved == null) {
            throw new BusinessException(40903, "章节不存在", HttpStatus.NOT_FOUND);
        }
        return new PublishChapterResponse(
                String.valueOf(saved.getChapterId()),
                String.valueOf(saved.getNovelId()),
                saved.getChapterNo(),
                saved.getWordCount(),
                saved.getStatus(),
                NovelSupport.iso(saved.getPublishTime()));
    }

    public OfflineChapterResponse offlineChapter(String chapterIdRaw) {
        AuthorPrincipal principal = AuthorContext.requireWriter();
        long chapterId = NovelSupport.parseId(chapterIdRaw, "chapterId");
        ChapterRow chapter = requireOwnedChapter(chapterId, principal);

        procRepository.offlineChapter(chapterId);
        ChapterRow saved = chapterMapper.selectChapterById(chapterId);
        if (saved == null) {
            throw new BusinessException(40903, "章节不存在", HttpStatus.NOT_FOUND);
        }
        NovelRow novel = novelMapper.selectNovelForWrite(saved.getNovelId());
        if (novel == null) {
            throw new BusinessException(40901, "作品不存在", HttpStatus.NOT_FOUND);
        }
        return new OfflineChapterResponse(
                String.valueOf(saved.getChapterId()),
                String.valueOf(saved.getNovelId()),
                saved.getStatus(),
                novel.getTotalWords(),
                novel.getLatestChapterId() == null ? null : String.valueOf(novel.getLatestChapterId()));
    }

    private ChapterRow requireOwnedChapter(long chapterId, AuthorPrincipal principal) {
        ChapterRow chapter = chapterMapper.selectChapterById(chapterId);
        if (chapter == null) {
            throw new BusinessException(40903, "章节不存在", HttpStatus.NOT_FOUND);
        }
        NovelRow novel = novelMapper.selectNovelForWrite(chapter.getNovelId());
        if (novel == null) {
            throw new BusinessException(40901, "作品不存在", HttpStatus.NOT_FOUND);
        }
        ensureOwner(novel, principal);
        return chapter;
    }

    private ChapterRow requireSavedChapter(Long chapterId) {
        if (chapterId == null) {
            throw new IllegalStateException("保存章节未返回主键");
        }
        ChapterRow saved = chapterMapper.selectChapterById(chapterId);
        if (saved == null) {
            throw new BusinessException(40903, "章节不存在", HttpStatus.NOT_FOUND);
        }
        return saved;
    }

    private void ensureOwner(NovelRow novel, AuthorPrincipal principal) {
        if (!principal.isAdmin() && novel.getAuthorId() != null
                && novel.getAuthorId().longValue() != principal.userId()) {
            throw new BusinessException(40902, "无权操作该作品", HttpStatus.FORBIDDEN);
        }
    }

    private String requireTitle(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw NovelSupport.badRequest("title: 不能为空");
        }
        String title = raw.trim();
        if (title.length() > 120) {
            throw NovelSupport.badRequest("title: 长度不能超过 120");
        }
        return title;
    }

    private String checkCoverUrl(String raw) {
        if (raw != null && raw.length() > 255) {
            throw NovelSupport.badRequest("coverUrl: 长度不能超过 255");
        }
        return raw;
    }

    private String checkDescription(String raw) {
        if (raw != null && raw.length() > 1000) {
            throw NovelSupport.badRequest("description: 长度不能超过 1000");
        }
        return raw;
    }

    private List<Long> parseTagIds(List<String> raw, boolean required) {
        if (raw == null) {
            if (required) {
                throw NovelSupport.badRequest("tagIds: 不能为空");
            }
            return List.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (String value : raw) {
            long tagId = NovelSupport.parseId(value, "tagIds");
            if (!ids.add(tagId)) {
                throw NovelSupport.badRequest("tagIds: 不能重复");
            }
        }
        return new ArrayList<>(ids);
    }

    private String toJsonArray(List<Long> ids) {
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < ids.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(ids.get(index));
        }
        return builder.append(']').toString();
    }

    private ChapterInput validateChapter(String titleRaw, String contentRaw, Integer isVip, BigDecimal vipPrice) {
        String title = requireTitle(titleRaw);
        if (contentRaw == null || contentRaw.trim().isEmpty()) {
            throw NovelSupport.badRequest("content: 不能为空");
        }
        if (isVip == null || (isVip != 0 && isVip != 1)) {
            throw NovelSupport.badRequest("isVip: 只能为 0 或 1");
        }
        if (vipPrice == null) {
            throw NovelSupport.badRequest("vipPrice: 不能为空");
        }
        if (vipPrice.scale() > 2) {
            throw NovelSupport.badRequest("vipPrice: 最多两位小数");
        }
        if (isVip == 0 && vipPrice.compareTo(BigDecimal.ZERO) != 0) {
            throw NovelSupport.badRequest("vipPrice: 非 VIP 章节必须为 0.00");
        }
        if (isVip == 1 && vipPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw NovelSupport.badRequest("vipPrice: VIP 章节必须大于 0.00");
        }
        return new ChapterInput(title, contentRaw, isVip, vipPrice);
    }

    private record ChapterInput(String title, String content, Integer isVip, BigDecimal vipPrice) {
    }
}
