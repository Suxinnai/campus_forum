package org.example.finishedbackend.service.admin;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.example.finishedbackend.entity.DTO.AccountDTO;
import org.example.finishedbackend.entity.DTO.TopicCommentDTO;
import org.example.finishedbackend.entity.DTO.TopicDTO;
import org.example.finishedbackend.mapper.SensitiveWordMapper;
import org.example.finishedbackend.mapper.TopicCommentMapper;
import org.example.finishedbackend.mapper.TopicMapper;
import org.example.finishedbackend.service.AccountService;
import org.example.finishedbackend.service.TopicService;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDashboardService {

    @Resource
    AccountService accountService;

    @Resource
    TopicService topicService;

    @Resource
    TopicCommentMapper topicCommentMapper;

    @Resource
    TopicMapper topicMapper;

    @Resource
    SensitiveWordMapper sensitiveWordMapper;

    public Map<String, Object> stats() {
        Map<String, Object> map = new HashMap<>();
        map.put("users", accountService.count());
        map.put("topics", topicService.count());
        map.put("comments", topicCommentMapper.selectCount(null));
        map.put("sensitiveWords", sensitiveWordMapper.selectCount(null));

        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        map.put("todayPosts", topicService.count(
                Wrappers.<TopicDTO>query().ge("time", today + " 00:00:00")));
        map.put("todayRegistered", accountService.count(
                Wrappers.<AccountDTO>query().ge("register_time", today + " 00:00:00")));

        long todayActivePosters = topicService.count(
                Wrappers.<TopicDTO>query().ge("time", today + " 00:00:00").select("DISTINCT uid"));
        long todayActiveCommenters = topicCommentMapper.selectCount(
                Wrappers.<TopicCommentDTO>query().ge("time", today + " 00:00:00"));
        map.put("todayActive", todayActivePosters + todayActiveCommenters);
        map.put("hotTopics", hotTopics());
        map.put("dailyPosts", dailyPosts());
        map.put("categoryDistribution", categoryDistribution());
        return map;
    }

    private List<Map<String, Object>> hotTopics() {
        List<Map<String, Object>> hotTopics = new ArrayList<>();
        List<TopicDTO> recentTopics = topicService.list(
                Wrappers.<TopicDTO>query().orderByDesc("time").last("LIMIT 50"));
        for (TopicDTO t : recentTopics) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", t.getId());
            item.put("title", t.getTitle());
            item.put("likes", topicMapper.interactCount(t.getId(), "like"));
            item.put("time", t.getTime());
            hotTopics.add(item);
        }
        hotTopics.sort((a, b) -> (int) b.get("likes") - (int) a.get("likes"));
        return hotTopics.stream().limit(5).toList();
    }

    private List<Map<String, Object>> dailyPosts() {
        List<Map<String, Object>> dailyPosts = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (int i = 6; i >= 0; i--) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -i);
            String day = sdf.format(cal.getTime());
            long count = topicService.count(
                    Wrappers.<TopicDTO>query()
                            .ge("time", day + " 00:00:00")
                            .le("time", day + " 23:59:59"));
            Map<String, Object> item = new HashMap<>();
            item.put("date", day);
            item.put("count", count);
            dailyPosts.add(item);
        }
        return dailyPosts;
    }

    private List<Map<String, Object>> categoryDistribution() {
        Map<String, Integer> tagCounter = new LinkedHashMap<>();
        for (TopicDTO topic : topicService.list()) {
            try {
                JSONObject content = JSONObject.parseObject(topic.getContent());
                List<String> tags = content.getList("tags", String.class);
                tagCounter.merge(tags != null && !tags.isEmpty() ? tags.get(0) : "未分类", 1, Integer::sum);
            } catch (Exception e) {
                tagCounter.merge("未分类", 1, Integer::sum);
            }
        }
        List<Map<String, Object>> categoryDistribution = new ArrayList<>();
        tagCounter.forEach((name, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", name);
            item.put("value", count);
            categoryDistribution.add(item);
        });
        return categoryDistribution;
    }
}
