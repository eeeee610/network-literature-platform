package com.novel.platform.modules.user.service;

import com.novel.platform.common.api.PageParam;
import com.novel.platform.common.api.PageResult;
import com.novel.platform.common.auth.CurrentUser;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.common.exception.ErrorCodes;
import com.novel.platform.modules.user.entity.UserBookshelf;
import com.novel.platform.modules.user.mapper.NovelQueryMapper;
import com.novel.platform.modules.user.mapper.UserBookshelfMapper;
import com.novel.platform.modules.user.model.Requests;
import com.novel.platform.modules.user.model.Rows;
import com.novel.platform.modules.user.model.Views;
import com.novel.platform.utils.Ids;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookshelfService {

    private final UserBookshelfMapper bookshelfMapper;
    private final NovelQueryMapper novelQueryMapper;

    public BookshelfService(UserBookshelfMapper bookshelfMapper, NovelQueryMapper novelQueryMapper) {
        this.bookshelfMapper = bookshelfMapper;
        this.novelQueryMapper = novelQueryMapper;
    }

    public PageResult<Views.BookshelfItem> list(Integer page, Integer pageSize) {
        PageParam param = PageParam.of(page, pageSize);
        Long userId = CurrentUser.userId();
        long total = bookshelfMapper.countByUser(userId);
        List<Rows.BookshelfRow> rows = bookshelfMapper.selectPage(userId, param.offset(), param.pageSize());
        return PageResult.of(rows, total, param.page(), param.pageSize(), this::toItem);
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(Requests.ShelfAdd request) {
        Long userId = CurrentUser.userId();
        Long novelId = Ids.parse(request.novelId(), "novelId");
        Rows.NovelBrief novel = novelQueryMapper.selectNovel(novelId);
        if (!isPublic(novel)) {
            throw BusinessException.notFound(ErrorCodes.NOVEL_NOT_FOUND, "作品不存在或已下架");
        }
        if (bookshelfMapper.selectOne(userId, novelId) != null) {
            return;
        }
        bookshelfMapper.insertOne(userId, novelId);
    }

    public Views.ShelfStatus status(String novelId) {
        Long userId = CurrentUser.userId();
        Long id = Ids.parse(novelId, "novelId");
        UserBookshelf shelf = bookshelfMapper.selectOne(userId, id);
        if (shelf == null) {
            return new Views.ShelfStatus(false, null, null, null);
        }
        return new Views.ShelfStatus(true, shelf.getIsTop(), shelf.getIsNotify(),
                shelf.getLastReadChapterId() == null ? null : String.valueOf(shelf.getLastReadChapterId()));
    }

    public void updateFlags(String novelId, Requests.ShelfUpdate request) {
        Long userId = CurrentUser.userId();
        Long id = Ids.parse(novelId, "novelId");
        if (request.isTop() == null && request.isNotify() == null) {
            throw BusinessException.badRequest("isTop 与 isNotify 至少提交一项");
        }
        int updated = bookshelfMapper.updateFlags(userId, id, request.isTop(), request.isNotify());
        if (updated == 0) {
            throw BusinessException.notFound(ErrorCodes.SHELF_NOT_FOUND, "书架中不存在该作品");
        }
    }

    public void updateReading(String novelId, Requests.ReadingProgress request) {
        Long userId = CurrentUser.userId();
        Long id = Ids.parse(novelId, "novelId");
        Long chapterId = Ids.parse(request.chapterId(), "chapterId");
        if (bookshelfMapper.selectOne(userId, id) == null) {
            throw BusinessException.notFound(ErrorCodes.SHELF_NOT_FOUND, "书架中不存在该作品");
        }
        Rows.ChapterBrief chapter = novelQueryMapper.selectChapter(chapterId);
        if (chapter == null || !chapter.novelId.equals(id)) {
            throw BusinessException.notFound(ErrorCodes.CHAPTER_NOT_AVAILABLE, "章节不存在或不属于该作品");
        }
        bookshelfMapper.updateReading(userId, id, chapterId);
    }

    public void remove(String novelId) {
        Long userId = CurrentUser.userId();
        Long id = Ids.parse(novelId, "novelId");
        int deleted = bookshelfMapper.deleteOne(userId, id);
        if (deleted == 0) {
            throw BusinessException.notFound(ErrorCodes.SHELF_NOT_FOUND, "书架中不存在该作品");
        }
    }

    private boolean isPublic(Rows.NovelBrief novel) {
        return novel != null && novel.auditStatus != null && novel.auditStatus == 1
                && novel.status != null && (novel.status == 0 || novel.status == 1);
    }

    private Views.BookshelfItem toItem(Rows.BookshelfRow row) {
        return new Views.BookshelfItem(
                String.valueOf(row.novelId), row.title, row.coverUrl, row.status, row.totalWords,
                row.latestChapterId == null ? null : String.valueOf(row.latestChapterId),
                row.latestChapterTitle,
                row.lastReadChapterId == null ? null : String.valueOf(row.lastReadChapterId),
                row.lastReadTitle, row.lastReadAt, row.isTop, row.isNotify, row.updatedAt);
    }
}
