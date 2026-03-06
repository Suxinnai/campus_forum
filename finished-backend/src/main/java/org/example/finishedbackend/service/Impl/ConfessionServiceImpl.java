package org.example.finishedbackend.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.example.finishedbackend.entity.DTO.AccountDTO;
import org.example.finishedbackend.entity.DTO.ConfessionDTO;
import org.example.finishedbackend.mapper.ConfessionMapper;
import org.example.finishedbackend.service.AccountService;
import org.example.finishedbackend.service.ConfessionService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ConfessionServiceImpl extends ServiceImpl<ConfessionMapper, ConfessionDTO> implements ConfessionService {

    @Resource
    AccountService accountService;

    @Override
    public List<Map<String, Object>> listByPage(int page) {
        QueryWrapper<ConfessionDTO> qw = new QueryWrapper<>();
        qw.orderByDesc("create_time");
        Page<ConfessionDTO> result = this.page(new Page<>(page, 10), qw);
        List<Map<String, Object>> list = new ArrayList<>();
        for (ConfessionDTO dto : result.getRecords()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", dto.getId());
            map.put("content", dto.getContent());
            map.put("anonymous", dto.getAnonymous());
            map.put("likes", dto.getLikes());
            map.put("createTime", dto.getCreateTime());
            if (dto.getAnonymous() == 0) {
                AccountDTO account = accountService.getById(dto.getUid());
                map.put("username", account != null ? account.getUsername() : "未知用户");
            } else {
                map.put("username", "匿名用户");
            }
            list.add(map);
        }
        return list;
    }

    @Override
    public String publish(int uid, ConfessionDTO dto) {
        dto.setUid(uid);
        dto.setLikes(0);
        dto.setCreateTime(new Date());
        return this.save(dto) ? null : "发布失败";
    }

    @Override
    public String like(int id) {
        ConfessionDTO dto = this.getById(id);
        if (dto == null)
            return "不存在";
        this.update(new UpdateWrapper<ConfessionDTO>()
                .eq("id", id)
                .setSql("likes = likes + 1"));
        return null;
    }
}
