package com.novel.platform.modules.user.service;

import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.user.dto.PageResponse;
import com.novel.platform.modules.user.dto.ShelfItemResponse;
import com.novel.platform.modules.user.mapper.CatalogQueryMapper;
import com.novel.platform.modules.user.mapper.UserBookshelfMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookshelfService {

    private final UserBookshelfMapper shelfMapper;
    private final CatalogQueryMapper catalogMapper;

    public BookshelfService(UserBookshelfMapper shelfMapper, CatalogQueryMapper catalogMapper) {
        this.shelfMapper = shelfMapper;
        this.catalogMapper = catalogMapper;
    }

    public PageResponse<ShelfItemResponse> list(Long userId, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (safePage - 1) * safeSize;
        long total = shelfMapper.countShelf(userId);
        List<ShelfItemResponse> list = shelfMapper.selectShelfPage(userId, offset, safeSize);
        return PageResponse.of(list, total, safePage, safeSize);
    }

    public void add(Long userId, String novelIdText) {
        Long novelId = parseId(novelIdText, "novelId");
        if (catalogMapper.countNovel(novelId) == 0) {
            throw new BusinessException(40410, "小说不存在", HttpStatus.NOT_FOUND);
        }
        try {
            shelfMapper.insertShelf(userId, novelId);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(40920, "该小说已在书架中", HttpStatus.CONFLICT);
        }
    }

    public void update(Long userId, Long novelId, Boolean isTop, Boolean isNotify) {
        if (isTop == null && isNotify == null) {
            throw new BusinessException(40010, "isTop 和 isNotify 至少提供一个");
        }
        int changed = shelfMapper.updateFlags(userId, novelId, isTop, isNotify);
        if (changed == 0) {
            throw new BusinessException(40411, "书架记录不存在", HttpStatus.NOT_FOUND);
        }
    }

    public void remove(Long userId, Long novelId) {
        int changed = shelfMapper.deleteShelf(userId, novelId);
        if (changed == 0) {
            throw new BusinessException(40411, "书架记录不存在", HttpStatus.NOT_FOUND);
        }
    }

    private Long parseId(String value, String field) {
        try {
            long id = Long.parseLong(value.trim());
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (Exception exception) {
            throw new BusinessException(40000, field + ": ID 格式不正确");
        }
    }
}
