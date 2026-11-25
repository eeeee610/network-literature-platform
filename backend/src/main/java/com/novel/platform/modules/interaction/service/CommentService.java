package com.novel.platform.modules.interaction.service;

import com.novel.platform.common.api.PageResult;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.common.util.IdUtils;
import com.novel.platform.modules.interaction.dto.AdminCommentItem;
import com.novel.platform.modules.interaction.dto.CommentAuditRequest;
import com.novel.platform.modules.interaction.dto.CommentCreateRequest;
import com.novel.platform.modules.interaction.dto.CommentCreateResponse;
import com.novel.platform.modules.interaction.dto.CommentItem;
import com.novel.platform.modules.interaction.dto.CommentRow;
import com.novel.platform.modules.interaction.dto.CommentStatusRequest;
import com.novel.platform.modules.interaction.dto.NovelBrief;
import com.novel.platform.modules.interaction.dto.UserBrief;
import com.novel.platform.modules.interaction.entity.Comment;
import com.novel.platform.modules.interaction.mapper.AuditLogMapper;
import com.novel.platform.modules.interaction.mapper.CommentLikeMapper;
import com.novel.platform.modules.interaction.mapper.CommentMapper;
import com.novel.platform.modules.interaction.mapper.InteractionProcedureMapper;
import com.novel.platform.modules.interaction.mapper.NovelLookupMapper;
import com.novel.platform.modules.interaction.mapper.param.CommentCreateParam;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final InteractionProcedureMapper procedureMapper;
    private final AuditLogMapper auditLogMapper;
    private final NovelLookupMapper novelLookupMapper;

    public CommentService(CommentMapper commentMapper, CommentLikeMapper commentLikeMapper,
                          InteractionProcedureMapper procedureMapper, AuditLogMapper auditLogMapper,
                          NovelLookupMapper novelLookupMapper) {
        this.commentMapper = commentMapper;
        this.commentLikeMapper = commentLikeMapper;
        this.procedureMapper = procedureMapper;
        this.auditLogMapper = auditLogMapper;
        this.novelLookupMapper = novelLookupMapper;
    }

    public PageResult<CommentItem> list(String novelIdValue, String chapterIdValue,
                                        int pageParam, int pageSizeParam, Long viewerUserId) {
        Long novelId = IdUtils.parse(novelIdValue, "novelId");
        Long chapterId = IdUtils.parseNullable(chapterIdValue, "chapterId");
        int page = Pagination.page(pageParam);
        int pageSize = Pagination.size(pageSizeParam);

        List<CommentRow> topRows = commentMapper.selectTopComments(
                novelId, chapterId, viewerUserId, Pagination.offset(page, pageSize), pageSize);
        long total = commentMapper.countTopComments(novelId, chapterId);

        Map<Long, List<CommentRow>> repliesByParent = new HashMap<>();
        if (!topRows.isEmpty()) {
            List<Long> parentIds = topRows.stream().map(CommentRow::getCommentId).toList();
            List<CommentRow> replyRows = commentMapper.selectReplies(parentIds, viewerUserId);
            repliesByParent = replyRows.stream()
                    .collect(Collectors.groupingBy(CommentRow::getParentId));
        }

        List<CommentItem> items = new ArrayList<>(topRows.size());
        for (CommentRow row : topRows) {
            List<CommentRow> replies = repliesByParent.getOrDefault(row.getCommentId(), List.of());
            items.add(toItem(row, replies));
        }
        return new PageResult<>(items, total, page, pageSize);
    }

    @Transactional
    public CommentCreateResponse create(CommentCreateRequest request, Long userId) {
        Long novelId = IdUtils.parse(request.novelId(), "novelId");
        Long chapterId = IdUtils.parseNullable(request.chapterId(), "chapterId");
        Long parentId = IdUtils.parseNullable(request.parentId(), "parentId");

        NovelBrief novel = novelLookupMapper.selectBrief(novelId);
        if (novel == null) {
            throw new BusinessException(40401, "作品不存在", HttpStatus.NOT_FOUND);
        }
        if (novel.getStatus() != null && novel.getStatus() == 2) {
            throw new BusinessException(40001, "作品已下架，不能评论");
        }

        Long replyToUserId = null;
        if (parentId != null) {
            Comment parent = commentMapper.selectById(parentId);
            if (parent == null) {
                throw new BusinessException(40001, "父评论不存在");
            }
            if (parent.getParentId() != null) {
                throw new BusinessException(40001, "仅允许一级评论和直接回复，不允许继续嵌套");
            }
            if (parent.getStatus() != 1 || parent.getAuditStatus() != 1) {
                throw new BusinessException(40001, "父评论不可回复");
            }
            replyToUserId = parent.getUserId();
        }

        CommentCreateParam param = new CommentCreateParam();
        param.setNovelId(novelId);
        param.setChapterId(chapterId);
        param.setUserId(userId);
        param.setParentId(parentId);
        param.setReplyToUserId(replyToUserId);
        param.setContent(request.content().trim());

        try {
            procedureMapper.createComment(param);
        } catch (DataAccessException exception) {
            BusinessException businessException = ProcedureErrors.toBusiness(exception);
            if (businessException != null) {
                throw businessException;
            }
            throw exception;
        }

        if (param.getCommentId() == null) {
            throw new BusinessException(50000, "评论保存失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new CommentCreateResponse(String.valueOf(param.getCommentId()), 0);
    }

    @Transactional
    public void delete(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(40401, "评论不存在", HttpStatus.NOT_FOUND);
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(40300, "只能删除自己的评论", HttpStatus.FORBIDDEN);
        }
        if (comment.getStatus() == 3) {
            return;
        }
        callSetStatus(commentId, 3);
    }

    @Transactional
    public void like(Long commentId, Long userId) {
        if (commentMapper.countVisible(commentId) == 0) {
            throw new BusinessException(40401, "评论不存在或不可见", HttpStatus.NOT_FOUND);
        }
        commentLikeMapper.insertIgnore(commentId, userId);
    }

    @Transactional
    public void unlike(Long commentId, Long userId) {
        commentLikeMapper.deleteLike(commentId, userId);
    }

    public PageResult<AdminCommentItem> adminList(String novelIdValue, Integer status, Integer auditStatus,
                                                  String keyword, int pageParam, int pageSizeParam) {
        Long novelId = IdUtils.parseNullable(novelIdValue, "novelId");
        int page = Pagination.page(pageParam);
        int pageSize = Pagination.size(pageSizeParam);
        String normalizedKeyword = keyword == null ? null : keyword.trim();

        List<CommentRow> rows = commentMapper.selectAdminComments(
                novelId, status, auditStatus, normalizedKeyword,
                Pagination.offset(page, pageSize), pageSize);
        long total = commentMapper.countAdminComments(novelId, status, auditStatus, normalizedKeyword);

        List<AdminCommentItem> items = rows.stream().map(row -> new AdminCommentItem(
                String.valueOf(row.getCommentId()),
                String.valueOf(row.getNovelId()),
                row.getNovelTitle(),
                toId(row.getChapterId()),
                String.valueOf(row.getUserId()),
                row.getNickname(),
                toId(row.getParentId()),
                row.getContent(),
                nvl(row.getLikeCount()),
                nvl(row.getReplyCount()),
                nvl(row.getStatus()),
                nvl(row.getAuditStatus()),
                row.getCreatedAt()
        )).toList();
        return new PageResult<>(items, total, page, pageSize);
    }

    @Transactional
    public void audit(Long commentId, CommentAuditRequest request, Long adminId) {
        if (request.action() == 2 && (request.reason() == null || request.reason().isBlank())) {
            throw new BusinessException(40000, "驳回时必须填写审核原因");
        }
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(40401, "评论不存在", HttpStatus.NOT_FOUND);
        }
        try {
            procedureMapper.auditComment(commentId, request.action());
        } catch (DataAccessException exception) {
            BusinessException businessException = ProcedureErrors.toBusiness(exception);
            if (businessException != null) {
                throw businessException;
            }
            throw exception;
        }
        auditLogMapper.insert(adminId, "COMMENT", commentId, request.action(), trimToNull(request.reason()));
    }

    @Transactional
    public void changeStatus(Long commentId, CommentStatusRequest request) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(40401, "评论不存在", HttpStatus.NOT_FOUND);
        }
        if (comment.getStatus().equals(request.status())) {
            return;
        }
        callSetStatus(commentId, request.status());
    }

    private void callSetStatus(Long commentId, Integer status) {
        try {
            procedureMapper.setCommentStatus(commentId, status);
        } catch (DataAccessException exception) {
            BusinessException businessException = ProcedureErrors.toBusiness(exception);
            if (businessException != null) {
                throw businessException;
            }
            throw exception;
        }
    }

    private CommentItem toItem(CommentRow row, List<CommentRow> replies) {
        List<CommentItem> replyItems = replies.stream()
                .map(reply -> toItem(reply, List.of()))
                .toList();
        return new CommentItem(
                String.valueOf(row.getCommentId()),
                String.valueOf(row.getNovelId()),
                toId(row.getChapterId()),
                toId(row.getParentId()),
                toId(row.getReplyToUserId()),
                row.getReplyToNickname(),
                row.getContent(),
                nvl(row.getLikeCount()),
                nvl(row.getReplyCount()),
                row.getIsTop() != null && row.getIsTop() == 1,
                row.getLiked() != null && row.getLiked() > 0,
                row.getCreatedAt(),
                new UserBrief(String.valueOf(row.getUserId()), row.getNickname(), row.getAvatarUrl()),
                replyItems
        );
    }

    private static String toId(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
