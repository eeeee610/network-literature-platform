package com.novel.platform;

import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.interaction.dto.CommentAuditRequest;
import com.novel.platform.modules.interaction.dto.CommentCreateRequest;
import com.novel.platform.modules.interaction.dto.CommentCreateResponse;
import com.novel.platform.modules.interaction.dto.NovelBrief;
import com.novel.platform.modules.interaction.entity.Comment;
import com.novel.platform.modules.interaction.mapper.AuditLogMapper;
import com.novel.platform.modules.interaction.mapper.CommentLikeMapper;
import com.novel.platform.modules.interaction.mapper.CommentMapper;
import com.novel.platform.modules.interaction.mapper.InteractionProcedureMapper;
import com.novel.platform.modules.interaction.mapper.NovelLookupMapper;
import com.novel.platform.modules.interaction.mapper.param.CommentCreateParam;
import com.novel.platform.modules.interaction.service.CommentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentMapper commentMapper;
    @Mock
    private CommentLikeMapper commentLikeMapper;
    @Mock
    private InteractionProcedureMapper procedureMapper;
    @Mock
    private AuditLogMapper auditLogMapper;
    @Mock
    private NovelLookupMapper novelLookupMapper;

    @InjectMocks
    private CommentService commentService;

    private static NovelBrief brief(Integer status) {
        NovelBrief brief = new NovelBrief();
        brief.setNovelId(101L);
        brief.setTitle("测试作品");
        brief.setStatus(status);
        return brief;
    }

    private static Comment comment(long userId, Integer status, Integer auditStatus, Long parentId) {
        Comment comment = mock(Comment.class);
        when(comment.getUserId()).thenReturn(userId);
        when(comment.getStatus()).thenReturn(status);
        when(comment.getAuditStatus()).thenReturn(auditStatus);
        when(comment.getParentId()).thenReturn(parentId);
        return comment;
    }

    @Test
    void shouldRejectCreateWhenNovelNotFound() {
        when(novelLookupMapper.selectBrief(101L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> commentService.create(new CommentCreateRequest("101", null, null, "你好"), 101L));

        assertThat(exception.getCode()).isEqualTo(40401);
    }

    @Test
    void shouldRejectCreateWhenNovelOffline() {
        when(novelLookupMapper.selectBrief(101L)).thenReturn(brief(2));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> commentService.create(new CommentCreateRequest("101", null, null, "你好"), 101L));

        assertThat(exception.getCode()).isEqualTo(40001);
    }

    @Test
    void shouldRejectCreateWhenParentNotFound() {
        when(novelLookupMapper.selectBrief(101L)).thenReturn(brief(1));
        when(commentMapper.selectById(200L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> commentService.create(new CommentCreateRequest("101", null, "200", "回复"), 101L));

        assertThat(exception.getCode()).isEqualTo(40001);
        verify(procedureMapper, never()).createComment(any());
    }

    @Test
    void shouldRejectCreateWhenReplyNested() {
        when(novelLookupMapper.selectBrief(101L)).thenReturn(brief(1));
        Comment parent = comment(303L, 1, 1, 150L);
        when(commentMapper.selectById(200L)).thenReturn(parent);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> commentService.create(new CommentCreateRequest("101", null, "200", "嵌套回复"), 101L));

        assertThat(exception.getCode()).isEqualTo(40001);
    }

    @Test
    void shouldRejectCreateWhenParentNotVisible() {
        when(novelLookupMapper.selectBrief(101L)).thenReturn(brief(1));
        Comment parent = comment(303L, 0, 1, null);
        when(commentMapper.selectById(200L)).thenReturn(parent);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> commentService.create(new CommentCreateRequest("101", null, "200", "回复"), 101L));

        assertThat(exception.getCode()).isEqualTo(40001);
    }

    @Test
    void shouldCreateTopCommentSuccessfully() {
        when(novelLookupMapper.selectBrief(101L)).thenReturn(brief(1));
        doAnswer(invocation -> {
            CommentCreateParam param = invocation.getArgument(0);
            param.setCommentId(9001L);
            return null;
        }).when(procedureMapper).createComment(any(CommentCreateParam.class));

        CommentCreateResponse response = commentService.create(
                new CommentCreateRequest("101", null, null, "  你好世界  "), 101L);

        assertThat(response.commentId()).isEqualTo("9001");
        assertThat(response.auditStatus()).isZero();
        verify(procedureMapper).createComment(any(CommentCreateParam.class));
    }

    @Test
    void shouldRejectCreateWhenProcedureReturnsNullCommentId() {
        when(novelLookupMapper.selectBrief(101L)).thenReturn(brief(1));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> commentService.create(new CommentCreateRequest("101", null, null, "你好"), 101L));

        assertThat(exception.getCode()).isEqualTo(50000);
    }

    @Test
    void shouldRejectDeleteWhenCommentNotFound() {
        when(commentMapper.selectById(901L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> commentService.delete(901L, 101L));

        assertThat(exception.getCode()).isEqualTo(40401);
    }

    @Test
    void shouldRejectDeleteWhenNotOwner() {
        Comment existing = comment(202L, 1, 1, null);
        when(commentMapper.selectById(901L)).thenReturn(existing);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> commentService.delete(901L, 101L));

        assertThat(exception.getCode()).isEqualTo(40300);
        verify(procedureMapper, never()).setCommentStatus(any(), any());
    }

    @Test
    void shouldSkipUpdateWhenCommentAlreadyDeleted() {
        Comment deleted = comment(101L, 3, 1, null);
        when(commentMapper.selectById(901L)).thenReturn(deleted);

        commentService.delete(901L, 101L);

        verify(procedureMapper, never()).setCommentStatus(any(), any());
    }

    @Test
    void shouldRejectLikeWhenCommentInvisible() {
        when(commentMapper.countVisible(901L)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> commentService.like(901L, 101L));

        assertThat(exception.getCode()).isEqualTo(40401);
        verify(commentLikeMapper, never()).insertIgnore(any(), any());
    }

    @Test
    void shouldLikeVisibleComment() {
        when(commentMapper.countVisible(901L)).thenReturn(1);

        commentService.like(901L, 101L);

        verify(commentLikeMapper).insertIgnore(901L, 101L);
    }

    @Test
    void shouldRejectAuditWhenRejectWithoutReason() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> commentService.audit(901L, new CommentAuditRequest(2, "   "), 7001L));

        assertThat(exception.getCode()).isEqualTo(40000);
        verify(commentMapper, never()).selectById(any());
    }

    @Test
    void shouldAuditCommentSuccessfully() {
        Comment pending = comment(101L, 1, 0, null);
        when(commentMapper.selectById(901L)).thenReturn(pending);

        commentService.audit(901L, new CommentAuditRequest(1, null), 7001L);

        verify(procedureMapper).auditComment(901L, 1);
        verify(auditLogMapper).insert(7001L, "COMMENT", 901L, 1, null);
    }
}
