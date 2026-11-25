package com.novel.platform.common.api;

import com.novel.platform.common.exception.BusinessException;

/**
 * 分页参数校验：page 从 1 开始，pageSize 取值 1～100。
 */
public record PageParam(int page, int pageSize) {

    public static final int MAX_PAGE_SIZE = 100;

    public static PageParam of(Integer page, Integer pageSize) {
        int p = page == null ? 1 : page;
        int size = pageSize == null ? 10 : pageSize;
        if (p < 1) {
            throw BusinessException.badRequest("page: 必须大于等于 1");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw BusinessException.badRequest("pageSize: 必须在 1～100 之间");
        }
        return new PageParam(p, size);
    }

    public int offset() {
        return (page - 1) * pageSize;
    }
}
