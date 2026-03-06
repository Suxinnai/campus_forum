package org.example.finishedbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.finishedbackend.entity.DTO.NoticeDTO;

@Mapper
public interface NoticeMapper extends BaseMapper<NoticeDTO> {
}
