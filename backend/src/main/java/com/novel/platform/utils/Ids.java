package com.novel.platform.utils;

import com.novel.platform.common.exception.BusinessException;

/**
 * 字符串 ID 解析：JSON 中 BIGINT ID 以字符串传递，服务端统一转 Long。
 */
public final class Ids {

    private Ids() {
    }

    public static Long parse(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw BusinessException.badRequest(field + ": 不能为空");
        }
        try {
            long value = Long.parseLong(raw.trim());
            if (value <= 0) {
                throw BusinessException.badRequest(field + ": 必须为正整数");
            }
            return value;
        } catch (NumberFormatException e) {
            throw BusinessException.badRequest(field + ": 必须为合法的数字 ID");
        }
    }
}
