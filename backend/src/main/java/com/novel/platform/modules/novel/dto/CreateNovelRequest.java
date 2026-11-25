package com.novel.platform.modules.novel.dto;

import java.util.List;

public record CreateNovelRequest(String title, String coverUrl, String description, List<String> tagIds) {
}
