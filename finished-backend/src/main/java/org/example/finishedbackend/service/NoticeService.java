package org.example.finishedbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.finishedbackend.entity.DTO.NoticeDTO;

import java.util.List;

public interface NoticeService extends IService<NoticeDTO> {
    List<NoticeDTO> listAll();

    NoticeDTO getDetail(int id);
}
