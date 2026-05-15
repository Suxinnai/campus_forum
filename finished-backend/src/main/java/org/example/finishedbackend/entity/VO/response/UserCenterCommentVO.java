package org.example.finishedbackend.entity.VO.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class UserCenterCommentVO {
    private Integer id;
    private Integer topicId;
    private String topicTitle;
    private String content;
    private String quote;
    private Date time;
    private Integer likeCount;
}
