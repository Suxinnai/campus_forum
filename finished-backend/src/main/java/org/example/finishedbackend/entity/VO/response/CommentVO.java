package org.example.finishedbackend.entity.VO.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class CommentVO {
    Integer id;
    String content;
    Date time;
    String quote;
    User user;
    Integer likeCount;
    Boolean liked;
    List<CommentVO> replies;

    @Data
    public static class User {
        Integer id;
        String username;
        String avatar;
        Integer gender;
        String qq;
        String phone;
        String email;
    }
}
