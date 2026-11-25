package com.novel.platform.modules.user.dto;

import java.util.List;

/**
 * 列表统一响应：{ list, total, page, pageSize }
 */
public record PageResponse<T>(List<T> list, long total, long page, long pageSize) {

    public static <T> PageResponse<T> of(List<T> list, long total, long page, long pageSize) {
        return new PageResponse<>(list, total, page, pageSize);
    }

    public static <T> PageResponse<T> empty(long page, long pageSize) {
        return new PageResponse<>(List.of(), 0, page, pageSize);
    }
}
