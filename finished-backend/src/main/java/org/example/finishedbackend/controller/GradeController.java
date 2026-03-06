package org.example.finishedbackend.controller;

import jakarta.annotation.Resource;
import org.example.finishedbackend.entity.DTO.GradeDTO;
import org.example.finishedbackend.entity.RestBean;
import org.example.finishedbackend.service.GradeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grade")
public class GradeController {

    @Resource
    GradeService gradeService;

    @GetMapping("/query")
    public RestBean<List<GradeDTO>> query(@RequestParam String studentId) {
        List<GradeDTO> list = gradeService.queryByStudentId(studentId);
        return list.isEmpty() ? RestBean.failure(400, "未找到该学号的成绩记录")
                : RestBean.success(list, null);
    }
}
