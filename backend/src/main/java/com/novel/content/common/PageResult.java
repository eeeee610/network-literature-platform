package com.novel.content.common;

import java.util.List;

public class PageResult<T> {

    private final int page;
    private final int pageSize;
    private final long total;
    private final List<T> list;

    public PageResult(int page, int pageSize, long total, List<T> list) {
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
        this.list = list;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotal() {
        return total;
    }

    public List<T> getList() {
        return list;
    }
}
