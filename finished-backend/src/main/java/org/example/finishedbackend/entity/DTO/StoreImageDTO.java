package org.example.finishedbackend.entity.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@TableName("db_image_store")
@AllArgsConstructor
@NoArgsConstructor
public class StoreImageDTO {

    @TableId(value = "name", type = IdType.INPUT)
    String name;
    Integer uid;
    Date time;
}
