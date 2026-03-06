package org.example.finishedbackend.entity.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("db_book")
public class BookDTO {
    @TableId(type = IdType.AUTO)
    Integer id;
    String title;
    String author;
    String isbn;
    String category;
    String description;
    @TableField("cover_url")
    String coverUrl;
    String location;
    Integer available;
    @TableField("create_time")
    Date createTime;
}
