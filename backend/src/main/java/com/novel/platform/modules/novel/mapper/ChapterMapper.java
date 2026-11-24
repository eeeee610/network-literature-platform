package com.novel.platform.modules.novel.mapper;

import com.novel.platform.modules.novel.dto.ChapterRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChapterMapper {

    List<ChapterRow> selectPublicCatalog(@Param("novelId") long novelId);

    ChapterRow selectChapterForRead(@Param("chapterId") long chapterId);

    ChapterRow selectPrevChapter(@Param("novelId") long novelId, @Param("chapterNo") int chapterNo);

    ChapterRow selectNextChapter(@Param("novelId") long novelId, @Param("chapterNo") int chapterNo);

    List<ChapterRow> selectAuthorChapters(@Param("novelId") long novelId,
                                          @Param("offset") long offset,
                                          @Param("pageSize") int pageSize);

    long countAuthorChapters(@Param("novelId") long novelId);

    ChapterRow selectChapterById(@Param("chapterId") long chapterId);

    ChapterRow selectAuthorChapterDetail(@Param("chapterId") long chapterId);
}
