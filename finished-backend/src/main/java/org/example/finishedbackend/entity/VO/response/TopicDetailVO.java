package org.example.finishedbackend.entity.VO.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopicDetailVO {
    Integer id;
    String title;
    String content;
    String rawContent;
    Integer type;
    Date time;
    User user;
    Interact interact;
    Long comments;
    int like;
    int collect;
    java.util.List<String> tags;
    java.util.List<String> images;

    @Data
    @AllArgsConstructor
    public static class Interact {
        Boolean like;
        Boolean collect;
    }

    @Data
    public static class User {
        Integer id;
        String username;
        String avatar;
        String desc;
        Integer gender;
        String qq;
        String phone;
        String email;
    }
}
