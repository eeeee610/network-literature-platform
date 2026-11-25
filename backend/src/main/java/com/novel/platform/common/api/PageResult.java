package com.novel.platform.common.api;

import java.util.List;
import java.util.function.Function;

/**
 * 分页响应：list、total、page、pageSize 固定结构。
 */
public record PageResult<T>(List<T> list, long total, int page, int pageSize) {

    public static <R, T> PageResult<T> of(List<R> rows, long total, int page, int pageSize,
                                          Function<R, T> mapper) {
        return new PageResult<>(rows.stream().map(mapper).toList(), total, page, pageSize);
    }
}
