package org.example.finishedbackend.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.finishedbackend.entity.DTO.NoticeDTO;
import org.example.finishedbackend.mapper.NoticeMapper;
import org.example.finishedbackend.service.NoticeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, NoticeDTO> implements NoticeService {

    @Override
    public List<NoticeDTO> listAll() {
        return this.list(new QueryWrapper<NoticeDTO>()
                .orderByDesc("is_top")
                .orderByDesc("create_time"));
    }

    @Override
    public NoticeDTO getDetail(int id) {
        return this.getById(id);
    }
}
