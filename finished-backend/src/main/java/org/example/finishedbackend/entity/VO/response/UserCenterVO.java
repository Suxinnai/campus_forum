package org.example.finishedbackend.entity.VO.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UserCenterVO {
    private AccountVO account;
    private AccountDetailsVO details;
    private UserCenterStatsVO stats;
    private List<TopicPreviewVO> posts;
    private List<TopicPreviewVO> bookmarks;
}
