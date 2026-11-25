package com.novel.platform;

import com.novel.platform.common.security.AuthUser;
import com.novel.platform.common.security.UserContext;
import com.novel.platform.modules.novel.dto.ChapterReadResponse;
import com.novel.platform.modules.novel.dto.ChapterRow;
import com.novel.platform.modules.novel.mapper.ChapterMapper;
import com.novel.platform.modules.novel.mapper.NovelMapper;
import com.novel.platform.modules.novel.mapper.NovelTagMapper;
import com.novel.platform.modules.novel.mapper.TagMapper;
import com.novel.platform.modules.novel.service.NovelQueryService;
import com.novel.platform.modules.user.service.ChapterEntitlementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NovelQueryServiceTest {

    @Mock
    private NovelMapper novelMapper;
    @Mock
    private ChapterMapper chapterMapper;
    @Mock
    private TagMapper tagMapper;
    @Mock
    private NovelTagMapper novelTagMapper;
    @Mock
    private ChapterEntitlementService entitlementService;
    @InjectMocks
    private NovelQueryService queryService;

    @BeforeEach
    void setUp() {
        UserContext.clear();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void shouldReadFreeChapterWithoutSubscription() {
        when(chapterMapper.selectChapterForRead(501L)).thenReturn(chapter(0));

        ChapterReadResponse response = queryService.readChapter("501");

        assertTrue(response.canRead());
        assertEquals("章节正文", response.content());
        verifyNoInteractions(entitlementService);
    }

    @Test
    void shouldHideVipContentFromGuest() {
        when(chapterMapper.selectChapterForRead(501L)).thenReturn(chapter(1));

        ChapterReadResponse response = queryService.readChapter("501");

        assertFalse(response.canRead());
        assertNull(response.content());
    }

    @Test
    void shouldHideVipContentFromUnsubscribedUser() {
        UserContext.set(new AuthUser(101L, "reader", "READER"));
        when(chapterMapper.selectChapterForRead(501L)).thenReturn(chapter(1));
        when(entitlementService.hasActiveSubscription(101L, 501L)).thenReturn(false);

        ChapterReadResponse response = queryService.readChapter("501");

        assertFalse(response.canRead());
        assertNull(response.content());
    }

    @Test
    void shouldReturnVipContentForActiveSubscription() {
        UserContext.set(new AuthUser(101L, "reader", "READER"));
        when(chapterMapper.selectChapterForRead(501L)).thenReturn(chapter(1));
        when(entitlementService.hasActiveSubscription(101L, 501L)).thenReturn(true);

        ChapterReadResponse response = queryService.readChapter("501");

        assertTrue(response.canRead());
        assertEquals("章节正文", response.content());
    }

    private ChapterRow chapter(int isVip) {
        ChapterRow row = new ChapterRow();
        row.setChapterId(501L);
        row.setNovelId(301L);
        row.setNovelTitle("测试作品");
        row.setChapterNo(1);
        row.setTitle("第一章");
        row.setContent("章节正文");
        row.setIsVip(isVip);
        row.setVipPrice(isVip == 1 ? new BigDecimal("2.50") : null);
        row.setWordCount(4);
        return row;
    }
}
