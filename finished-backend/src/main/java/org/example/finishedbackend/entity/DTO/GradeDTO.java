package org.example.finishedbackend.entity.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("db_grade")
public class GradeDTO {
    @TableId(type = IdType.AUTO)
    Integer id;
    @TableField("student_id")
    String studentId;
    @TableField("course_name")
    String courseName;
    BigDecimal score;
    String semester;
    @TableField("create_time")
    Date createTime;
}
