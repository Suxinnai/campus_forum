package org.example.finishedbackend.entity.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("db_sensitive_word")
public class SensitiveWordDTO {
    @TableId(type = IdType.AUTO)
    Integer id;
    String word;
    String type;
}
