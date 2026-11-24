package com.novel.platform.modules.novel.service;

import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.novel.dto.CreateNovelParam;
import com.novel.platform.modules.novel.dto.SaveChapterDraftParam;
import com.novel.platform.modules.novel.mapper.NovelProcMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

/**
 * 存储过程调用封装：统一把 SIGNAL 45000 的业务错误映射为 BusinessException。
 * 过程自管事务，调用方不得再包 @Transactional。
 */
@Repository
public class NovelProcRepository {

    private static final String SIGNAL_STATE = "45000";
    private static final int SIGNAL_ERROR_CODE = 1644;

    private final NovelProcMapper procMapper;

    public NovelProcRepository(NovelProcMapper procMapper) {
        this.procMapper = procMapper;
    }

    public void createNovel(CreateNovelParam param) {
        try {
            procMapper.createNovel(param);
        } catch (RuntimeException exception) {
            throw translate(exception);
        }
    }

    public void saveChapterDraft(SaveChapterDraftParam param) {
        try {
            procMapper.saveChapterDraft(param);
        } catch (RuntimeException exception) {
            throw translate(exception);
        }
    }

    public void publishChapter(Long chapterId) {
        try {
            procMapper.publishChapter(chapterId);
        } catch (RuntimeException exception) {
            throw translate(exception);
        }
    }

    public void offlineChapter(Long chapterId) {
        try {
            procMapper.offlineChapter(chapterId);
        } catch (RuntimeException exception) {
            throw translate(exception);
        }
    }

    private RuntimeException translate(RuntimeException exception) {
        SQLException sqlException = findSqlException(exception);
        if (sqlException == null) {
            return new IllegalStateException("数据库操作失败");
        }
        String state = sqlException.getSQLState();
        int errorCode = sqlException.getErrorCode();
        if (SIGNAL_STATE.equals(state) || errorCode == SIGNAL_ERROR_CODE) {
            return toBusinessException(sqlException.getMessage());
        }
        return new IllegalStateException("数据库操作失败");
    }

    private SQLException findSqlException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                return sqlException;
            }
            current = current.getCause();
        }
        return null;
    }

    private BusinessException toBusinessException(String message) {
        String text = message == null ? "" : message;
        if (text.contains("标签")) {
            if (text.contains("重复")) {
                return new BusinessException(40905, "标签参数重复或存在数据冲突", HttpStatus.BAD_REQUEST);
            }
            return new BusinessException(40905, "标签不存在或已停用", HttpStatus.BAD_REQUEST);
        }
        if (text.contains("作者不存在") || text.contains("创作权限")) {
            return new BusinessException(40300, "无权执行该操作", HttpStatus.FORBIDDEN);
        }
        if (text.contains("章节序号冲突")) {
            return new BusinessException(40906, "章节序号冲突，请重试", HttpStatus.CONFLICT);
        }
        if (text.contains("已下架章节")) {
            return new BusinessException(40904, "已下架章节不可编辑，请新建章节", HttpStatus.CONFLICT);
        }
        if (text.contains("未通过审核")) {
            return new BusinessException(40904, "章节未通过审核，不能发布", HttpStatus.CONFLICT);
        }
        if (text.contains("重复发布") || text.contains("已发布")) {
            return new BusinessException(40904, "章节已发布，请勿重复发布", HttpStatus.CONFLICT);
        }
        if (text.contains("下架状态")) {
            return new BusinessException(40904, "章节已处于下架状态", HttpStatus.CONFLICT);
        }
        if (text.contains("章节不存在")) {
            return new BusinessException(40903, "章节不存在", HttpStatus.NOT_FOUND);
        }
        if (text.contains("小说不存在")) {
            return new BusinessException(40901, "作品不存在", HttpStatus.NOT_FOUND);
        }
        if (text.contains("VIP")) {
            return new BusinessException(40900, "VIP 价格与 VIP 标记不一致", HttpStatus.BAD_REQUEST);
        }
        if (text.contains("书名")) {
            return new BusinessException(40000, "书名不能为空", HttpStatus.BAD_REQUEST);
        }
        if (text.contains("章节标题")) {
            return new BusinessException(40000, "章节标题不能为空", HttpStatus.BAD_REQUEST);
        }
        return new BusinessException(40900, "操作未完成，请检查输入后重试", HttpStatus.CONFLICT);
    }
}
