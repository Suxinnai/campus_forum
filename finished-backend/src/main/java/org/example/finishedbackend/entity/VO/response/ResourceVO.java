package org.example.finishedbackend.entity.VO.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class ResourceVO {
    Integer id;
    String title;
    String category;
    String fileName;
    Long fileSize;
    Integer downloadCount;
    String description;
    String uploaderName;
    String uploaderAvatar;
    Date createTime;
}
