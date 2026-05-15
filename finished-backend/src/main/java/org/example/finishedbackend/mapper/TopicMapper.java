package org.example.finishedbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.example.finishedbackend.entity.DTO.Interact;
import org.example.finishedbackend.entity.DTO.TopicDTO;

import java.util.List;

@Mapper
public interface TopicMapper extends BaseMapper<TopicDTO> {
    @Insert("""
            <script>
                insert ignore into db_topic_interact_${type} values
                <foreach collection="interacts" item="item" separator=','>
                    (#{item.tid}, #{item.uid}, #{item.time})
                </foreach>
            </script>
            """)
    void addInteract(List<Interact> interacts, String type);

    @Delete("""
            <script>
                delete from db_topic_interact_${type} where
                <foreach collection="interacts" item="item" separator=" or ">
                    (tid = #{item.tid} and uid = #{item.uid})
                </foreach>
            </script>
            """)
    int deleteInteract(List<Interact> interacts, String type);

    @Select("""
            select count(*) from db_topic_interact_${type} where tid = #{tid}
            """)
    int interactCount(int tid, String type);

    @Select("""
            select count(*) from db_topic_interact_${type} where tid = #{tid} and uid = #{uid}
            """)
     int userInteractCount(int tid, int uid, String type);

    @Select("""
            select count(*)
            from db_topic_interact_like l
            inner join db_topic t on l.tid = t.id
            where t.uid = #{uid}
            """)
    int receivedLikeCount(int uid);

    @Select("""
            select count(*) from db_topic_interact_collect where uid = #{uid}
            """)
    int collectCountByUid(int uid);

    @Select("""
            select db_topic.* from db_topic_interact_collect left join db_topic on tid = db_topic.id
            where db_topic_interact_collect.uid = #{uid}
              and db_topic.status = 'approved'
            """)
    List<TopicDTO> collectTopics(int uid);

    @Select("""
            select t.*,
                   (
                       coalesce(v.views, 0) * 1
                       + coalesce(l.likes, 0) * 5
                       + coalesce(c.comments, 0) * 8
                       + coalesce(f.collects, 0) * 10
                       + if(t.featured = 1, 8, 0)
                       + if(t.top = 1, 5, 0)
                   ) / pow(timestampdiff(hour, t.time, now()) + 2, 0.25) as hot_score
            from db_topic t
            left join (
                select tid, count(*) views from db_user_behavior where type = 'view' group by tid
            ) v on v.tid = t.id
            left join (
                select tid, count(*) likes from db_topic_interact_like group by tid
            ) l on l.tid = t.id
            left join (
                select tid, count(*) collects from db_topic_interact_collect group by tid
            ) f on f.tid = t.id
            left join (
                select tid, count(*) comments from db_topic_comment group by tid
            ) c on c.tid = t.id
            where t.status = 'approved'
            order by hot_score desc, t.time desc
            """)
    List<TopicDTO> selectHotTopics();

    @Select("""
            select t.*,
                   (
                       coalesce(v.views, 0) * 1
                       + coalesce(l.likes, 0) * 5
                       + coalesce(c.comments, 0) * 8
                       + coalesce(f.collects, 0) * 10
                       + if(t.featured = 1, 8, 0)
                       + if(t.top = 1, 5, 0)
                   ) / pow(timestampdiff(hour, t.time, now()) + 2, 0.25) as hot_score
            from db_topic t
            left join (
                select tid, count(*) views from db_user_behavior where type = 'view' group by tid
            ) v on v.tid = t.id
            left join (
                select tid, count(*) likes from db_topic_interact_like group by tid
            ) l on l.tid = t.id
            left join (
                select tid, count(*) collects from db_topic_interact_collect group by tid
            ) f on f.tid = t.id
            left join (
                select tid, count(*) comments from db_topic_comment group by tid
            ) c on c.tid = t.id
            where t.status = 'approved'
            order by hot_score desc, t.time desc
            limit #{limit}
            """)
    List<TopicDTO> selectTopHotTopics(int limit);

    @Select("""
            select count(*) from db_user_behavior where tid = #{tid} and type = 'view'
            """)
    int viewCount(int tid);
}
