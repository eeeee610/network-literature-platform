package com.novel.platform.common.api;

import java.util.List;

/**
 * 统一分页结果，data 结构固定为 list/total/page/pageSize。
 */
public record PageResult<T>(List<T> list, long total, int page, int pageSize) {
}
