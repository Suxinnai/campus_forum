package org.example.finishedbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.finishedbackend.entity.DTO.ConfessionDTO;

import java.util.List;
import java.util.Map;

public interface ConfessionService extends IService<ConfessionDTO> {
    List<Map<String, Object>> listByPage(int page);

    String publish(int uid, ConfessionDTO dto);

    String like(int id);
}
