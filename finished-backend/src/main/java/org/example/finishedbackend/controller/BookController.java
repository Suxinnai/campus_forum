package org.example.finishedbackend.controller;

import jakarta.annotation.Resource;
import org.example.finishedbackend.entity.DTO.BookDTO;
import org.example.finishedbackend.entity.RestBean;
import org.example.finishedbackend.service.BookService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/book")
public class BookController {

    @Resource
    BookService bookService;

    @GetMapping("/list")
    public RestBean<List<BookDTO>> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String category) {
        return RestBean.success(bookService.listByPage(page, keyword, category), null);
    }

    @GetMapping("/detail")
    public RestBean<BookDTO> detail(@RequestParam int id) {
        BookDTO dto = bookService.getDetail(id);
        return dto != null ? RestBean.success(dto, null) : RestBean.failure(400, "图书不存在");
    }

    @GetMapping("/categories")
    public RestBean<List<String>> categories() {
        return RestBean.success(bookService.listCategories(), null);
    }
}
