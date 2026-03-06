package org.example.finishedbackend.entity.VO.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 分类统计 VO（饼图数据点）
 */
@Data
@AllArgsConstructor
public class CategoryStatVO {
    String name;
    Integer value;
}
