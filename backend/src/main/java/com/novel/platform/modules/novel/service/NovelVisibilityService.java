package com.novel.platform.modules.novel.service;

import com.novel.platform.modules.novel.mapper.ChapterMapper;
import com.novel.platform.modules.novel.mapper.NovelMapper;
import org.springframework.stereotype.Service;

/**
 * B 模块公开可见性契约，供交易等跨模块场景复用公共阅读条件。
 */
@Service
public class NovelVisibilityService {

    private final NovelMapper novelMapper;
    private final ChapterMapper chapterMapper;

    public NovelVisibilityService(NovelMapper novelMapper, ChapterMapper chapterMapper) {
        this.novelMapper = novelMapper;
        this.chapterMapper = chapterMapper;
    }

    public boolean isPublicNovel(long novelId) {
        return novelMapper.selectPublicDetail(novelId) != null;
    }

    public boolean isPublicChapter(long chapterId) {
        return chapterMapper.selectChapterForRead(chapterId) != null;
    }
}
