package com.novel.platform.common.util;

import com.novel.platform.common.exception.BusinessException;

/**
 * 请求中的 BIGINT ID 以字符串传递，服务层统一转换为 Long。
 */
public final class IdUtils {

    private IdUtils() {
    }

    public static Long parse(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(40000, fieldName + " 不能为空");
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            throw new BusinessException(40000, fieldName + " 必须为数字");
        }
    }

    public static Long parseNullable(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parse(value, fieldName);
    }
}
