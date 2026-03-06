package org.example.finishedbackend.entity.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("db_activity")
public class ActivityDTO {
    @TableId(type = IdType.AUTO)
    Integer id;
    String title;
    String content;
    String location;
    @TableField("start_time")
    Date startTime;
    @TableField("end_time")
    Date endTime;
    String organizer;
    @TableField("max_people")
    Integer maxPeople;
    @TableField("current_people")
    Integer currentPeople;
    String image;
    @TableField("create_time")
    Date createTime;
}
