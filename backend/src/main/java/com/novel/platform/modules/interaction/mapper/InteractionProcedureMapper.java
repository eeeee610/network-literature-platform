package com.novel.platform.modules.interaction.mapper;

import com.novel.platform.modules.interaction.dto.RankingDetailRow;
import com.novel.platform.modules.interaction.mapper.param.CommentCreateParam;
import com.novel.platform.modules.interaction.mapper.param.RankingGenerateParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.StatementType;

import java.util.List;

/**
 * 调用冻结的 C 模块存储过程。事务由 Service 层统一管理。
 */
@Mapper
public interface InteractionProcedureMapper {

    @Options(statementType = StatementType.CALLABLE)
    @Select("CALL sp_create_comment("
            + "#{novelId,mode=IN,jdbcType=BIGINT}, "
            + "#{chapterId,mode=IN,jdbcType=BIGINT}, "
            + "#{userId,mode=IN,jdbcType=BIGINT}, "
            + "#{parentId,mode=IN,jdbcType=BIGINT}, "
            + "#{replyToUserId,mode=IN,jdbcType=BIGINT}, "
            + "#{content,mode=IN,jdbcType=VARCHAR}, "
            + "#{commentId,mode=OUT,jdbcType=BIGINT})")
    void createComment(CommentCreateParam param);

    @Options(statementType = StatementType.CALLABLE)
    @Select("CALL sp_set_comment_status(#{commentId,mode=IN,jdbcType=BIGINT}, "
            + "#{status,mode=IN,jdbcType=TINYINT})")
    void setCommentStatus(@Param("commentId") Long commentId, @Param("status") Integer status);

    @Options(statementType = StatementType.CALLABLE)
    @Select("CALL sp_audit_comment(#{commentId,mode=IN,jdbcType=BIGINT}, "
            + "#{auditStatus,mode=IN,jdbcType=TINYINT})")
    void auditComment(@Param("commentId") Long commentId, @Param("auditStatus") Integer auditStatus);

    @Options(statementType = StatementType.CALLABLE)
    @Select("CALL sp_generate_ranking("
            + "#{rankingId,mode=IN,jdbcType=BIGINT}, "
            + "#{periodNo,mode=IN,jdbcType=VARCHAR}, "
            + "#{allowHistoryOverwrite,mode=IN,jdbcType=TINYINT}, "
            + "#{itemCount,mode=OUT,jdbcType=INTEGER})")
    void generateRanking(RankingGenerateParam param);

    @Options(statementType = StatementType.CALLABLE)
    @Select("CALL sp_get_ranking("
            + "#{rankingCode,mode=IN,jdbcType=VARCHAR}, "
            + "#{periodNo,mode=IN,jdbcType=VARCHAR}, "
            + "#{limit,mode=IN,jdbcType=INTEGER})")
    List<RankingDetailRow> getRanking(@Param("rankingCode") String rankingCode,
                                      @Param("periodNo") String periodNo,
                                      @Param("limit") Integer limit);
}
