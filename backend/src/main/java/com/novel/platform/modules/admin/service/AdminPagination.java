package com.novel.platform.modules.admin.service;

final class AdminPagination {

    private static final int MAX_PAGE_SIZE = 100;

    private AdminPagination() {
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
