package org.example.finishedbackend.controller;

import jakarta.annotation.Resource;
import org.example.finishedbackend.entity.DTO.ActivityDTO;
import org.example.finishedbackend.entity.RestBean;
import org.example.finishedbackend.service.ActivityService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    @Resource
    ActivityService activityService;

    @GetMapping("/list")
    public RestBean<List<ActivityDTO>> list() {
        return RestBean.success(activityService.listAll(), null);
    }

    @PostMapping("/join")
    public RestBean<Void> join(@RequestParam int id) {
        String result = activityService.join(id);
        return result == null ? RestBean.success("报名成功") : RestBean.failure(400, result);
    }
}
