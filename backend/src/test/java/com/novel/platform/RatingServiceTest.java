package com.novel.platform;

import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.interaction.dto.NovelBrief;
import com.novel.platform.modules.interaction.dto.RatingAuditRequest;
import com.novel.platform.modules.interaction.dto.RatingSummaryResponse;
import com.novel.platform.modules.interaction.dto.RatingUpsertRequest;
import com.novel.platform.modules.interaction.entity.NovelRating;
import com.novel.platform.modules.interaction.entity.NovelRatingStat;
import com.novel.platform.modules.interaction.mapper.AuditLogMapper;
import com.novel.platform.modules.interaction.mapper.NovelLookupMapper;
import com.novel.platform.modules.interaction.mapper.NovelRatingMapper;
import com.novel.platform.modules.interaction.mapper.NovelRatingStatMapper;
import com.novel.platform.modules.interaction.service.RatingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private NovelRatingMapper ratingMapper;
    @Mock
    private NovelRatingStatMapper statMapper;
    @Mock
    private NovelLookupMapper novelLookupMapper;
    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private RatingService ratingService;

    private static NovelBrief brief(Integer status) {
        NovelBrief brief = new NovelBrief();
        brief.setNovelId(101L);
        brief.setTitle("测试作品");
        brief.setStatus(status);
        return brief;
    }

    @Test
    void shouldRejectSummaryWhenNovelNotFound() {
        when(novelLookupMapper.selectBrief(101L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> ratingService.summary("101", null));

        assertThat(exception.getCode()).isEqualTo(40401);
    }

    @Test
    void shouldReturnZerosWhenStatMissingAndGuestViewing() {
        when(novelLookupMapper.selectBrief(101L)).thenReturn(brief(1));
        when(statMapper.selectById(101L)).thenReturn(null);

        RatingSummaryResponse response = ratingService.summary("101", null);

        assertThat(response.novelTitle()).isEqualTo("测试作品");
        assertThat(response.ratingCount()).isZero();
        assertThat(response.scoreTotal()).isZero();
        assertThat(response.avgScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.myScore()).isNull();
    }

    @Test
    void shouldIncludeViewerRatingAndStat() {
        NovelRatingStat stat = new NovelRatingStat();
        stat.setRatingCount(12);
        stat.setScoreTotal(54L);
        stat.setAvgScore(new BigDecimal("4.50"));
        NovelRating mine = mock(NovelRating.class);
        when(mine.getScore()).thenReturn(5);
        when(mine.getReviewContent()).thenReturn("很棒");
        when(mine.getReviewAuditStatus()).thenReturn(1);

        when(novelLookupMapper.selectBrief(101L)).thenReturn(brief(1));
        when(statMapper.selectById(101L)).thenReturn(stat);
        when(ratingMapper.selectOne(any())).thenReturn(mine);

        RatingSummaryResponse response = ratingService.summary("101", 101L);

        assertThat(response.ratingCount()).isEqualTo(12);
        assertThat(response.avgScore()).isEqualByComparingTo("4.50");
        assertThat(response.myScore()).isEqualTo(5);
        assertThat(response.myReviewContent()).isEqualTo("很棒");
    }

    @Test
    void shouldRejectSaveWhenNovelNotFound() {
        when(novelLookupMapper.selectBrief(101L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> ratingService.save(new RatingUpsertRequest("101", 5, "好看"), 101L));

        assertThat(exception.getCode()).isEqualTo(40401);
    }

    @Test
    void shouldRejectSaveWhenNovelOffline() {
        when(novelLookupMapper.selectBrief(101L)).thenReturn(brief(2));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> ratingService.save(new RatingUpsertRequest("101", 5, "好看"), 101L));

        assertThat(exception.getCode()).isEqualTo(40001);
    }

    @Test
    void shouldInsertRatingWhenNoneExists() {
        when(novelLookupMapper.selectBrief(101L)).thenReturn(brief(1));
        when(ratingMapper.selectOne(any())).thenReturn(null);
        when(statMapper.selectById(101L)).thenReturn(null);

        RatingSummaryResponse response = ratingService.save(
                new RatingUpsertRequest("101", 5, "好看"), 101L);

        verify(ratingMapper).insert(any(NovelRating.class));
        assertThat(response.novelId()).isEqualTo("101");
    }

    @Test
    void shouldUpdateRatingWhenAlreadyExists() {
        NovelRating existing = mock(NovelRating.class);
        when(novelLookupMapper.selectBrief(101L)).thenReturn(brief(1));
        when(ratingMapper.selectOne(any())).thenReturn(existing);
        when(statMapper.selectById(101L)).thenReturn(null);

        ratingService.save(new RatingUpsertRequest("101", 4, "还行"), 101L);

        verify(ratingMapper).updateById(existing);
        verify(ratingMapper, never()).insert(any(NovelRating.class));
    }

    @Test
    void shouldRejectAuditReviewWhenRejectWithoutReason() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> ratingService.auditReview(501L, new RatingAuditRequest(2, "  "), 7001L));

        assertThat(exception.getCode()).isEqualTo(40000);
        verify(ratingMapper, never()).selectById(any());
    }

    @Test
    void shouldRejectAuditReviewWhenRatingNotFound() {
        when(ratingMapper.selectById(501L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> ratingService.auditReview(501L, new RatingAuditRequest(1, null), 7001L));

        assertThat(exception.getCode()).isEqualTo(40401);
    }

    @Test
    void shouldRejectAuditReviewWhenNoReviewContent() {
        NovelRating rating = mock(NovelRating.class);
        when(rating.getReviewContent()).thenReturn("   ");
        when(ratingMapper.selectById(501L)).thenReturn(rating);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> ratingService.auditReview(501L, new RatingAuditRequest(1, null), 7001L));

        assertThat(exception.getCode()).isEqualTo(40001);
        verify(ratingMapper, never()).update(any(), any());
    }

    @Test
    void shouldAuditReviewSuccessfully() {
        NovelRating rating = mock(NovelRating.class);
        when(rating.getReviewContent()).thenReturn("短评内容");
        when(ratingMapper.selectById(501L)).thenReturn(rating);

        ratingService.auditReview(501L, new RatingAuditRequest(1, null), 7001L);

        verify(ratingMapper).update(any(), any());
        verify(auditLogMapper).insert(7001L, "RATING", 501L, 1, null);
    }
}
