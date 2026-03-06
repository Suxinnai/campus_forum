package org.example.finishedbackend.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.finishedbackend.entity.DTO.ActivityDTO;
import org.example.finishedbackend.mapper.ActivityMapper;
import org.example.finishedbackend.service.ActivityService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, ActivityDTO> implements ActivityService {

    @Override
    public List<ActivityDTO> listAll() {
        return this.list(new QueryWrapper<ActivityDTO>().orderByDesc("start_time"));
    }

    @Override
    public String join(int id) {
        ActivityDTO dto = this.getById(id);
        if (dto == null)
            return "活动不存在";
        if (dto.getMaxPeople() > 0 && dto.getCurrentPeople() >= dto.getMaxPeople()) {
            return "报名人数已满";
        }
        // 使用原子操作避免并发问题
        this.update(new UpdateWrapper<ActivityDTO>()
                .eq("id", id)
                .setSql("current_people = current_people + 1"));
        return null;
    }
}
