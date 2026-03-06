package org.example.finishedbackend.entity.VO.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 热度趋势 VO（折线图数据点）
 */
@Data
@AllArgsConstructor
public class HotTrendVO {
    String date;
    Integer count;
}
