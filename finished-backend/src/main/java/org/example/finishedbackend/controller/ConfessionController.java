package org.example.finishedbackend.controller;

import jakarta.annotation.Resource;
import org.example.finishedbackend.entity.DTO.ConfessionDTO;
import org.example.finishedbackend.entity.RestBean;
import org.example.finishedbackend.service.ConfessionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/confession")
public class ConfessionController {

    @Resource
    ConfessionService confessionService;

    @GetMapping("/list")
    public RestBean<List<Map<String, Object>>> list(@RequestParam(defaultValue = "1") int page) {
        return RestBean.success(confessionService.listByPage(page), null);
    }

    @PostMapping("/publish")
    public RestBean<Void> publish(@RequestAttribute("id") int uid,
            @RequestBody ConfessionDTO dto) {
        String result = confessionService.publish(uid, dto);
        return result == null ? RestBean.success("发布成功") : RestBean.failure(400, result);
    }

    @PostMapping("/like")
    public RestBean<Void> like(@RequestParam int id) {
        String result = confessionService.like(id);
        return result == null ? RestBean.success(null) : RestBean.failure(400, result);
    }
}
