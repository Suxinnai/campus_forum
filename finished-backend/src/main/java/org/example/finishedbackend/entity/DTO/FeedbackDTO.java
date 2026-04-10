package org.example.finishedbackend.entity.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("db_feedback")
public class FeedbackDTO {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer uid;
    String type;
    String title;
    String content;
    String contact;
    String status;
    Date createTime;
}
