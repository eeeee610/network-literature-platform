package com.novel.platform.modules.novel.service;

import com.novel.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * B 模块内部通用校验与格式转换工具。
 */
final class NovelSupport {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter ISO_OFFSET =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private NovelSupport() {
    }

    static String iso(LocalDateTime time) {
        return time == null ? null : time.atZone(ZONE).toOffsetDateTime().format(ISO_OFFSET);
    }

    static long parseId(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw badRequest(field + ": 不能为空");
        }
        try {
            long value = Long.parseLong(raw.trim());
            if (value <= 0) {
                throw badRequest(field + ": 必须是正整数");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw badRequest(field + ": 必须是数字");
        }
    }

    static int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    static int checkPageSize(Integer pageSize) {
        if (pageSize == null) {
            return 10;
        }
        if (pageSize < 1 || pageSize > 100) {
            throw badRequest("pageSize: 取值 1～100");
        }
        return pageSize;
    }

    static long offset(int page, int pageSize) {
        return (long) (page - 1) * pageSize;
    }

    static BusinessException badRequest(String message) {
        return new BusinessException(40000, message, HttpStatus.BAD_REQUEST);
    }
}
