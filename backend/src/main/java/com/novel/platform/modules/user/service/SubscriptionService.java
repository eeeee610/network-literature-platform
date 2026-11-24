package com.novel.platform.modules.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.novel.platform.common.api.PageResponse;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.user.dto.SubscribeResult;
import com.novel.platform.modules.user.dto.SubscriptionItemResponse;
import com.novel.platform.modules.user.mapper.ChapterSubscriptionMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SubscriptionService {

    private final ChapterSubscriptionMapper subscriptionMapper;
    private final TradeProcedureService procedureService;

    public SubscriptionService(ChapterSubscriptionMapper subscriptionMapper,
                               TradeProcedureService procedureService) {
        this.subscriptionMapper = subscriptionMapper;
        this.procedureService = procedureService;
    }

    public PageResponse<SubscriptionItemResponse> list(Long userId, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        Page<SubscriptionItemResponse> pageRequest = new Page<>(safePage, safeSize);
        return PageResponse.from(subscriptionMapper.selectSubscriptionPage(pageRequest, userId));
    }

    public SubscribeResult subscribe(Long userId, String chapterIdText, String requestNoText) {
        Long chapterId = parseId(chapterIdText, "chapterId");
        String requestNo = normalizeRequestNo(requestNoText);

        TradeProcedureService.ProcedureSubscribeResult result =
                procedureService.subscribe(userId, chapterId, requestNo);

        if (result.resultCode() == null) {
            throw new BusinessException(50000, "订阅处理未返回结果", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (!"SUCCESS".equals(result.resultCode()) && !"ALREADY_SUBSCRIBED".equals(result.resultCode())) {
            throw new BusinessException(40903, result.message() == null ? "订阅未完成" : result.message(),
                    HttpStatus.CONFLICT);
        }

        return new SubscribeResult(
                requestNo,
                result.txnNo(),
                result.balanceAfter(),
                "ALREADY_SUBSCRIBED".equals(result.resultCode()),
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
