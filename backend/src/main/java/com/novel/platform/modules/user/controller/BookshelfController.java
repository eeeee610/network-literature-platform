package com.novel.platform.modules.user.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.common.api.PageResult;
import com.novel.platform.modules.user.model.Requests;
import com.novel.platform.modules.user.model.Views;
import com.novel.platform.modules.user.service.BookshelfService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    public ApiResponse<PageResult<Views.BookshelfItem>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(bookshelfService.list(page, pageSize));
    }

    @PostMapping
    public ApiResponse<Void> add(@Valid @RequestBody Requests.ShelfAdd request) {
        bookshelfService.add(request);
        return ApiResponse.success();
    }

    @GetMapping("/{novelId}")
    public ApiResponse<Views.ShelfStatus> status(@PathVariable String novelId) {
        return ApiResponse.success(bookshelfService.status(novelId));
    }

    @PutMapping("/{novelId}")
    public ApiResponse<Void> updateFlags(@PathVariable String novelId,
                                         @Valid @RequestBody Requests.ShelfUpdate request) {
        bookshelfService.updateFlags(novelId, request);
        return ApiResponse.success();
    }

    @PutMapping("/{novelId}/reading")
    public ApiResponse<Void> updateReading(@PathVariable String novelId,
                                           @Valid @RequestBody Requests.ReadingProgress request) {
        bookshelfService.updateReading(novelId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{novelId}")
    public ApiResponse<Void> remove(@PathVariable String novelId) {
        bookshelfService.remove(novelId);
        return ApiResponse.success();
    }
}
