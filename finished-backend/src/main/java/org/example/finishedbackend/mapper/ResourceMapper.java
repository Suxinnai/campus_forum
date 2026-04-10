package org.example.finishedbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.example.finishedbackend.entity.DTO.ResourceDTO;

import java.util.List;

@Mapper
public interface ResourceMapper extends BaseMapper<ResourceDTO> {

    @Insert("INSERT IGNORE INTO db_resource_collect (rid, uid, time) VALUES (#{rid}, #{uid}, NOW())")
    void addCollect(@Param("rid") int rid, @Param("uid") int uid);

    @Delete("DELETE FROM db_resource_collect WHERE rid = #{rid} AND uid = #{uid}")
    int deleteCollect(@Param("rid") int rid, @Param("uid") int uid);

    @Select("SELECT COUNT(*) FROM db_resource_collect WHERE rid = #{rid} AND uid = #{uid}")
    int userCollectCount(@Param("rid") int rid, @Param("uid") int uid);

    @Select("SELECT r.* FROM db_resource r INNER JOIN db_resource_collect c ON r.id = c.rid WHERE c.uid = #{uid} ORDER BY c.time DESC")
    List<ResourceDTO> collectResources(@Param("uid") int uid);
}
