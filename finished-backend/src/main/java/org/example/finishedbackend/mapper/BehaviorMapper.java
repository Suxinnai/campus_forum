package org.example.finishedbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.finishedbackend.entity.DTO.UserBehaviorDTO;

import java.util.List;

@Mapper
public interface BehaviorMapper extends BaseMapper<UserBehaviorDTO> {

    /**
     * 获取所有有行为记录的用户ID
     */
    @Select("SELECT DISTINCT uid FROM db_user_behavior")
    List<Integer> selectAllActiveUserIds();

    /**
     * 获取某用户的行为向量（tid -> 总得分）
     */
    @Select("""
            SELECT tid, SUM(score) as score
            FROM db_user_behavior
            WHERE uid = #{uid}
            GROUP BY tid
            """)
    List<UserBehaviorDTO> selectUserVector(int uid);
}
