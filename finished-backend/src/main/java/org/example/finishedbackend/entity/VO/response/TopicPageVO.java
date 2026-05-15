package org.example.finishedbackend.entity.VO.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TopicPageVO {
    private List<TopicPreviewVO> records;
    private long page;
    private long size;
    private long total;
    private long pages;
}
