package com.novel.platform.modules.interaction.service;

/**
 * 分页参数归一化：page 从 1 开始，pageSize 上限 100。
 */
final class Pagination {

    private static final int MAX_PAGE_SIZE = 100;

    private Pagination() {
    }

    static int page(int page) {
        return Math.max(page, 1);
    }

    static int size(int pageSize) {
        return Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }

    static int offset(int page, int pageSize) {
        return (page - 1) * pageSize;
    }
}
