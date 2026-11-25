package com.novel.platform.modules.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novel.platform.modules.interaction.dto.RatingRow;
import com.novel.platform.modules.interaction.entity.NovelRating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NovelRatingMapper extends BaseMapper<NovelRating> {

    List<RatingRow> selectPublicReviews(@Param("novelId") Long novelId,
                                        @Param("offset") int offset,
                                        @Param("size") int size);

    long countPublicReviews(@Param("novelId") Long novelId);

    List<RatingRow> selectAdminReviews(@Param("reviewAuditStatus") Integer reviewAuditStatus,
                                       @Param("offset") int offset,
                                       @Param("size") int size);

    long countAdminReviews(@Param("reviewAuditStatus") Integer reviewAuditStatus);
}
