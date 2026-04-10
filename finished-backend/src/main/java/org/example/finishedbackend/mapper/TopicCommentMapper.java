package org.example.finishedbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.finishedbackend.entity.DTO.TopicCommentDTO;

@Mapper
public interface TopicCommentMapper extends BaseMapper<TopicCommentDTO> {
    @Select("""
            select coalesce(sum(like_count), 0)
            from db_topic_comment
            where uid = #{uid}
            """)
    Integer sumReceivedLikeCount(int uid);
}
