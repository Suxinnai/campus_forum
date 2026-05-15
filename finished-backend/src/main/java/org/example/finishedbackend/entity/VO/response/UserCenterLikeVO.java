package org.example.finishedbackend.entity.VO.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class UserCenterLikeVO {
    private String kind;
    private Integer id;
    private Integer topicId;
    private String topicTitle;
    private String title;
    private String content;
    private Date time;
    private Integer likeCount;
}
