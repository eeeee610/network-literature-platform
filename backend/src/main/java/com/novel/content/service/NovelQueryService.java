package com.novel.content.service;

import com.novel.content.common.BizException;
import com.novel.content.common.PageResult;
import com.novel.content.dto.ChapterListItem;
import com.novel.content.dto.ChapterVO;
import com.novel.content.dto.NovelDetailVO;
import com.novel.content.dto.NovelListItem;
import com.novel.content.dto.PageQuery;
import com.novel.content.entity.Chapter;
import com.novel.content.entity.Tag;
import com.novel.content.mapper.ChapterMapper;
import com.novel.content.mapper.NovelMapper;
import com.novel.content.mapper.TagMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NovelQueryService {

    private static final int SORT_UPDATE = 0;
    private static final int SORT_WORDS = 1;
    private static final int SORT_NEW = 2;

    private final NovelMapper novelMapper;
    private final ChapterMapper chapterMapper;
    private final TagMapper tagMapper;

    public NovelQueryService(NovelMapper novelMapper, ChapterMapper chapterMapper, TagMapper tagMapper) {
        this.novelMapper = novelMapper;
        this.chapterMapper = chapterMapper;
        this.tagMapper = tagMapper;
    }

    public PageResult<NovelListItem> bookstore(PageQuery pageQuery, Integer status, Long tagId, String sort) {
        if (status != null && status != 0 && status != 1) {
            throw BizException.badRequest("status 只能为 0 或 1");
        }
        int sortCode = parseSort(sort);
        long total = novelMapper.countBookstore(status, tagId);
        List<NovelListItem> list = total == 0
                ? List.of()
                : novelMapper.selectBookstore(status, tagId, sortCode, pageQuery.offset(), pageQuery.getPageSize());
        fillTagNames(list);
        return new PageResult<>(pageQuery.getPage(), pageQuery.getPageSize(), total, list);
    }

    public PageResult<NovelListItem> search(String keyword, Long tagId, PageQuery pageQuery) {
        String normalized = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        if (normalized == null && tagId == null) {
            throw BizException.badRequest("keyword 与 tag_id 至少传一个");
        }
        if (normalized != null && normalized.length() > 50) {
            throw BizException.badRequest("keyword 长度不能超过 50");
        }
        long total = novelMapper.countSearch(normalized, tagId);
        List<NovelListItem> list = total == 0
                ? List.of()
                : novelMapper.selectSearch(normalized, tagId, pageQuery.offset(), pageQuery.getPageSize());
        fillTagNames(list);
        return new PageResult<>(pageQuery.getPage(), pageQuery.getPageSize(), total, list);
    }

    public NovelDetailVO detail(Long novelId) {
        NovelDetailVO detail = novelMapper.selectDetail(novelId);
        if (detail == null) {
            throw BizException.notFound("作品不存在或不可展示");
        }
        detail.setTags(tagMapper.selectNovelTags(novelId));
        detail.setChapterCount(chapterMapper.countPublished(novelId));
        detail.setLastChapterTitle(chapterMapper.selectLastPublishedTitle(novelId));
        return detail;
    }

    public Map<String, Object> catalog(Long novelId) {
        if (novelMapper.countDisplayable(novelId) == 0) {
            throw BizException.notFound("作品不存在或不可展示");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("novel_id", novelId);
        data.put("list", chapterMapper.selectCatalog(novelId));
        return data;
    }

    public ChapterVO readChapter(Long chapterId) {
        Chapter chapter = chapterMapper.selectReadable(chapterId);
        if (chapter == null) {
            throw BizException.notFound("章节不存在或不可阅读");
        }
        ChapterVO vo = new ChapterVO();
        vo.setChapterId(chapter.getChapterId());
        vo.setNovelId(chapter.getNovelId());
        vo.setChapterNo(chapter.getChapterNo());
        vo.setTitle(chapter.getTitle());
        vo.setContent(chapter.getContent());
        vo.setIsVip(chapter.getIsVip());
        vo.setVipPrice(chapter.getVipPrice());
        vo.setWordCount(chapter.getWordCount());
        vo.setPublishTime(chapter.getPublishTime());
        vo.setPrevChapterId(chapterMapper.selectPrevChapterId(chapter.getNovelId(), chapter.getChapterNo()));
        vo.setNextChapterId(chapterMapper.selectNextChapterId(chapter.getNovelId(), chapter.getChapterNo()));
        return vo;
    }

    public List<Tag> tags() {
        return tagMapper.selectActive();
    }

    public PageResult<NovelListItem> authorNovels(long authorId, Integer status, PageQuery pageQuery) {
        if (status != null && (status < 0 || status > 2)) {
            throw BizException.badRequest("status 只能为 0/1/2");
        }
        long total = novelMapper.countByAuthor(authorId, status);
        List<NovelListItem> list = total == 0
                ? List.of()
                : novelMapper.selectByAuthor(authorId, status, pageQuery.offset(), pageQuery.getPageSize());
        fillTagNames(list);
        return new PageResult<>(pageQuery.getPage(), pageQuery.getPageSize(), total, list);
    }

    private int parseSort(String sort) {
        if (sort == null) {
            return SORT_UPDATE;
        }
        return switch (sort.trim().toLowerCase()) {
            case "words" -> SORT_WORDS;
            case "new" -> SORT_NEW;
            default -> SORT_UPDATE;
        };
    }

    private void fillTagNames(List<NovelListItem> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> novelIds = list.stream().map(NovelListItem::getNovelId).toList();
        Map<Long, List<String>> tagNamesByNovel = new HashMap<>();
        for (Map<String, Object> row : tagMapper.selectTagNames(novelIds)) {
            Long novelId = ((Number) row.get("novelId")).longValue();
            tagNamesByNovel.computeIfAbsent(novelId, key -> new ArrayList<>()).add((String) row.get("tagName"));
        }
        for (NovelListItem item : list) {
            item.setTagNames(tagNamesByNovel.getOrDefault(item.getNovelId(), List.of()));
        }
    }
}
