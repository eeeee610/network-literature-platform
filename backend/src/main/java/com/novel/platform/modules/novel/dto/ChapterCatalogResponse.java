package com.novel.platform.modules.novel.dto;

import java.util.List;

public record ChapterCatalogResponse(String novelId, Integer total, List<ChapterCatalogItem> list) {
}
