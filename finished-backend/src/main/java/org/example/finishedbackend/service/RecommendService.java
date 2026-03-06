package org.example.finishedbackend.service;

import org.example.finishedbackend.entity.VO.response.TopicPreviewVO;

import java.util.List;

public interface RecommendService {
    /**
     * 记录用户行为
     * 
     * @param uid  用户ID
     * @param tid  帖子ID
     * @param type 行为类型: view / like / collect / comment
     */
    void recordBehavior(int uid, int tid, String type);

    /**
     * 获取推荐帖子列表
     * 
     * @param uid 当前用户ID
     * @return 推荐的帖子预览列表
     */
    List<TopicPreviewVO> recommend(int uid);
}
