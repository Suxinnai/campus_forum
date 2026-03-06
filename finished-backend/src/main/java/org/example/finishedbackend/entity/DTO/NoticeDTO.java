package org.example.finishedbackend.entity.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("db_notice")
public class NoticeDTO {
    @TableId(type = IdType.AUTO)
    Integer id;
    String title;
    String content;
    String publisher;
    @TableField("is_top")
    Integer isTop;
    @TableField("create_time")
    Date createTime;
}
