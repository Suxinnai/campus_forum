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

import org.example.finishedbackend.mapper.TopicMapper;
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
    @Resource TopicMapper topicMapper;
    @Resource SensitiveWordMapper sensitiveWordMapper;
    @Resource org.example.finishedbackend.mapper.FeedbackMapper feedbackMapper;
    @Resource org.example.finishedbackend.mapper.AccountMapper accountMapper;
    @Resource ScheduleService scheduleService;

    // ================== 权限辅助 ==================

    /** 获取当前操作者的 AccountDTO */
    private AccountDTO currentAccount(int uid) {
        return accountService.findAccountById(uid);
    }

    /** 当前用户是否为超级管理员 */
    private boolean isAdmin(int uid) {
        AccountDTO acc = currentAccount(uid);
        return acc != null && "admin".equals(acc.getRole());
    }

    /**
     * 检查当前用户对指定帖子是否有操作权限：
     *   admin        → 全部帖子
     *   content_admin → 全部帖子
     *   moderator    → 仅其 moderatorType 对应版块
     */
    private boolean canManageTopic(int uid, TopicDTO topic) {
        AccountDTO acc = currentAccount(uid);
        if (acc == null) return false;
        String role = acc.getRole();
        if ("admin".equals(role) || "content_admin".equals(role)) return true;
        if ("moderator".equals(role)) {
            return acc.getModeratorType() != null && acc.getModeratorType().equals(topic.getType());
        }
        return false;
    }

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
        // 今日注册用户
        long todayRegistered = accountService.count(
            Wrappers.<AccountDTO>query().ge("register_time", today + " 00:00:00")
        );
        map.put("todayRegistered", todayRegistered);
        // 今日活跃用户（今日发帖或评论的去重用户数）
        long todayActivePosters = topicService.count(
            Wrappers.<TopicDTO>query().ge("time", today + " 00:00:00").select("DISTINCT uid")
        );
        long todayActiveCommenters = topicCommentMapper.selectCount(
            Wrappers.<TopicCommentDTO>query().ge("time", today + " 00:00:00")
        );
        map.put("todayActive", todayActivePosters + todayActiveCommenters);
        // 热门帖子 Top5（按点赞数）
        List<Map<String, Object>> hotTopics = new ArrayList<>();
        List<TopicDTO> recentTopics = topicService.list(
            Wrappers.<TopicDTO>query().orderByDesc("time").last("LIMIT 50")
        );
        for (TopicDTO t : recentTopics) {
            int likes = topicMapper.interactCount(t.getId(), "like");
            Map<String, Object> item = new HashMap<>();
            item.put("id", t.getId());
            item.put("title", t.getTitle());
            item.put("likes", likes);
            item.put("time", t.getTime());
            hotTopics.add(item);
        }
        hotTopics.sort((a, b) -> (int) b.get("likes") - (int) a.get("likes"));
        map.put("hotTopics", hotTopics.stream().limit(5).toList());
        // 最近7天每日发帖量
        List<Map<String, Object>> dailyPosts = new ArrayList<>();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        for (int i = 6; i >= 0; i--) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.add(java.util.Calendar.DAY_OF_MONTH, -i);
            String day = sdf.format(cal.getTime());
            long count = topicService.count(
                Wrappers.<TopicDTO>query()
                    .ge("time", day + " 00:00:00")
                    .le("time", day + " 23:59:59")
            );
            Map<String, Object> item = new HashMap<>();
            item.put("date", day);
            item.put("count", count);
            dailyPosts.add(item);
        }
        map.put("dailyPosts", dailyPosts);
        // 帖子标签分布（饼图数据）— 基于content中的tags字段统计
        List<TopicDTO> allTopics = topicService.list();
        Map<String, Integer> tagCounter = new LinkedHashMap<>();
        for (TopicDTO topic : allTopics) {
            try {
                com.alibaba.fastjson2.JSONObject content = com.alibaba.fastjson2.JSONObject.parseObject(topic.getContent());
                List<String> tags = content.getList("tags", String.class);
                if (tags != null && !tags.isEmpty()) {
                    // 只统计第一个tag（主分类）
                    tagCounter.merge(tags.get(0), 1, Integer::sum);
                } else {
                    tagCounter.merge("未分类", 1, Integer::sum);
                }
            } catch (Exception e) {
                tagCounter.merge("未分类", 1, Integer::sum);
            }
        }
        List<Map<String, Object>> categoryDistribution = new ArrayList<>();
        tagCounter.forEach((name, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", name);
            item.put("value", count);
            categoryDistribution.add(item);
        });
        map.put("categoryDistribution", categoryDistribution);
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
    public RestBean<Void> deleteUser(@RequestParam int id, @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        AccountDTO acc = accountService.findAccountById(id);
        if (acc == null) return RestBean.failure(404, "用户不存在");
        if ("admin".equals(acc.getRole())) return RestBean.failure(403, "不能删除超级管理员");
        accountService.removeById(id);
        return RestBean.success(null, "删除成功");
    }

    @PostMapping("/user/ban")
    public RestBean<Void> banUser(@RequestParam int id, @RequestParam boolean banned,
                                  @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        AccountDTO acc = accountService.findAccountById(id);
        if (acc == null) return RestBean.failure(404, "用户不存在");
        if ("admin".equals(acc.getRole())) return RestBean.failure(403, "不能封禁超级管理员");
        accountService.update(Wrappers.<AccountDTO>update().eq("id", id).set("banned", banned ? 1 : 0));
        return RestBean.success(null, banned ? "用户已封禁" : "用户已解封");
    }

    @PostMapping("/user/role")
    public RestBean<Void> changeRole(@RequestParam int id, @RequestParam String role,
                                     @RequestParam(required = false) Integer moderatorType,
                                     @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        if ("admin".equals(role)) return RestBean.failure(403, "超级管理员不能通过此接口设置");
        AccountDTO acc = accountService.findAccountById(id);
        if (acc == null) return RestBean.failure(404, "用户不存在");
        if ("admin".equals(acc.getRole())) return RestBean.failure(403, "不能修改超级管理员的角色");
        var update = Wrappers.<AccountDTO>update().eq("id", id).set("role", role);
        if ("moderator".equals(role) && moderatorType != null) {
            update.set("moderator_type", moderatorType);
        } else if (!"moderator".equals(role)) {
            update.set("moderator_type", null);
        }
        accountService.update(update);
        return RestBean.success(null, "角色已更新");
    }

    @PostMapping("/user/batch-delete")
    public RestBean<Void> batchDeleteUsers(@RequestBody List<Integer> ids,
                                           @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        if (ids == null || ids.isEmpty()) return RestBean.failure(400, "请选择用户");
        for (Integer id : ids) {
            AccountDTO acc = accountService.findAccountById(id);
            if (acc != null && !"admin".equals(acc.getRole())) {
                accountService.removeById(id);
            }
        }
        return RestBean.success(null, "批量删除成功");
    }

    @PostMapping("/user/batch-ban")
    public RestBean<Void> batchBanUsers(@RequestBody List<Integer> ids, @RequestParam boolean banned,
                                        @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        if (ids == null || ids.isEmpty()) return RestBean.failure(400, "请选择用户");
        for (Integer id : ids) {
            AccountDTO acc = accountService.findAccountById(id);
            if (acc != null && !"admin".equals(acc.getRole())) {
                accountService.update(Wrappers.<AccountDTO>update().eq("id", id).set("banned", banned ? 1 : 0));
            }
        }
        return RestBean.success(null, banned ? "批量封禁成功" : "批量解封成功");
    }

    // ================== 帖子管理 ==================

    @GetMapping("/topics")
    public RestBean<Map<String, Object>> topics(@RequestParam(defaultValue = "0") int page,
                                                @RequestAttribute("id") int uid) {
        AccountDTO acc = currentAccount(uid);
        Page<TopicDTO> pageObj = new Page<>(page + 1, 10);
        var wrapper = Wrappers.<TopicDTO>query().orderByDesc("top", "time");
        // 版主只看自己负责版块的帖子
        if (acc != null && "moderator".equals(acc.getRole()) && acc.getModeratorType() != null) {
            wrapper.eq("type", acc.getModeratorType());
        }
        topicService.page(pageObj, wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageObj.getRecords());
        result.put("total", pageObj.getTotal());
        return RestBean.success(result, null);
    }

    @DeleteMapping("/topic")
    public RestBean<Void> deleteTopic(@RequestParam int id, @RequestAttribute("id") int uid) {
        TopicDTO topic = topicService.getById(id);
        if (topic == null) return RestBean.failure(404, "帖子不存在");
        if (!canManageTopic(uid, topic)) return RestBean.failure(403, "无权操作此版块的帖子");
        String result = topicService.deleteTopic(uid, id);
        return result == null ? RestBean.success(null, "删除成功") : RestBean.failure(400, result);
    }

    @PostMapping("/topic/top")
    public RestBean<Void> toggleTopTopic(@RequestParam int id, @RequestParam int top,
                                          @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限，置顶操作仅超级管理员可用");
        topicService.update(Wrappers.<TopicDTO>update().eq("id", id).set("top", top));
        return RestBean.success(null, top == 1 ? "置顶成功" : "取消置顶成功");
    }

    @PostMapping("/topic/status")
    public RestBean<Void> auditTopic(@RequestParam int id, @RequestParam String status,
                                      @RequestAttribute("id") int uid) {
        TopicDTO topic = topicService.getById(id);
        if (topic == null) return RestBean.failure(404, "帖子不存在");
        if (!canManageTopic(uid, topic)) return RestBean.failure(403, "无权操作此版块的帖子");
        topicService.update(Wrappers.<TopicDTO>update().eq("id", id).set("status", status));
        return RestBean.success(null, "审核状态已更新");
    }

    @PostMapping("/topic/featured")
    public RestBean<Void> toggleFeatured(@RequestParam int id, @RequestParam int featured,
                                          @RequestAttribute("id") int uid) {
        TopicDTO topic = topicService.getById(id);
        if (topic == null) return RestBean.failure(404, "帖子不存在");
        if (!canManageTopic(uid, topic)) return RestBean.failure(403, "无权操作此版块的帖子");
        topicService.update(Wrappers.<TopicDTO>update().eq("id", id).set("featured", featured));
        return RestBean.success(null, featured == 1 ? "已设为精华" : "已取消精华");
    }

    @PostMapping("/topic/batch-delete")
    public RestBean<Void> batchDeleteTopics(@RequestBody List<Integer> ids,
                                             @RequestAttribute("id") int uid) {
        if (ids == null || ids.isEmpty()) return RestBean.failure(400, "请选择帖子");
        for (Integer id : ids) {
            TopicDTO topic = topicService.getById(id);
            if (topic != null && canManageTopic(uid, topic)) {
                topicService.deleteTopic(uid, id);
            }
        }
        return RestBean.success(null, "批量删除成功");
    }

    // ================== 公告管理（仅 admin）==================

    @GetMapping("/notices")
    public RestBean<List<NoticeDTO>> notices(@RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        return RestBean.success(noticeService.listAll(), null);
    }

    @PostMapping("/notice/add")
    public RestBean<Void> addNotice(@RequestBody NoticeDTO dto, @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        dto.setCreateTime(new Date());
        noticeService.save(dto);
        return RestBean.success(null, "公告发布成功");
    }

    @PutMapping("/notice/update")
    public RestBean<Void> updateNotice(@RequestBody NoticeDTO dto, @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        noticeService.updateById(dto);
        return RestBean.success(null, "公告已更新");
    }

    @PostMapping("/notice/top")
    public RestBean<Void> toggleNoticeTop(@RequestParam int id, @RequestParam int isTop,
                                           @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        noticeService.update(Wrappers.<NoticeDTO>update().eq("id", id).set("is_top", isTop));
        return RestBean.success(null, isTop == 1 ? "已置顶" : "已取消置顶");
    }

    @DeleteMapping("/notice/delete")
    public RestBean<Void> deleteNotice(@RequestParam int id, @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        noticeService.removeById(id);
        return RestBean.success(null, "公告已删除");
    }

    // ================== 校历/活动管理（仅 admin）==================

    @GetMapping("/activities")
    public RestBean<List<ActivityDTO>> activities(@RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        return RestBean.success(activityService.listAll(), null);
    }

    @PostMapping("/activity/add")
    public RestBean<Void> addActivity(@RequestBody ActivityDTO dto, @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        dto.setCreateTime(new Date());
        if (dto.getCurrentPeople() == null) dto.setCurrentPeople(0);
        activityService.save(dto);
        return RestBean.success(null, "活动添加成功");
    }

    @PutMapping("/activity/update")
    public RestBean<Void> updateActivity(@RequestBody ActivityDTO dto, @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        activityService.updateById(dto);
        return RestBean.success(null, "活动更新成功");
    }

    @DeleteMapping("/activity/delete")
    public RestBean<Void> deleteActivity(@RequestParam int id, @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        activityService.removeById(id);
        return RestBean.success(null, "活动已删除");
    }

    // ================== 校历日程管理（仅 admin）==================

    @GetMapping("/schedules")
    public RestBean<List<ScheduleDTO>> schedules(@RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        return RestBean.success(scheduleService.listAll(), null);
    }

    @PostMapping("/schedule/add")
    public RestBean<Void> addSchedule(@RequestBody ScheduleDTO dto, @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        dto.setCreateTime(new Date());
        scheduleService.save(dto);
        return RestBean.success(null, "日程添加成功");
    }

    @PutMapping("/schedule/update")
    public RestBean<Void> updateSchedule(@RequestBody ScheduleDTO dto, @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        scheduleService.updateById(dto);
        return RestBean.success(null, "日程已更新");
    }

    @DeleteMapping("/schedule/delete")
    public RestBean<Void> deleteSchedule(@RequestParam int id, @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        scheduleService.removeById(id);
        return RestBean.success(null, "日程已删除");
    }

    // ================== 资源管理（仅 admin）==================

    @GetMapping("/resources")
    public RestBean<Map<String, Object>> resources(@RequestParam(defaultValue = "0") int page,
                                                   @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        Page<ResourceDTO> pageObj = new Page<>(page + 1, 15);
        resourceService.page(pageObj, Wrappers.<ResourceDTO>query().orderByDesc("create_time"));
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageObj.getRecords());
        result.put("total", pageObj.getTotal());
        return RestBean.success(result, null);
    }

    @DeleteMapping("/resource/delete")
    public RestBean<Void> deleteResource(@RequestParam int id, @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        resourceService.removeById(id);
        return RestBean.success(null, "资源已删除");
    }

    // ================== 敏感词管理（仅 admin）==================

    @GetMapping("/sensitive-words")
    public RestBean<List<SensitiveWordDTO>> sensitiveWords(@RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        return RestBean.success(sensitiveWordMapper.selectList(null), null);
    }

    @PostMapping("/sensitive-word/add")
    public RestBean<Void> addSensitiveWord(@RequestParam String word, @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        if (word == null || word.isBlank()) return RestBean.failure(400, "词语不能为空");
        long exists = sensitiveWordMapper.selectCount(Wrappers.<SensitiveWordDTO>query().eq("word", word.trim()));
        if (exists > 0) return RestBean.failure(400, "该词语已存在");
        SensitiveWordDTO dto = new SensitiveWordDTO();
        dto.setWord(word.trim());
        sensitiveWordMapper.insert(dto);
        return RestBean.success(null, "添加成功");
    }

    @DeleteMapping("/sensitive-word/delete")
    public RestBean<Void> deleteSensitiveWord(@RequestParam int id, @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        sensitiveWordMapper.deleteById(id);
        return RestBean.success(null, "删除成功");
    }

    // ================== 反馈管理（仅 admin）==================

    @GetMapping("/feedbacks")
    public RestBean<List<Map<String, Object>>> feedbacks(@RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        List<FeedbackDTO> list = feedbackMapper.selectList(
                Wrappers.<FeedbackDTO>query().orderByDesc("create_time"));
        List<Map<String, Object>> result = list.stream().map(f -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", f.getId());
            m.put("uid", f.getUid());
            m.put("type", f.getType());
            m.put("title", f.getTitle());
            m.put("content", f.getContent());
            m.put("contact", f.getContact());
            m.put("status", f.getStatus());
            m.put("createTime", f.getCreateTime());
            AccountDTO acc = accountMapper.selectById(f.getUid());
            m.put("username", acc != null ? acc.getUsername() : "未知");
            return m;
        }).toList();
        return RestBean.success(result, null);
    }

    @PostMapping("/feedback/resolve")
    public RestBean<Void> resolveFeedback(@RequestParam int id, @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        feedbackMapper.update(null,
                Wrappers.<FeedbackDTO>update().eq("id", id).set("status", "resolved"));
        return RestBean.success(null, "已标记为已处理");
    }

    @DeleteMapping("/feedback/delete")
    public RestBean<Void> deleteFeedback(@RequestParam int id, @RequestAttribute("id") int uid) {
        if (!isAdmin(uid)) return RestBean.failure(403, "无权限");
        feedbackMapper.deleteById(id);
        return RestBean.success(null, "反馈已删除");
    }
}
