package org.example.finishedbackend.entity.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("db_comment_report")
public class CommentReportDTO {
    @TableId(type = IdType.AUTO)
    Integer id;
    
    @TableField("comment_id")
    Integer commentId;
    
    @TableField("reporter_uid")
    Integer reporterUid;
    
    String reason;
    String status;
    Date time;
    
    @TableField("handler_uid")
    Integer handlerUid;
    
    @TableField("handle_time")
    Date handleTime;
    
    @TableField("handle_action")
    String handleAction;
}
