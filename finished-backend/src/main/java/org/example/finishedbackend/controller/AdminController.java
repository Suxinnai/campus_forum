package org.example.finishedbackend.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.finishedbackend.entity.DTO.*;
import org.example.finishedbackend.entity.RestBean;
import org.example.finishedbackend.mapper.SensitiveWordMapper;
import org.example.finishedbackend.mapper.TopicCommentMapper;
import org.example.finishedbackend.service.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Resource AccountService accountService;
    @Resource TopicService topicService;
    @Resource TopicCommentMapper topicCommentMapper;
    @Resource NoticeService noticeService;
    @Resource ActivityService activityService;
    @Resource ResourceService resourceService;
    @Resource SensitiveWordMapper sensitiveWordMapper;

    // ================== 数据总览 ==================

    @GetMapping("/stats")
    public RestBean<Map<String, Object>> stats() {
        Map<String, Object> map = new HashMap<>();
        map.put("users", accountService.count());
        map.put("topics", topicService.count());
        map.put("comments", topicCommentMapper.selectCount(null));
        map.put("sensitiveWords", sensitiveWordMapper.selectCount(null));
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
        long todayPosts = topicService.count(
            Wrappers.<TopicDTO>query().ge("time", today + " 00:00:00")
        );
        map.put("todayPosts", todayPosts);
        return RestBean.success(map, null);
    }

    // ================== 用户管理 ==================

    @GetMapping("/users")
    public RestBean<Map<String, Object>> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword) {
        Page<AccountDTO> pageObj = new Page<>(page + 1, 10);
        var wrapper = Wrappers.<AccountDTO>query();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("username", keyword).or().like("email", keyword);
        }
        wrapper.orderByDesc("register_time");
        accountService.page(pageObj, wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageObj.getRecords());
        result.put("total", pageObj.getTotal());
        return RestBean.success(result, null);
    }

    @DeleteMapping("/user")
    public RestBean<Void> deleteUser(@RequestParam int id) {
        AccountDTO acc = accountService.findAccountById(id);
        if (acc == null) return RestBean.failure(404, "用户不存在");
        if ("admin".equals(acc.getRole())) return RestBean.failure(403, "不能删除管理员");
        accountService.removeById(id);
        return RestBean.success(null, "删除成功");
    }

    // ================== 帖子管理 ==================

    @GetMapping("/topics")
    public RestBean<Map<String, Object>> topics(@RequestParam(defaultValue = "0") int page) {
        Page<TopicDTO> pageObj = new Page<>(page + 1, 10);
        topicService.page(pageObj, Wrappers.<TopicDTO>query().orderByDesc("top", "time"));
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageObj.getRecords());
        result.put("total", pageObj.getTotal());
        return RestBean.success(result, null);
    }

    @DeleteMapping("/topic")
    public RestBean<Void> deleteTopic(@RequestParam int id,
                                       @RequestAttribute("id") int uid) {
        String result = topicService.deleteTopic(uid, id);
        return result == null ? RestBean.success(null, "删除成功") : RestBean.failure(400, result);
    }

    @PostMapping("/topic/top")
    public RestBean<Void> toggleTopTopic(@RequestParam int id, @RequestParam int top) {
        topicService.update(Wrappers.<TopicDTO>update().eq("id", id).set("top", top));
        return RestBean.success(null, top == 1 ? "置顶成功" : "取消置顶成功");
    }

    // ================== 公告管理 ==================

    @GetMapping("/notices")
    public RestBean<List<NoticeDTO>> notices() {
        return RestBean.success(noticeService.listAll(), null);
    }

    @PostMapping("/notice/add")
    public RestBean<Void> addNotice(@RequestBody NoticeDTO dto) {
        dto.setCreateTime(new Date());
        noticeService.save(dto);
        return RestBean.success(null, "公告发布成功");
    }

    @DeleteMapping("/notice/delete")
    public RestBean<Void> deleteNotice(@RequestParam int id) {
        noticeService.removeById(id);
        return RestBean.success(null, "公告已删除");
    }

    // ================== 校历/活动管理 ==================

    @GetMapping("/activities")
    public RestBean<List<ActivityDTO>> activities() {
        return RestBean.success(activityService.listAll(), null);
    }

    @PostMapping("/activity/add")
    public RestBean<Void> addActivity(@RequestBody ActivityDTO dto) {
        dto.setCreateTime(new Date());
        if (dto.getCurrentPeople() == null) dto.setCurrentPeople(0);
        activityService.save(dto);
        return RestBean.success(null, "活动添加成功");
    }

    @PutMapping("/activity/update")
    public RestBean<Void> updateActivity(@RequestBody ActivityDTO dto) {
        activityService.updateById(dto);
        return RestBean.success(null, "活动更新成功");
    }

    @DeleteMapping("/activity/delete")
    public RestBean<Void> deleteActivity(@RequestParam int id) {
        activityService.removeById(id);
        return RestBean.success(null, "活动已删除");
    }

    // ================== 资源管理 ==================

    @GetMapping("/resources")
    public RestBean<Map<String, Object>> resources(@RequestParam(defaultValue = "0") int page) {
        Page<ResourceDTO> pageObj = new Page<>(page + 1, 15);
        resourceService.page(pageObj, Wrappers.<ResourceDTO>query().orderByDesc("create_time"));
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageObj.getRecords());
        result.put("total", pageObj.getTotal());
        return RestBean.success(result, null);
    }

    @DeleteMapping("/resource/delete")
    public RestBean<Void> deleteResource(@RequestParam int id) {
        resourceService.removeById(id);
        return RestBean.success(null, "资源已删除");
    }

    // ================== 敏感词管理 ==================

    @GetMapping("/sensitive-words")
    public RestBean<List<SensitiveWordDTO>> sensitiveWords() {
        return RestBean.success(sensitiveWordMapper.selectList(null), null);
    }

    @PostMapping("/sensitive-word/add")
    public RestBean<Void> addSensitiveWord(@RequestParam String word) {
        if (word == null || word.isBlank()) return RestBean.failure(400, "词语不能为空");
        long exists = sensitiveWordMapper.selectCount(Wrappers.<SensitiveWordDTO>query().eq("word", word.trim()));
        if (exists > 0) return RestBean.failure(400, "该词语已存在");
        SensitiveWordDTO dto = new SensitiveWordDTO();
        dto.setWord(word.trim());
        sensitiveWordMapper.insert(dto);
        return RestBean.success(null, "添加成功");
    }

    @DeleteMapping("/sensitive-word/delete")
    public RestBean<Void> deleteSensitiveWord(@RequestParam int id) {
        sensitiveWordMapper.deleteById(id);
        return RestBean.success(null, "删除成功");
    }
}
