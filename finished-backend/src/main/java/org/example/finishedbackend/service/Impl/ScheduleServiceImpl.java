package org.example.finishedbackend.service.Impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.finishedbackend.entity.DTO.ScheduleDTO;
import org.example.finishedbackend.mapper.ScheduleMapper;
import org.example.finishedbackend.service.ScheduleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, ScheduleDTO> implements ScheduleService {
    @Override
    public List<ScheduleDTO> listAll() {
        return this.list(Wrappers.<ScheduleDTO>query().orderByAsc("event_date"));
    }
}
