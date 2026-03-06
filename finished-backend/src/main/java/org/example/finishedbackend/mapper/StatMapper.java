package org.example.finishedbackend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.finishedbackend.entity.VO.response.CategoryStatVO;
import org.example.finishedbackend.entity.VO.response.HotTrendVO;

import java.util.List;

@Mapper
public interface StatMapper {
    /**
     * 查询近7天每天的发帖数量
     */
    @Select("""
            SELECT DATE_FORMAT(time, '%Y-%m-%d') AS date, COUNT(*) AS count
            FROM db_topic
            WHERE time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
            GROUP BY DATE_FORMAT(time, '%Y-%m-%d')
            ORDER BY date ASC
            """)
    List<HotTrendVO> selectHotTrend();

    /**
     * 查询各帖子分类的数量
     */
    @Select("""
            SELECT t2.name AS name, COUNT(*) AS value
            FROM db_topic t1
            LEFT JOIN db_topic_type t2 ON t1.type = t2.id
            GROUP BY t1.type, t2.name
            """)
    List<CategoryStatVO> selectCategoryStat();

    /**
     * 查询最近200条帖子的标题（用于后端分词统计）
     */
    @Select("""
            SELECT title FROM db_topic ORDER BY time DESC LIMIT 200
            """)
    List<String> selectRecentTitles();
}
