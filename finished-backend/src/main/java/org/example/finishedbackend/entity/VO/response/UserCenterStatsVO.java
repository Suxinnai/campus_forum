package org.example.finishedbackend.entity.VO.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserCenterStatsVO {
    private int posts;
    private int comments;
    private int likes;
    private int bookmarks;
}
