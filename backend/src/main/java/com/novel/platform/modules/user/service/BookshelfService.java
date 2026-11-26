package com.novel.platform.modules.user.service;

import com.novel.platform.common.api.PageResult;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.user.dto.BookshelfItem;
import com.novel.platform.modules.user.entity.AppUser;
import com.novel.platform.modules.user.mapper.BookshelfMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookshelfService {

    private final UserService userService;
    private final BookshelfMapper bookshelfMapper;

    public BookshelfService(UserService userService, BookshelfMapper bookshelfMapper) {
        this.userService = userService;
        this.bookshelfMapper = bookshelfMapper;
    }

    @Transactional
    public BookshelfItem add(String novelIdRaw) {
        AppUser user = userService.requireActiveUser();
        long novelId = UserSupport.id(novelIdRaw, "novelId");
        BookshelfItem existing = bookshelfMapper.selectItem(user.getUserId(), novelId);
        if (existing != null) {
            return withAdded(existing, false);
        }
        if (bookshelfMapper.selectVisibleNovelId(novelId) == null) {
            throw new BusinessException(41009, "作品不存在或当前不可加入书架", HttpStatus.NOT_FOUND);
        }
        int inserted = bookshelfMapper.insertIgnore(user.getUserId(), novelId);
        BookshelfItem saved = bookshelfMapper.selectItem(user.getUserId(), novelId);
        if (saved == null) {
            throw new IllegalStateException("书架记录保存失败");
        }
        return withAdded(saved, inserted == 1);
    }

    @Transactional
    public BookshelfItem remove(String novelIdRaw) {
        AppUser user = userService.requireActiveUser();
        long novelId = UserSupport.id(novelIdRaw, "novelId");
        int removed = bookshelfMapper.delete(user.getUserId(), novelId);
        return new BookshelfItem(String.valueOf(novelId), null, null, null, null, null, null,
                null, null, null, null, null, null, null, false, removed == 1);
    }

    public PageResult<BookshelfItem> list(Integer pageRaw, Integer pageSizeRaw) {
        AppUser user = userService.requireActiveUser();
        int page = UserSupport.page(pageRaw);
        int pageSize = UserSupport.pageSize(pageSizeRaw);
        long total = bookshelfMapper.countByUser(user.getUserId());
        List<BookshelfItem> items = total == 0 ? List.of() : bookshelfMapper.selectPage(
                user.getUserId(), UserSupport.offset(page, pageSize), pageSize);
        return new PageResult<>(items, total, page, pageSize);
    }

    private BookshelfItem withAdded(BookshelfItem item, boolean added) {
        return new BookshelfItem(item.novelId(), item.title(), item.coverUrl(), item.authorId(),
                item.authorName(), item.novelStatus(), item.auditStatus(), item.lastReadChapterId(),
                item.lastReadChapterTitle(), item.lastReadAt(), item.isTop(), item.isNotify(),
                item.createdAt(), item.updatedAt(), added, false);
    }
}
