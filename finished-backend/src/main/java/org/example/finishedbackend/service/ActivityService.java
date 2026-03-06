package org.example.finishedbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.finishedbackend.entity.DTO.ActivityDTO;

import java.util.List;

public interface ActivityService extends IService<ActivityDTO> {
    List<ActivityDTO> listAll();

    String join(int id);
}
