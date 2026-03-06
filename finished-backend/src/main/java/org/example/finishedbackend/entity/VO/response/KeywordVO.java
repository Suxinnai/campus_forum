package org.example.finishedbackend.entity.VO.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 关键词词频 VO（词云数据点）
 */
@Data
@AllArgsConstructor
public class KeywordVO {
    String name;
    Integer value;
}
