package org.example.finishedbackend.entity.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("db_confession")
public class ConfessionDTO {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer uid;
    String content;
    Integer anonymous;
    Integer likes;
    @TableField("create_time")
    Date createTime;
}
