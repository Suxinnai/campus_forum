package org.example.finishedbackend.entity.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("db_resource")
public class ResourceDTO {
    @TableId(type = IdType.AUTO)
    Integer id;
    String title;
    String category;
    String fileUrl;
    String fileName;
    Long fileSize;
    Integer uploaderId;
    Integer downloadCount;
    String description;
    Date createTime;
}
