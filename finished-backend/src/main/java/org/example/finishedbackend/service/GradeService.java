package org.example.finishedbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.finishedbackend.entity.DTO.GradeDTO;

import java.util.List;

public interface GradeService extends IService<GradeDTO> {
    List<GradeDTO> queryByStudentId(String studentId);
}
