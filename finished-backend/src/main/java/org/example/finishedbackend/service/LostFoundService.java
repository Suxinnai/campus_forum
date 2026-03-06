package org.example.finishedbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.finishedbackend.entity.DTO.LostFoundDTO;

import java.util.List;

public interface LostFoundService extends IService<LostFoundDTO> {
    List<LostFoundDTO> listByPage(int page, String type);

    String publish(int uid, LostFoundDTO dto);

    String close(int id, int uid);
}
