package org.example.finishedbackend.controller;

import jakarta.annotation.Resource;
import org.example.finishedbackend.entity.DTO.ScheduleDTO;
import org.example.finishedbackend.entity.RestBean;
import org.example.finishedbackend.service.ScheduleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    @Resource
    ScheduleService scheduleService;

    @GetMapping("/list")
    public RestBean<List<ScheduleDTO>> list() {
        return RestBean.success(scheduleService.listAll(), null);
    }
}
