package com.novel.platform.modules.novel.mapper;

import com.novel.platform.modules.novel.dto.NovelRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NovelMapper {

    List<NovelRow> selectPublicList(@Param("status") Integer status,
                                    @Param("tagId") Long tagId,
                                    @Param("sort") String sort,
                                    @Param("offset") long offset,
                                    @Param("pageSize") int pageSize);

    long countPublicList(@Param("status") Integer status, @Param("tagId") Long tagId);

    List<NovelRow> searchPublic(@Param("keyword") String keyword,
                                @Param("tagId") Long tagId,
                                @Param("offset") long offset,
                                @Param("pageSize") int pageSize);

    long countSearch(@Param("keyword") String keyword, @Param("tagId") Long tagId);

    NovelRow selectPublicDetail(@Param("novelId") long novelId);

    List<NovelRow> selectAuthorNovels(@Param("authorId") long authorId,
                                      @Param("offset") long offset,
                                      @Param("pageSize") int pageSize);

    long countAuthorNovels(@Param("authorId") long authorId);

    NovelRow selectNovelForWrite(@Param("novelId") long novelId);

    int updateForEdit(@Param("novelId") long novelId,
                      @Param("title") String title,
                      @Param("coverUrl") String coverUrl,
                      @Param("description") String description);
}
