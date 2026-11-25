package com.novel.platform.modules.user.service;

import com.novel.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

final class UserSupport {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private UserSupport() {
    }

    static long id(String raw, String field) {
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

    static Long nullableId(String raw, String field) {
        return raw == null || raw.isBlank() ? null : id(raw, field);
    }

    static int page(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }

    static int pageSize(Integer value) {
        if (value == null) {
            return 10;
        }
        if (value < 1 || value > 100) {
            throw badRequest("pageSize: 取值 1～100");
        }
        return value;
    }

    static long offset(int page, int pageSize) {
        return (long) (page - 1) * pageSize;
    }

    static String iso(LocalDateTime time) {
        return time == null ? null : time.atZone(ZONE).toOffsetDateTime().format(ISO);
    }

    static String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static BusinessException badRequest(String message) {
        return new BusinessException(40000, message, HttpStatus.BAD_REQUEST);
    }
}
