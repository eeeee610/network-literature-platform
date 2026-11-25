package com.novel.platform;

import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.admin.dto.ContentAuditRequest;
import com.novel.platform.modules.admin.mapper.AdminContentAuditMapper;
import com.novel.platform.modules.admin.service.AdminContentAuditService;
import com.novel.platform.modules.interaction.mapper.AuditLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminContentAuditServiceTest {

    @Mock
    private AdminContentAuditMapper auditMapper;

    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private AdminContentAuditService auditService;

    @Test
    void shouldRequireReasonWhenRejecting() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> auditService.auditNovel(11L, new ContentAuditRequest(2, " "), 7L));

        assertEquals(40000, exception.getCode());
        verifyNoInteractions(auditMapper, auditLogMapper);
    }

    @Test
    void shouldUpdateNovelAndAppendLog() {
        when(auditMapper.countNovelById(11L)).thenReturn(1);

        auditService.auditNovel(11L, new ContentAuditRequest(1, null), 7L);

        verify(auditMapper).updateNovelAuditStatus(11L, 1);
        verify(auditLogMapper).insert(7L, "NOVEL", 11L, 1, null);
    }

    @Test
    void shouldUpdateChapterAndAppendLogWithoutPublishing() {
        when(auditMapper.countChapterById(21L)).thenReturn(1);

        auditService.auditChapter(21L, new ContentAuditRequest(2, "正文需要修改"), 7L);

        verify(auditMapper).updateChapterAuditStatus(21L, 2);
        verify(auditLogMapper).insert(7L, "CHAPTER", 21L, 2, "正文需要修改");
    }
}
