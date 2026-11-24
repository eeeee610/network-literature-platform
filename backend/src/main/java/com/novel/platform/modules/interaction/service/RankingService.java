package com.novel.platform.modules.interaction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.interaction.dto.RankingConfigCreateRequest;
import com.novel.platform.modules.interaction.dto.RankingConfigResponse;
import com.novel.platform.modules.interaction.dto.RankingConfigRow;
import com.novel.platform.modules.interaction.dto.RankingConfigUpdateRequest;
import com.novel.platform.modules.interaction.dto.RankingDetailItem;
import com.novel.platform.modules.interaction.dto.RankingDetailResponse;
import com.novel.platform.modules.interaction.dto.RankingDetailRow;
import com.novel.platform.modules.interaction.dto.RankingGenerateRequest;
import com.novel.platform.modules.interaction.dto.RankingGenerateResponse;
import com.novel.platform.modules.interaction.dto.RankingPeriodItem;
import com.novel.platform.modules.interaction.dto.RankingPeriodRow;
import com.novel.platform.modules.interaction.entity.RankingConfig;
import com.novel.platform.modules.interaction.mapper.InteractionProcedureMapper;
import com.novel.platform.modules.interaction.mapper.RankingConfigMapper;
import com.novel.platform.modules.interaction.mapper.RankingItemMapper;
import com.novel.platform.modules.interaction.mapper.param.RankingGenerateParam;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class RankingService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 1000;

    private final RankingConfigMapper configMapper;
    private final RankingItemMapper itemMapper;
    private final InteractionProcedureMapper procedureMapper;
    private final ObjectMapper objectMapper;

    public RankingService(RankingConfigMapper configMapper, RankingItemMapper itemMapper,
                          InteractionProcedureMapper procedureMapper, ObjectMapper objectMapper) {
        this.configMapper = configMapper;
        this.itemMapper = itemMapper;
        this.procedureMapper = procedureMapper;
        this.objectMapper = objectMapper;
    }

    public List<RankingConfigResponse> listActive() {
        return configMapper.selectActiveWithLatest().stream().map(this::toConfig).toList();
    }

    public List<RankingConfigResponse> listAll() {
        return configMapper.selectAllWithLatest().stream().map(this::toConfig).toList();
    }

    public RankingDetailResponse detail(String rankingCode, String periodNo, Integer limitParam) {
        String code = requireCode(rankingCode);
        RankingConfigRow config = configMapper.selectRowByCode(code);
        if (config == null || config.getIsActive() == null || config.getIsActive() != 1) {
            throw new BusinessException(40401, "榜单不存在或已停用", HttpStatus.NOT_FOUND);
        }

        int limit = limitParam == null ? DEFAULT_LIMIT : Math.min(Math.max(limitParam, 1), MAX_LIMIT);
        List<RankingDetailRow> rows;
        try {
            rows = procedureMapper.getRanking(code, trimToNull(periodNo), limit);
        } catch (DataAccessException exception) {
            BusinessException businessException = ProcedureErrors.toBusiness(exception);
            if (businessException != null) {
                throw businessException;
            }
            throw exception;
        }

        String actualPeriod = trimToNull(periodNo);
        if (!rows.isEmpty() && rows.get(0).getPeriodNo() != null) {
            actualPeriod = rows.get(0).getPeriodNo();
        }
        if (actualPeriod == null) {
            actualPeriod = config.getLatestPeriodNo();
        }
        List<RankingDetailItem> items = rows.stream().map(this::toDetailItem).toList();
        return new RankingDetailResponse(
                config.getRankingCode(),
                config.getRankingName(),
                config.getRankingType(),
                config.getPeriodType(),
                actualPeriod,
                items
        );
    }

    public List<RankingPeriodItem> periods(String rankingCode, Integer limitParam) {
        String code = requireCode(rankingCode);
        RankingConfigRow config = configMapper.selectRowByCode(code);
        if (config == null) {
            throw new BusinessException(40401, "榜单不存在", HttpStatus.NOT_FOUND);
        }
        int limit = limitParam == null ? 20 : Math.min(Math.max(limitParam, 1), 50);
        List<RankingPeriodRow> rows = itemMapper.selectPeriods(code, limit);
        return rows.stream().map(row -> new RankingPeriodItem(
                row.getPeriodNo(),
                row.getItemCount() == null ? 0L : row.getItemCount(),
                row.getCreatedAt()
        )).toList();
    }

    @Transactional
    public RankingConfigResponse create(RankingConfigCreateRequest request) {
        String code = request.rankingCode().trim();
        if (configMapper.selectRowByCode(code) != null) {
            throw new BusinessException(40900, "榜单编码已存在", HttpStatus.CONFLICT);
        }

        RankingConfig config = new RankingConfig();
        config.setRankingCode(code);
        config.setRankingName(request.rankingName().trim());
        config.setRankingType(request.rankingType());
        config.setPeriodType(request.periodType());
        config.setTopN(request.topN());
        config.setWeightConfig(writeWeight(request.weightConfig()));
        config.setIsActive(Boolean.FALSE.equals(request.isActive()) ? 0 : 1);
        configMapper.insert(config);

        return toConfig(configMapper.selectRowByCode(code));
    }

    @Transactional
    public RankingConfigResponse update(Long rankingId, RankingConfigUpdateRequest request) {
        RankingConfig config = configMapper.selectById(rankingId);
        if (config == null) {
            throw new BusinessException(40401, "榜单不存在", HttpStatus.NOT_FOUND);
        }
        config.setRankingName(request.rankingName().trim());
        config.setTopN(request.topN());
        config.setWeightConfig(writeWeight(request.weightConfig()));
        if (request.isActive() != null) {
            config.setIsActive(request.isActive() ? 1 : 0);
        }
        configMapper.updateById(config);

        return toConfig(configMapper.selectRowByCode(config.getRankingCode()));
    }

    @Transactional
    public RankingGenerateResponse generate(Long rankingId, RankingGenerateRequest request) {
        RankingConfig config = configMapper.selectById(rankingId);
        if (config == null) {
            throw new BusinessException(40401, "榜单不存在", HttpStatus.NOT_FOUND);
        }
        if (config.getIsActive() == null || config.getIsActive() != 1) {
            throw new BusinessException(40001, "榜单已停用，不能生成新期次");
        }

        String periodNo = request.periodNo().trim();
        RankingGenerateParam param = new RankingGenerateParam();
        param.setRankingId(rankingId);
        param.setPeriodNo(periodNo);
        param.setAllowHistoryOverwrite(Boolean.TRUE.equals(request.allowHistoryOverwrite()) ? 1 : 0);

        try {
            procedureMapper.generateRanking(param);
        } catch (DataAccessException exception) {
            BusinessException businessException = ProcedureErrors.toBusiness(exception);
            if (businessException != null) {
                throw businessException;
            }
            throw exception;
        }

        int itemCount = param.getItemCount() == null ? 0 : param.getItemCount();
        return new RankingGenerateResponse(String.valueOf(rankingId), periodNo, itemCount);
    }

    private RankingConfigResponse toConfig(RankingConfigRow row) {
        return new RankingConfigResponse(
                String.valueOf(row.getRankingId()),
                row.getRankingCode(),
                row.getRankingName(),
                row.getRankingType(),
                row.getPeriodType(),
                row.getTopN() == null ? 0 : row.getTopN(),
                readJsonObject(row.getWeightConfig()),
                row.getIsActive() != null && row.getIsActive() == 1,
                row.getLatestPeriodNo(),
                row.getLatestItemCount() == null ? 0L : row.getLatestItemCount(),
                row.getUpdatedAt()
        );
    }

    private RankingDetailItem toDetailItem(RankingDetailRow row) {
        return new RankingDetailItem(
                row.getRankNo() == null ? 0 : row.getRankNo(),
                String.valueOf(row.getNovelId()),
                row.getNovelTitle(),
                row.getScore(),
                readJsonObject(row.getMetricSnapshot()),
                row.getCreatedAt()
        );
    }

    private String writeWeight(Map<String, Object> weightConfig) {
        if (weightConfig == null || weightConfig.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(weightConfig);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(40000, "权重配置不是合法的 JSON 对象");
        }
    }

    private Map<String, Object> readJsonObject(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException exception) {
            return Map.of();
        }
    }

    private static String requireCode(String rankingCode) {
        if (rankingCode == null || rankingCode.isBlank()) {
            throw new BusinessException(40000, "榜单编码不能为空");
        }
        return rankingCode.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
