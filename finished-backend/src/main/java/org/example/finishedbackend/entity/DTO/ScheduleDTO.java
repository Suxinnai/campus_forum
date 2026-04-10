package org.example.finishedbackend.entity.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("db_schedule")
public class ScheduleDTO {
    @TableId(type = IdType.AUTO)
    Integer id;
    String title;
    String description;
    @TableField("event_date")
    Date eventDate;
    @TableField("end_date")
    Date endDate;
    String type;
    @TableField("create_time")
    Date createTime;
}
