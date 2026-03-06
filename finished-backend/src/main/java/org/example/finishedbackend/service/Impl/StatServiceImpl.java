package org.example.finishedbackend.service.Impl;

import jakarta.annotation.Resource;
import org.example.finishedbackend.entity.VO.response.CategoryStatVO;
import org.example.finishedbackend.entity.VO.response.HotTrendVO;
import org.example.finishedbackend.entity.VO.response.KeywordVO;
import org.example.finishedbackend.mapper.StatMapper;
import org.example.finishedbackend.service.StatService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatServiceImpl implements StatService {

    @Resource
    StatMapper statMapper;

    @Override
    public List<HotTrendVO> hotTrend() {
        return statMapper.selectHotTrend();
    }

    @Override
    public List<CategoryStatVO> categoryStat() {
        return statMapper.selectCategoryStat();
    }

    @Override
    public List<KeywordVO> keywordCloud() {
        List<String> titles = statMapper.selectRecentTitles();
        // 简单中文分词：按单字符或2-4字连续汉字组合进行统计
        // 毕设阶段使用简化版分词，剔除常用停用词
        Map<String, Integer> wordFreq = new HashMap<>();
        Set<String> stopWords = Set.of(
                "的", "了", "在", "是", "我", "有", "和", "就",
                "不", "人", "都", "一", "一个", "上", "也", "很",
                "到", "说", "要", "去", "你", "会", "着", "没有",
                "看", "好", "自己", "这", "他", "她", "吗", "什么",
                "那", "怎么", "如何", "可以", "大家", "请问");
        for (String title : titles) {
            if (title == null || title.isEmpty())
                continue;
            // 提取2~4字的汉字词组作为简单"分词"
            for (int len = 2; len <= 4; len++) {
                for (int i = 0; i <= title.length() - len; i++) {
                    String word = title.substring(i, i + len);
                    // 只保留纯汉字词组
                    if (word.matches("[\\u4e00-\\u9fa5]+") && !stopWords.contains(word)) {
                        wordFreq.merge(word, 1, Integer::sum);
                    }
                }
            }
        }
        // 取词频前50的关键词
        return wordFreq.entrySet().stream()
                .filter(e -> e.getValue() >= 2) // 至少出现2次
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(50)
                .map(e -> new KeywordVO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }
}
