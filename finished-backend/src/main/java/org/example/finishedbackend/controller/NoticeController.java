package org.example.finishedbackend.controller;

import jakarta.annotation.Resource;
import org.example.finishedbackend.entity.DTO.NoticeDTO;
import org.example.finishedbackend.entity.RestBean;
import org.example.finishedbackend.service.NoticeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    @Resource
    NoticeService noticeService;

    @GetMapping("/list")
    public RestBean<List<NoticeDTO>> list() {
        return RestBean.success(noticeService.listAll(), null);
    }

    @GetMapping("/detail")
    public RestBean<NoticeDTO> detail(@RequestParam int id) {
        NoticeDTO dto = noticeService.getDetail(id);
        return dto != null ? RestBean.success(dto, null) : RestBean.failure(400, "通知不存在");
    }
}
