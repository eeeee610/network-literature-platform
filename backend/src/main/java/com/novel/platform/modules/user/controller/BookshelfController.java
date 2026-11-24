package com.novel.platform.modules.user.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.common.api.PageResponse;
import com.novel.platform.common.security.UserContext;
import com.novel.platform.modules.user.dto.ShelfAddRequest;
import com.novel.platform.modules.user.dto.ShelfItemResponse;
import com.novel.platform.modules.user.dto.ShelfUpdateRequest;
import com.novel.platform.modules.user.service.BookshelfService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/bookshelf")
public class BookshelfController {

    private final BookshelfService bookshelfService;

    public BookshelfController(BookshelfService bookshelfService) {
        this.bookshelfService = bookshelfService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ShelfItemResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(bookshelfService.list(UserContext.requireUserId(), page, pageSize));
    }

    @PostMapping
    public ApiResponse<Void> add(@Valid @RequestBody ShelfAddRequest request) {
        bookshelfService.add(UserContext.requireUserId(), request.novelId());
        return ApiResponse.success();
    }

    @PatchMapping("/{novelId}")
    public ApiResponse<Void> update(@PathVariable Long novelId,
                                    @RequestBody ShelfUpdateRequest request) {
        bookshelfService.update(UserContext.requireUserId(), novelId, request.isTop(), request.isNotify());
        return ApiResponse.success();
    }

    @DeleteMapping("/{novelId}")
    public ApiResponse<Void> remove(@PathVariable Long novelId) {
        bookshelfService.remove(UserContext.requireUserId(), novelId);
        return ApiResponse.success();
    }
}
