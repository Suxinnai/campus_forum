package org.example.finishedbackend.controller;

import jakarta.annotation.Resource;
import org.example.finishedbackend.entity.DTO.LostFoundDTO;
import org.example.finishedbackend.entity.RestBean;
import org.example.finishedbackend.service.LostFoundService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lost-found")
public class LostFoundController {

    @Resource
    LostFoundService lostFoundService;

    @GetMapping("/list")
    public RestBean<List<LostFoundDTO>> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "") String type) {
        return RestBean.success(lostFoundService.listByPage(page, type), null);
    }

    @PostMapping("/publish")
    public RestBean<Void> publish(@RequestAttribute("id") int uid,
            @RequestBody LostFoundDTO dto) {
        String result = lostFoundService.publish(uid, dto);
        return result == null ? RestBean.success("发布成功") : RestBean.failure(400, result);
    }

    @PostMapping("/close")
    public RestBean<Void> close(@RequestParam int id, @RequestAttribute("id") int uid) {
        String result = lostFoundService.close(id, uid);
        return result == null ? RestBean.success("已关闭") : RestBean.failure(400, result);
    }
}
