package com.novel.platform.modules.user.mapper;

import com.novel.platform.modules.user.dto.BookshelfItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookshelfMapper {

    Long selectVisibleNovelId(@Param("novelId") Long novelId);

    BookshelfItem selectItem(@Param("userId") Long userId, @Param("novelId") Long novelId);

    int insertIgnore(@Param("userId") Long userId, @Param("novelId") Long novelId);

    int delete(@Param("userId") Long userId, @Param("novelId") Long novelId);

    long countByUser(@Param("userId") Long userId);

    List<BookshelfItem> selectPage(@Param("userId") Long userId,
                                   @Param("offset") long offset,
                                   @Param("pageSize") int pageSize);
}
