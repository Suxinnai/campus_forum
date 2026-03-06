package org.example.finishedbackend.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.finishedbackend.entity.DTO.GradeDTO;
import org.example.finishedbackend.mapper.GradeMapper;
import org.example.finishedbackend.service.GradeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GradeServiceImpl extends ServiceImpl<GradeMapper, GradeDTO> implements GradeService {

    @Override
    public List<GradeDTO> queryByStudentId(String studentId) {
        return this.list(new QueryWrapper<GradeDTO>()
                .eq("student_id", studentId)
                .orderByAsc("course_name"));
    }
}
