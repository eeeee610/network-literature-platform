package com.novel.platform.modules.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.novel.platform.common.api.PageResponse;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.user.dto.TipItemResponse;
import com.novel.platform.modules.user.dto.TipRequest;
import com.novel.platform.modules.user.dto.TipResult;
import com.novel.platform.modules.user.mapper.CatalogQueryMapper;
import com.novel.platform.modules.user.mapper.RewardTipMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TipService {

    private final RewardTipMapper tipMapper;
    private final CatalogQueryMapper catalogMapper;
    private final TradeProcedureService procedureService;

    public TipService(RewardTipMapper tipMapper, CatalogQueryMapper catalogMapper,
                      TradeProcedureService procedureService) {
        this.tipMapper = tipMapper;
        this.catalogMapper = catalogMapper;
        this.procedureService = procedureService;
    }

    public PageResponse<TipItemResponse> list(Long userId, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        Page<TipItemResponse> pageRequest = new Page<>(safePage, safeSize);
        return PageResponse.from(tipMapper.selectTipPage(pageRequest, userId));
    }

    public TipResult tip(Long userId, TipRequest request) {
        Long novelId = parseId(request.novelId(), "novelId");
        Long chapterId = blankToNull(request.chapterId()) == null
                ? null : parseId(request.chapterId(), "chapterId");

        Long authorId;
        if (chapterId == null) {
            authorId = catalogMapper.findNovelAuthor(novelId);
        } else {
            authorId = catalogMapper.findChapterAuthor(chapterId, novelId);
        }
        if (authorId == null) {
            throw new BusinessException(40420, "小说或章节不存在，或章节不属于该小说", HttpStatus.NOT_FOUND);
        }
        if (authorId.equals(userId)) {
            throw new BusinessException(40020, "不能给自己打赏");
        }

        String requestNo = normalizeRequestNo(request.requestNo());
        TradeProcedureService.ProcedureTipResult result = procedureService.tip(
                userId, authorId, novelId, chapterId, request.amount(),
                request.message() == null ? "" : request.message().trim(), requestNo
        );

        if (result.resultCode() == null) {
            throw new BusinessException(50000, "打赏处理未返回结果", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (!"SUCCESS".equals(result.resultCode()) && !"DUPLICATE_REQUEST".equals(result.resultCode())) {
            throw new BusinessException(40903, result.message() == null ? "打赏未完成" : result.message(),
                    HttpStatus.CONFLICT);
        }

        return new TipResult(
                requestNo,
                result.tipNo(),
                result.fromBalanceAfter(),
                result.toBalanceAfter(),
                "DUPLICATE_REQUEST".equals(result.resultCode()),
                result.message()
        );
    }

    private String normalizeRequestNo(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 64) {
            throw new BusinessException(40000, "requestNo 不能超过 64 个字符");
        }
        return trimmed;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long parseId(String value, String field) {
        try {
            long id = Long.parseLong(value.trim());
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (Exception exception) {
            throw new BusinessException(40000, field + ": ID 格式不正确");
        }
    }
}
