package com.novel.content.dto;

public class PageQuery {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private int page = 1;
    private int pageSize = DEFAULT_PAGE_SIZE;

    public static PageQuery of(Integer page, Integer pageSize) {
        PageQuery query = new PageQuery();
        if (page != null) {
            query.page = page;
        }
        if (pageSize != null) {
            query.pageSize = pageSize;
        }
        return query.normalize();
    }

    public PageQuery normalize() {
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }
        return this;
    }

    public int offset() {
        return (page - 1) * pageSize;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }
}
