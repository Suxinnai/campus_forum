package org.example.finishedbackend.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.finishedbackend.entity.DTO.BookDTO;
import org.example.finishedbackend.mapper.BookMapper;
import org.example.finishedbackend.service.BookService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, BookDTO> implements BookService {

    @Override
    public List<BookDTO> listByPage(int page, String keyword, String category) {
        QueryWrapper<BookDTO> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("title", keyword).or().like("author", keyword));
        }
        if (category != null && !category.isEmpty()) {
            qw.eq("category", category);
        }
        qw.orderByDesc("create_time");
        return this.page(new Page<>(page, 12), qw).getRecords();
    }

    @Override
    public BookDTO getDetail(int id) {
        return this.getById(id);
    }

    @Override
    public List<String> listCategories() {
        QueryWrapper<BookDTO> qw = new QueryWrapper<>();
        qw.select("DISTINCT category");
        return this.list(qw).stream().map(BookDTO::getCategory).toList();
    }
}
