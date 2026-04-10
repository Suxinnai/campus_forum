package org.example.finishedbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.finishedbackend.entity.DTO.ScheduleDTO;

import java.util.List;

public interface ScheduleService extends IService<ScheduleDTO> {
    List<ScheduleDTO> listAll();
}
