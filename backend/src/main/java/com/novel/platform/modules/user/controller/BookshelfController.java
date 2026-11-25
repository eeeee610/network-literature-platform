package com.novel.platform.modules.user.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.common.api.PageResult;
import com.novel.platform.modules.user.dto.BookshelfItem;
import com.novel.platform.modules.user.dto.BookshelfRequest;
import com.novel.platform.modules.user.service.BookshelfService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

    @PostMapping
    public ApiResponse<BookshelfItem> add(@Valid @RequestBody BookshelfRequest request) {
        return ApiResponse.success(bookshelfService.add(request.novelId()));
    }

    @DeleteMapping("/{novelId}")
    public ApiResponse<BookshelfItem> remove(@PathVariable String novelId) {
        return ApiResponse.success(bookshelfService.remove(novelId));
    }

    @GetMapping
    public ApiResponse<PageResult<BookshelfItem>> list(@RequestParam(required = false) Integer page,
                                                       @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(bookshelfService.list(page, pageSize));
    }
}
