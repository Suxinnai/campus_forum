package org.example.finishedbackend.controller;

import jakarta.annotation.Resource;
import org.example.finishedbackend.entity.RestBean;
import org.example.finishedbackend.entity.VO.response.CategoryStatVO;
import org.example.finishedbackend.entity.VO.response.HotTrendVO;
import org.example.finishedbackend.entity.VO.response.KeywordVO;
import org.example.finishedbackend.service.StatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stat")
public class StatController {

    @Resource
    StatService statService;

    /**
     * 近7天发帖热度趋势（折线图）
     */
    @GetMapping("/hot-trend")
    public RestBean<List<HotTrendVO>> hotTrend() {
        return RestBean.success(statService.hotTrend(), "获取热度趋势成功");
    }

    /**
     * 各板块帖子分类占比（饼图）
     */
    @GetMapping("/category-pie")
    public RestBean<List<CategoryStatVO>> categoryStat() {
        return RestBean.success(statService.categoryStat(), "获取分类统计成功");
    }

    /**
     * 热门关键词词频（词云图）
     */
    @GetMapping("/keyword-cloud")
    public RestBean<List<KeywordVO>> keywordCloud() {
        return RestBean.success(statService.keywordCloud(), "获取关键词热度成功");
    }
}
