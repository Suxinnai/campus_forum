package org.example.finishedbackend.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.finishedbackend.entity.DTO.LostFoundDTO;
import org.example.finishedbackend.mapper.LostFoundMapper;
import org.example.finishedbackend.service.LostFoundService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class LostFoundServiceImpl extends ServiceImpl<LostFoundMapper, LostFoundDTO> implements LostFoundService {

    @Override
    public List<LostFoundDTO> listByPage(int page, String type) {
        QueryWrapper<LostFoundDTO> qw = new QueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            qw.eq("type", type);
        }
        qw.orderByDesc("create_time");
        return this.page(new Page<>(page, 10), qw).getRecords();
    }

    @Override
    public String publish(int uid, LostFoundDTO dto) {
        dto.setUid(uid);
        dto.setStatus("open");
        dto.setCreateTime(new Date());
        return this.save(dto) ? null : "发布失败";
    }

    @Override
    public String close(int id, int uid) {
        LostFoundDTO dto = this.getById(id);
        if (dto != null && dto.getUid().equals(uid)) {
            dto.setStatus("closed");
            this.updateById(dto);
            return null;
        }
        return "操作失败";
    }
}
