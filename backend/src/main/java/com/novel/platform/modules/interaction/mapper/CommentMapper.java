package com.novel.platform.modules.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novel.platform.modules.interaction.dto.CommentRow;
import com.novel.platform.modules.interaction.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    List<CommentRow> selectTopComments(@Param("novelId") Long novelId,
                                       @Param("chapterId") Long chapterId,
                                       @Param("viewerUserId") Long viewerUserId,
                                       @Param("offset") int offset,
                                       @Param("size") int size);

    long countTopComments(@Param("novelId") Long novelId,
                          @Param("chapterId") Long chapterId);

    List<CommentRow> selectReplies(@Param("parentIds") List<Long> parentIds,
                                   @Param("viewerUserId") Long viewerUserId);

    List<CommentRow> selectAdminComments(@Param("novelId") Long novelId,
                                         @Param("status") Integer status,
                                         @Param("auditStatus") Integer auditStatus,
                                         @Param("keyword") String keyword,
                                         @Param("offset") int offset,
                                         @Param("size") int size);

    long countAdminComments(@Param("novelId") Long novelId,
                            @Param("status") Integer status,
                            @Param("auditStatus") Integer auditStatus,
                            @Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM v_comment_visible WHERE comment_id = #{commentId}")
    int countVisible(@Param("commentId") Long commentId);
}
