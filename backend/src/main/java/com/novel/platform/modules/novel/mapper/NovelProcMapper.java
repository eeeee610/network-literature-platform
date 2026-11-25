package com.novel.platform.modules.novel.mapper;

import com.novel.platform.modules.novel.dto.CreateNovelParam;
import com.novel.platform.modules.novel.dto.SaveChapterDraftParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NovelProcMapper {

    void createNovel(CreateNovelParam param);

    void saveChapterDraft(SaveChapterDraftParam param);

    void publishChapter(@Param("chapterId") Long chapterId);

    void offlineChapter(@Param("chapterId") Long chapterId);
}
