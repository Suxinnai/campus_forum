package org.example.finishedbackend.controller;

import jakarta.annotation.Resource;
import org.example.finishedbackend.entity.RestBean;
import org.example.finishedbackend.entity.VO.response.TopicPreviewVO;
import org.example.finishedbackend.service.RecommendService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    @Resource
    RecommendService recommendService;

    /**
     * 获取个性化推荐帖子列表
     */
    @GetMapping("/topics")
    public RestBean<List<TopicPreviewVO>> recommend(@RequestAttribute("id") int id) {
        List<TopicPreviewVO> list = recommendService.recommend(id);
        return RestBean.success(list, "获取推荐列表成功");
    }
}
