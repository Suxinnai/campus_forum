package org.example.finishedbackend.service;

import org.example.finishedbackend.entity.VO.response.CategoryStatVO;
import org.example.finishedbackend.entity.VO.response.HotTrendVO;
import org.example.finishedbackend.entity.VO.response.KeywordVO;

import java.util.List;

public interface StatService {
    /**
     * 近7天发帖热度趋势
     */
    List<HotTrendVO> hotTrend();

    /**
     * 各板块帖子分类占比
     */
    List<CategoryStatVO> categoryStat();

    /**
     * 热门关键词词频（基于帖子标题分词统计）
     */
    List<KeywordVO> keywordCloud();
}
