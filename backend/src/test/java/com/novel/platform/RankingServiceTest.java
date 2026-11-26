package com.novel.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.interaction.dto.RankingConfigCreateRequest;
import com.novel.platform.modules.interaction.dto.RankingConfigResponse;
import com.novel.platform.modules.interaction.dto.RankingConfigRow;
import com.novel.platform.modules.interaction.dto.RankingDetailResponse;
import com.novel.platform.modules.interaction.dto.RankingDetailRow;
import com.novel.platform.modules.interaction.dto.RankingGenerateRequest;
import com.novel.platform.modules.interaction.dto.RankingGenerateResponse;
import com.novel.platform.modules.interaction.entity.RankingConfig;
import com.novel.platform.modules.interaction.mapper.InteractionProcedureMapper;
import com.novel.platform.modules.interaction.mapper.RankingConfigMapper;
import com.novel.platform.modules.interaction.mapper.RankingItemMapper;
import com.novel.platform.modules.interaction.mapper.param.RankingGenerateParam;
import com.novel.platform.modules.interaction.service.RankingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private RankingConfigMapper configMapper;
    @Mock
    private RankingItemMapper itemMapper;
    @Mock
    private InteractionProcedureMapper procedureMapper;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RankingService rankingService;

    private static RankingConfigRow activeRow(String code) {
        RankingConfigRow row = mock(RankingConfigRow.class);
        when(row.getIsActive()).thenReturn(1);
        when(row.getRankingCode()).thenReturn(code);
        when(row.getRankingName()).thenReturn("收藏榜");
        when(row.getRankingType()).thenReturn("FAVORITE");
        when(row.getPeriodType()).thenReturn("DAILY");
        when(row.getLatestPeriodNo()).thenReturn("20261124");
        return row;
    }

    @Test
    void shouldRejectDetailWhenCodeBlank() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> rankingService.detail("   ", null, null));

        assertThat(exception.getCode()).isEqualTo(40000);
    }

    @Test
    void shouldRejectDetailWhenConfigMissing() {
        when(configMapper.selectRowByCode("FAVORITE_DAILY")).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> rankingService.detail("FAVORITE_DAILY", null, null));

        assertThat(exception.getCode()).isEqualTo(40401);
    }

    @Test
    void shouldRejectDetailWhenInactive() {
        RankingConfigRow row = mock(RankingConfigRow.class);
        when(row.getIsActive()).thenReturn(0);
        when(configMapper.selectRowByCode("FAVORITE_DAILY")).thenReturn(row);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> rankingService.detail("FAVORITE_DAILY", null, null));

        assertThat(exception.getCode()).isEqualTo(40401);
        verify(procedureMapper, never()).getRanking(any(), any(), any());
    }

    @Test
    void shouldReturnDetailWithRowsAndActualPeriod() {
        RankingConfigRow row = activeRow("FAVORITE_DAILY");
        when(configMapper.selectRowByCode("FAVORITE_DAILY")).thenReturn(row);
        RankingDetailRow detailRow = mock(RankingDetailRow.class);
        when(detailRow.getPeriodNo()).thenReturn("20261125");
        when(detailRow.getRankNo()).thenReturn(1);
        when(detailRow.getNovelId()).thenReturn(301L);
        when(detailRow.getNovelTitle()).thenReturn("作品A");
        when(procedureMapper.getRanking(eq("FAVORITE_DAILY"), any(), eq(100)))
                .thenReturn(List.of(detailRow));

        RankingDetailResponse response = rankingService.detail("FAVORITE_DAILY", null, null);

        assertThat(response.rankingCode()).isEqualTo("FAVORITE_DAILY");
        assertThat(response.periodNo()).isEqualTo("20261125");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).novelId()).isEqualTo("301");
    }

    @Test
    void shouldUseLatestPeriodWhenNoneSpecifiedAndRowsEmpty() {
        RankingConfigRow row = activeRow("FAVORITE_DAILY");
        when(configMapper.selectRowByCode("FAVORITE_DAILY")).thenReturn(row);
        when(procedureMapper.getRanking(eq("FAVORITE_DAILY"), isNull(), eq(100)))
                .thenReturn(List.of());

        RankingDetailResponse response = rankingService.detail("FAVORITE_DAILY", null, null);

        assertThat(response.periodNo()).isEqualTo("20261124");
        assertThat(response.items()).isEmpty();
    }

    @Test
    void shouldClampDetailLimitToMaximum() {
        RankingConfigRow row = activeRow("FAVORITE_DAILY");
        when(configMapper.selectRowByCode("FAVORITE_DAILY")).thenReturn(row);
        when(procedureMapper.getRanking(any(), any(), any())).thenReturn(List.of());

        rankingService.detail("FAVORITE_DAILY", null, 5000);

        verify(procedureMapper).getRanking(eq("FAVORITE_DAILY"), any(), eq(1000));
    }

    @Test
    void shouldRejectCreateWhenCodeDuplicate() {
        RankingConfigRow row = activeRow("FAVORITE_DAILY");
        when(configMapper.selectRowByCode("FAVORITE_DAILY")).thenReturn(row);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> rankingService.create(new RankingConfigCreateRequest(
                        "FAVORITE_DAILY", "收藏榜", "FAVORITE", "DAILY", 50, null, true)));

        assertThat(exception.getCode()).isEqualTo(40900);
        verify(configMapper, never()).insert(any(RankingConfig.class));
    }

    @Test
    void shouldCreateConfigSuccessfully() {
        RankingConfigRow created = activeRow("FAVORITE_DAILY");
        when(configMapper.selectRowByCode("FAVORITE_DAILY"))
                .thenReturn(null, created);

        RankingConfigResponse response = rankingService.create(new RankingConfigCreateRequest(
                "  FAVORITE_DAILY  ", "  收藏榜  ", "FAVORITE", "DAILY", 50, null, true));

        verify(configMapper).insert(any(RankingConfig.class));
        assertThat(response.rankingCode()).isEqualTo("FAVORITE_DAILY");
    }

    @Test
    void shouldRejectGenerateWhenConfigMissing() {
        when(configMapper.selectById(88L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> rankingService.generate(88L, new RankingGenerateRequest("20261125", false)));

        assertThat(exception.getCode()).isEqualTo(40401);
    }

    @Test
    void shouldRejectGenerateWhenInactive() {
        RankingConfig config = mock(RankingConfig.class);
        when(config.getIsActive()).thenReturn(0);
        when(configMapper.selectById(88L)).thenReturn(config);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> rankingService.generate(88L, new RankingGenerateRequest("20261125", false)));

        assertThat(exception.getCode()).isEqualTo(40001);
        verify(procedureMapper, never()).generateRanking(any());
    }

    @Test
    void shouldGenerateRankingSuccessfully() {
        RankingConfig config = mock(RankingConfig.class);
        when(config.getIsActive()).thenReturn(1);
        when(configMapper.selectById(88L)).thenReturn(config);
        doAnswer(invocation -> {
            RankingGenerateParam param = invocation.getArgument(0);
            param.setItemCount(25);
            return null;
        }).when(procedureMapper).generateRanking(any(RankingGenerateParam.class));

        RankingGenerateResponse response = rankingService.generate(
                88L, new RankingGenerateRequest(" 20261125 ", false));

        assertThat(response.rankingId()).isEqualTo("88");
        assertThat(response.periodNo()).isEqualTo("20261125");
        assertThat(response.itemCount()).isEqualTo(25);
    }
}
