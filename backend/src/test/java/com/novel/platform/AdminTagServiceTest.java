package com.novel.platform;

import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.admin.dto.TagRequest;
import com.novel.platform.modules.admin.mapper.AdminTagMapper;
import com.novel.platform.modules.admin.service.AdminTagService;
import com.novel.platform.modules.novel.entity.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTagServiceTest {

    @Mock
    private AdminTagMapper tagMapper;

    @InjectMocks
    private AdminTagService tagService;

    @Test
    void shouldConvertDuplicateNameToConflict() {
        when(tagMapper.insert(any(Tag.class))).thenThrow(new DuplicateKeyException("duplicate"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> tagService.create(new TagRequest("科幻", 1)));

        assertEquals(40901, exception.getCode());
    }

    @Test
    void shouldNotDeleteReferencedTag() {
        Tag tag = new Tag();
        tag.setTagId(3L);
        tag.setTagName("科幻");
        when(tagMapper.selectById(3L)).thenReturn(tag);
        when(tagMapper.countNovelReferences(3L)).thenReturn(2L);

        BusinessException exception = assertThrows(BusinessException.class, () -> tagService.delete(3L));

        assertEquals(40902, exception.getCode());
        verify(tagMapper, never()).deleteById(3L);
    }
}
