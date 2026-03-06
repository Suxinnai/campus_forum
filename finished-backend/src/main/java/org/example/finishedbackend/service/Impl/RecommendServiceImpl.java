package org.example.finishedbackend.service.Impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.finishedbackend.entity.DTO.TopicDTO;
import org.example.finishedbackend.entity.DTO.UserBehaviorDTO;
import org.example.finishedbackend.entity.VO.response.TopicPreviewVO;
import org.example.finishedbackend.mapper.BehaviorMapper;
import org.example.finishedbackend.service.RecommendService;
import org.example.finishedbackend.service.TopicService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于用户的协同过滤推荐实现
 */
@Slf4j
@Service
public class RecommendServiceImpl implements RecommendService {

    @Resource
    BehaviorMapper behaviorMapper;

    @Resource
    TopicService topicService;

    // 行为类型 -> 权重分值映射
    private static final Map<String, Integer> BEHAVIOR_SCORES = Map.of(
            "view", 1,
            "like", 3,
            "comment", 4,
            "collect", 5);

    @Override
    public void recordBehavior(int uid, int tid, String type) {
        Integer score = BEHAVIOR_SCORES.getOrDefault(type, 1);
        // 使用 INSERT ... ON DUPLICATE KEY UPDATE 来避免重复插入
        UserBehaviorDTO existing = behaviorMapper.selectOne(
                Wrappers.<UserBehaviorDTO>query()
                        .eq("uid", uid)
                        .eq("tid", tid)
                        .eq("type", type));
        if (existing != null) {
            // 已有相同行为记录，更新时间（不重复计分）
            existing.setCreateTime(new Date());
            behaviorMapper.updateById(existing);
        } else {
            UserBehaviorDTO dto = new UserBehaviorDTO();
            dto.setUid(uid);
            dto.setTid(tid);
            dto.setType(type);
            dto.setScore(score);
            dto.setCreateTime(new Date());
            behaviorMapper.insert(dto);
        }
    }

    @Override
    public List<TopicPreviewVO> recommend(int uid) {
        // 1. 获取当前用户的行为向量
        Map<Integer, Double> userVector = getUserVector(uid);

        // 2. 若行为不足（新用户），降级为热度推荐
        if (userVector.size() < 3) {
            log.info("用户 {} 行为记录不足, 降级为热度推荐", uid);
            return topicService.listTopTopics().stream()
                    .map(top -> {
                        TopicDTO dto = topicService.getById(top.getId());
                        if (dto == null)
                            return null;
                        return topicService.listTopicByPage(0, 0);
                    })
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .distinct()
                    .limit(10)
                    .collect(Collectors.toList());
        }

        // 3. 获取所有活跃用户ID
        List<Integer> allUserIds = behaviorMapper.selectAllActiveUserIds();

        // 4. 计算与每个用户的相似度
        Map<Integer, Double> similarityMap = new HashMap<>();
        for (Integer otherId : allUserIds) {
            if (otherId.equals(uid))
                continue;
            Map<Integer, Double> otherVector = getUserVector(otherId);
            double sim = cosineSimilarity(userVector, otherVector);
            if (sim > 0) {
                similarityMap.put(otherId, sim);
            }
        }

        // 5. 按相似度排序，取Top 10相似用户
        List<Map.Entry<Integer, Double>> topSimilarUsers = similarityMap.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(10)
                .toList();

        // 6. 收集相似用户喜欢但当前用户未看过的帖子
        Set<Integer> userSeenTopics = userVector.keySet();
        Map<Integer, Double> candidateScores = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : topSimilarUsers) {
            Map<Integer, Double> otherVector = getUserVector(entry.getKey());
            double similarity = entry.getValue();
            for (Map.Entry<Integer, Double> topicEntry : otherVector.entrySet()) {
                if (!userSeenTopics.contains(topicEntry.getKey())) {
                    candidateScores.merge(topicEntry.getKey(),
                            topicEntry.getValue() * similarity, Double::sum);
                }
            }
        }

        // 7. 按综合得分排序，取Top 10
        List<Integer> recommendIds = candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .toList();

        // 8. 查询帖子详情并返回
        if (recommendIds.isEmpty()) {
            // 无推荐结果时也降级为热度推荐
            return topicService.listTopicByPage(0, 0);
        }

        return recommendIds.stream()
                .map(tid -> {
                    TopicDTO dto = topicService.getById(tid);
                    if (dto == null)
                        return null;
                    // 复用 listTopicByPage 的预览转换逻辑
                    return topicService.listTopicByPage(0, 0).stream()
                            .filter(p -> p.getId() == tid)
                            .findFirst()
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的行为向量（tid -> 总分）
     */
    private Map<Integer, Double> getUserVector(int uid) {
        List<UserBehaviorDTO> behaviors = behaviorMapper.selectUserVector(uid);
        Map<Integer, Double> vector = new HashMap<>();
        for (UserBehaviorDTO b : behaviors) {
            vector.put(b.getTid(), b.getScore() != null ? b.getScore().doubleValue() : 0.0);
        }
        return vector;
    }

    /**
     * 计算两个向量的余弦相似度
     */
    private double cosineSimilarity(Map<Integer, Double> v1, Map<Integer, Double> v2) {
        Set<Integer> commonKeys = new HashSet<>(v1.keySet());
        commonKeys.retainAll(v2.keySet());
        if (commonKeys.isEmpty())
            return 0.0;

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (Integer key : commonKeys) {
            dotProduct += v1.get(key) * v2.get(key);
        }
        for (Double val : v1.values()) {
            norm1 += val * val;
        }
        for (Double val : v2.values()) {
            norm2 += val * val;
        }
        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        return denominator == 0 ? 0 : dotProduct / denominator;
    }
}
