package com.novel.content.service;

import com.novel.content.common.BizException;
import com.novel.content.dto.CreateNovelRequest;
import com.novel.content.dto.SaveChapterRequest;
import com.novel.content.entity.Chapter;
import com.novel.content.mapper.ChapterMapper;
import com.novel.content.mapper.NovelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthorWriteService {

    private final ProcRepository procRepository;
    private final NovelMapper novelMapper;
    private final ChapterMapper chapterMapper;

    public AuthorWriteService(ProcRepository procRepository, NovelMapper novelMapper, ChapterMapper chapterMapper) {
        this.procRepository = procRepository;
        this.novelMapper = novelMapper;
        this.chapterMapper = chapterMapper;
    }

    public Map<String, Object> createNovel(long authorId, CreateNovelRequest request) {
        String coverUrl = blankToNull(request.getCoverUrl());
        String description = blankToNull(request.getDescription());
        Long novelId = procRepository.createNovel(authorId, request.getTitle().trim(),
                coverUrl, description, request.getTagIds());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("novel_id", novelId);
        return data;
    }

    public Map<String, Object> createChapter(long authorId, long novelId, SaveChapterRequest request) {
        checkVipPrice(request);
        Long ownerId = novelMapper.selectAuthorId(novelId);
        if (ownerId == null) {
            throw BizException.notFound("作品不存在");
        }
        if (ownerId.longValue() != authorId) {
            throw BizException.forbidden("无权操作他人作品");
        }
        Long chapterId = procRepository.saveChapterDraft(null, novelId, request.getTitle().trim(),
                request.getContent(), request.getIsVip(), request.getVipPrice());
        return chapterResult(chapterMapper.selectMeta(chapterId));
    }

    public Map<String, Object> updateChapter(long authorId, long chapterId, SaveChapterRequest request) {
        checkVipPrice(request);
        Chapter meta = requireOwnedChapter(authorId, chapterId);
        Long savedId = procRepository.saveChapterDraft(chapterId, meta.getNovelId(), request.getTitle().trim(),
                request.getContent(), request.getIsVip(), request.getVipPrice());
        return chapterResult(chapterMapper.selectMeta(savedId));
    }

    public void publishChapter(long authorId, long chapterId) {
        requireOwnedChapter(authorId, chapterId);
        procRepository.publishChapter(chapterId);
    }

    public void offlineChapter(long authorId, long chapterId) {
        requireOwnedChapter(authorId, chapterId);
        procRepository.offlineChapter(chapterId);
    }

    private Chapter requireOwnedChapter(long authorId, long chapterId) {
        Chapter meta = chapterMapper.selectMeta(chapterId);
        if (meta == null) {
            throw BizException.notFound("章节不存在");
        }
        Long ownerId = novelMapper.selectAuthorId(meta.getNovelId());
        if (ownerId == null || ownerId.longValue() != authorId) {
            throw BizException.forbidden("无权操作他人作品");
        }
        return meta;
    }

    private void checkVipPrice(SaveChapterRequest request) {
        BigDecimal price = request.getVipPrice();
        if (request.getIsVip() == 0 && price.compareTo(BigDecimal.ZERO) != 0) {
            throw BizException.badRequest("非 VIP 章节价格必须为 0.00");
        }
        if (request.getIsVip() == 1 && price.compareTo(BigDecimal.ZERO) <= 0) {
            throw BizException.badRequest("VIP 章节价格必须大于 0.00");
        }
    }

    private Map<String, Object> chapterResult(Chapter chapter) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("chapter_id", chapter.getChapterId());
        data.put("chapter_no", chapter.getChapterNo());
        data.put("word_count", chapter.getWordCount());
        return data;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
