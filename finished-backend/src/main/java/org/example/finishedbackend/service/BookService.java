package org.example.finishedbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.finishedbackend.entity.DTO.BookDTO;

import java.util.List;

public interface BookService extends IService<BookDTO> {
    List<BookDTO> listByPage(int page, String keyword, String category);

    BookDTO getDetail(int id);

    List<String> listCategories();
}
