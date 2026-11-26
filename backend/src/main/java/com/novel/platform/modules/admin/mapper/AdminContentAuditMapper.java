package com.novel.platform.modules.admin.mapper;

import com.novel.platform.modules.admin.dto.AuditLogRow;
import com.novel.platform.modules.admin.dto.ChapterAuditRow;
import com.novel.platform.modules.admin.dto.NovelAuditRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminContentAuditMapper {

    List<NovelAuditRow> selectNovels(@Param("auditStatus") Integer auditStatus,
                                     @Param("keyword") String keyword,
                                     @Param("author") String author,
                                     @Param("offset") int offset,
                                     @Param("size") int size);

    long countNovels(@Param("auditStatus") Integer auditStatus,
                     @Param("keyword") String keyword,
                     @Param("author") String author);

    List<ChapterAuditRow> selectChapters(@Param("novelId") Long novelId,
                                         @Param("auditStatus") Integer auditStatus,
                                         @Param("keyword") String keyword,
                                         @Param("offset") int offset,
                                         @Param("size") int size);

    long countChapters(@Param("novelId") Long novelId,
                       @Param("auditStatus") Integer auditStatus,
                       @Param("keyword") String keyword);

    ChapterAuditRow selectChapterDetail(@Param("chapterId") Long chapterId);

    int countNovelById(@Param("novelId") Long novelId);

    int countChapterById(@Param("chapterId") Long chapterId);

    int updateNovelAuditStatus(@Param("novelId") Long novelId,
                               @Param("auditStatus") int auditStatus);

    int updateChapterAuditStatus(@Param("chapterId") Long chapterId,
                                 @Param("auditStatus") int auditStatus);

    List<AuditLogRow> selectAuditLogs(@Param("targetType") String targetType,
                                      @Param("targetId") Long targetId,
                                      @Param("adminId") Long adminId,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime,
                                      @Param("offset") int offset,
                                      @Param("size") int size);

    long countAuditLogs(@Param("targetType") String targetType,
                        @Param("targetId") Long targetId,
                        @Param("adminId") Long adminId,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);
}
