package org.example.finishedbackend.entity.DTO;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("db_comment_like")
public class CommentLikeDTO {
    Integer commentId;
    Integer uid;
    Date time;
}
