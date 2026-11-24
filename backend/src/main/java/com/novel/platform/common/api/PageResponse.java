package com.novel.platform.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 列表统一响应：{ list, total, page, pageSize }
 */
public record PageResponse<T>(List<T> list, long total, long page, long pageSize) {

    public static <T> PageResponse<T> from(IPage<T> page) {
        return new PageResponse<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public static <T> PageResponse<T> empty(long page, long pageSize) {
        return new PageResponse<>(List.of(), 0, page, pageSize);
    }
}
