package org.example.finishedbackend.service.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.finishedbackend.entity.DTO.*;
import org.example.finishedbackend.entity.RestBean;
import org.example.finishedbackend.mapper.AccountMapper;
import org.example.finishedbackend.mapper.FeedbackMapper;
import org.example.finishedbackend.mapper.SensitiveWordMapper;
import org.example.finishedbackend.service.*;
import org.example.finishedbackend.utils.CacheUtils;
import org.example.finishedbackend.utils.Const;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdminOperationsService {

    private static final Set<String> ASSIGNABLE_ROLES = Set.of("user", "content_admin", "moderator");
    private static final Set<String> AUDIT_STATUSES = Set.of("pending", "approved", "rejected");

    @Resource
    AdminGuardService guard;

    @Resource
    AccountService accountService;

    @Resource
    TopicService topicService;

    @Resource
    NoticeService noticeService;

    @Resource
    ActivityService activityService;

    @Resource
    ResourceService resourceService;

    @Resource
    ScheduleService scheduleService;

    @Resource
    SensitiveWordMapper sensitiveWordMapper;

    @Resource
    FeedbackMapper feedbackMapper;

    @Resource
    AccountMapper accountMapper;

    @Resource
    CacheUtils cacheUtils;

    public RestBean<Map<String, Object>> users(int page, String keyword) {
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

    public RestBean<Void> deleteUser(int id, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        AccountDTO acc = accountService.findAccountById(id);
        if (acc == null) return RestBean.failure(404, "用户不存在");
        if ("admin".equals(acc.getRole())) return RestBean.failure(403, "不能删除超级管理员");
        accountService.removeById(id);
        return RestBean.success(null, "删除成功");
    }

    public RestBean<Void> banUser(int id, boolean banned, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        AccountDTO acc = accountService.findAccountById(id);
        if (acc == null) return RestBean.failure(404, "用户不存在");
        if ("admin".equals(acc.getRole())) return RestBean.failure(403, "不能封禁超级管理员");
        accountService.update(Wrappers.<AccountDTO>update().eq("id", id).set("banned", banned ? 1 : 0));
        return RestBean.success(null, banned ? "用户已封禁" : "用户已解封");
    }

    public RestBean<Void> changeRole(int id, String role, Integer moderatorType, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        String normalizedRole = role == null ? "" : role.trim();
        if ("admin".equals(normalizedRole)) return RestBean.failure(403, "超级管理员不能通过此接口设置");
        if (!ASSIGNABLE_ROLES.contains(normalizedRole)) return RestBean.failure(400, "角色非法");
        if ("moderator".equals(normalizedRole) && moderatorType == null) return RestBean.failure(400, "请选择版主负责的版块");
        AccountDTO acc = accountService.findAccountById(id);
        if (acc == null) return RestBean.failure(404, "用户不存在");
        if ("admin".equals(acc.getRole())) return RestBean.failure(403, "不能修改超级管理员的角色");
        var update = Wrappers.<AccountDTO>update().eq("id", id).set("role", normalizedRole);
        if ("moderator".equals(normalizedRole)) {
            update.set("moderator_type", moderatorType);
        } else {
            update.set("moderator_type", null);
        }
        accountService.update(update);
        return RestBean.success(null, "角色已更新");
    }

    public RestBean<Void> batchDeleteUsers(List<Integer> ids, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        if (ids == null || ids.isEmpty()) return RestBean.failure(400, "请选择用户");
        for (Integer id : ids) {
            AccountDTO acc = accountService.findAccountById(id);
            if (acc != null && !"admin".equals(acc.getRole())) {
                accountService.removeById(id);
            }
        }
        return RestBean.success(null, "批量删除成功");
    }

    public RestBean<Void> batchBanUsers(List<Integer> ids, boolean banned, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        if (ids == null || ids.isEmpty()) return RestBean.failure(400, "请选择用户");
        for (Integer id : ids) {
            AccountDTO acc = accountService.findAccountById(id);
            if (acc != null && !"admin".equals(acc.getRole())) {
                accountService.update(Wrappers.<AccountDTO>update().eq("id", id).set("banned", banned ? 1 : 0));
            }
        }
        return RestBean.success(null, banned ? "批量封禁成功" : "批量解封成功");
    }

    public RestBean<Map<String, Object>> topics(int page, int uid) {
        AccountDTO acc = guard.currentAccount(uid);
        Page<TopicDTO> pageObj = new Page<>(page + 1, 10);
        var wrapper = Wrappers.<TopicDTO>query().orderByDesc("top", "time");
        if (acc != null && "moderator".equals(acc.getRole()) && acc.getModeratorType() != null) {
            wrapper.eq("type", acc.getModeratorType());
        }
        topicService.page(pageObj, wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageObj.getRecords());
        result.put("total", pageObj.getTotal());
        return RestBean.success(result, null);
    }

    public RestBean<Void> deleteTopic(int id, int uid) {
        TopicDTO topic = topicService.getById(id);
        if (topic == null) return RestBean.failure(404, "帖子不存在");
        if (!guard.canManageTopic(uid, topic)) return RestBean.failure(403, "无权操作此版块的帖子");
        String result = topicService.deleteTopic(uid, id);
        return result == null ? RestBean.success(null, "删除成功") : RestBean.failure(400, result);
    }

    public RestBean<Void> toggleTopTopic(int id, int top, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限，置顶操作仅超级管理员可用");
        topicService.update(Wrappers.<TopicDTO>update().eq("id", id).set("top", top));
        return RestBean.success(null, top == 1 ? "置顶成功" : "取消置顶成功");
    }

    public RestBean<Void> auditTopic(int id, String status, int uid) {
        String normalizedStatus = normalizeAuditStatus(status);
        if (normalizedStatus == null) return RestBean.failure(400, "审核状态非法");
        TopicDTO topic = topicService.getById(id);
        if (topic == null) return RestBean.failure(404, "帖子不存在");
        if (!guard.canManageTopic(uid, topic)) return RestBean.failure(403, "无权操作此版块的帖子");
        topicService.update(Wrappers.<TopicDTO>update().eq("id", id).set("status", normalizedStatus));
        cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
        return RestBean.success(null, "审核状态已更新");
    }

    public RestBean<Void> toggleFeatured(int id, int featured, int uid) {
        TopicDTO topic = topicService.getById(id);
        if (topic == null) return RestBean.failure(404, "帖子不存在");
        if (!guard.canManageTopic(uid, topic)) return RestBean.failure(403, "无权操作此版块的帖子");
        topicService.update(Wrappers.<TopicDTO>update().eq("id", id).set("featured", featured));
        return RestBean.success(null, featured == 1 ? "已设为精华" : "已取消精华");
    }

    public RestBean<Void> batchDeleteTopics(List<Integer> ids, int uid) {
        if (ids == null || ids.isEmpty()) return RestBean.failure(400, "请选择帖子");
        for (Integer id : ids) {
            TopicDTO topic = topicService.getById(id);
            if (topic != null && guard.canManageTopic(uid, topic)) {
                topicService.deleteTopic(uid, id);
            }
        }
        return RestBean.success(null, "批量删除成功");
    }

    public RestBean<List<NoticeDTO>> notices(int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        return RestBean.success(noticeService.listAll(), null);
    }

    public RestBean<Void> addNotice(NoticeDTO dto, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        dto.setCreateTime(new Date());
        noticeService.save(dto);
        return RestBean.success(null, "公告发布成功");
    }

    public RestBean<Void> updateNotice(NoticeDTO dto, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        noticeService.updateById(dto);
        return RestBean.success(null, "公告已更新");
    }

    public RestBean<Void> toggleNoticeTop(int id, int isTop, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        noticeService.update(Wrappers.<NoticeDTO>update().eq("id", id).set("is_top", isTop));
        return RestBean.success(null, isTop == 1 ? "已置顶" : "已取消置顶");
    }

    public RestBean<Void> deleteNotice(int id, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        noticeService.removeById(id);
        return RestBean.success(null, "公告已删除");
    }

    public RestBean<List<ActivityDTO>> activities(int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        return RestBean.success(activityService.listAll(), null);
    }

    public RestBean<Void> addActivity(ActivityDTO dto, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        dto.setCreateTime(new Date());
        if (dto.getCurrentPeople() == null) dto.setCurrentPeople(0);
        activityService.save(dto);
        return RestBean.success(null, "活动添加成功");
    }

    public RestBean<Void> updateActivity(ActivityDTO dto, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        activityService.updateById(dto);
        return RestBean.success(null, "活动更新成功");
    }

    public RestBean<Void> deleteActivity(int id, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        activityService.removeById(id);
        return RestBean.success(null, "活动已删除");
    }

    public RestBean<List<ScheduleDTO>> schedules(int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        return RestBean.success(scheduleService.listAll(), null);
    }

    public RestBean<Void> addSchedule(ScheduleDTO dto, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        dto.setCreateTime(new Date());
        scheduleService.save(dto);
        return RestBean.success(null, "日程添加成功");
    }

    public RestBean<Void> updateSchedule(ScheduleDTO dto, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        scheduleService.updateById(dto);
        return RestBean.success(null, "日程已更新");
    }

    public RestBean<Void> deleteSchedule(int id, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        scheduleService.removeById(id);
        return RestBean.success(null, "日程已删除");
    }

    public RestBean<Map<String, Object>> resources(int page, String status, String keyword, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        Page<ResourceDTO> pageObj = new Page<>(page + 1, 15);
        var wrapper = Wrappers.<ResourceDTO>query();
        String normalizedStatus = normalizeAuditStatus(status);
        if (normalizedStatus != null) {
            wrapper.eq("status", normalizedStatus);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(q -> q.like("title", kw)
                    .or()
                    .like("file_name", kw)
                    .or()
                    .like("description", kw));
        }
        wrapper.last("ORDER BY FIELD(status, 'pending', 'rejected', 'approved'), create_time DESC");
        resourceService.page(pageObj, wrapper);
        List<Map<String, Object>> records = pageObj.getRecords().stream().map(this::resourceRecord).toList();
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", pageObj.getTotal());
        return RestBean.success(result, null);
    }

    public RestBean<Void> deleteResource(int id, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        boolean deleted = resourceService.deleteResource(id, uid);
        return deleted ? RestBean.success(null, "资源已删除") : RestBean.failure(404, "资源不存在");
    }

    public RestBean<Void> auditResource(int id, String status, String reason, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        String normalizedStatus = normalizeAuditStatus(status);
        if (normalizedStatus == null) return RestBean.failure(400, "审核状态非法");
        ResourceDTO resource = resourceService.getById(id);
        if (resource == null) return RestBean.failure(404, "资源不存在");
        updateResourceAuditStatus(id, normalizedStatus, reason, uid);
        return RestBean.success(null, "资源审核状态已更新");
    }

    public RestBean<Void> batchAuditResources(List<Integer> ids, String status, String reason, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        if (ids == null || ids.isEmpty()) return RestBean.failure(400, "请选择资源");
        String normalizedStatus = normalizeAuditStatus(status);
        if (normalizedStatus == null) return RestBean.failure(400, "审核状态非法");
        int updated = 0;
        for (Integer id : ids) {
            if (id == null || resourceService.getById(id) == null) continue;
            updateResourceAuditStatus(id, normalizedStatus, reason, uid);
            updated++;
        }
        return updated > 0 ? RestBean.success(null, "批量审核成功") : RestBean.failure(404, "资源不存在");
    }

    public RestBean<Void> batchDeleteResources(List<Integer> ids, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        if (ids == null || ids.isEmpty()) return RestBean.failure(400, "请选择资源");
        int deleted = 0;
        for (Integer id : ids) {
            if (id != null && resourceService.deleteResource(id, uid)) {
                deleted++;
            }
        }
        return deleted > 0 ? RestBean.success(null, "批量删除成功") : RestBean.failure(404, "资源不存在");
    }

    private void updateResourceAuditStatus(int id, String normalizedStatus, String reason, int uid) {
        var update = Wrappers.<ResourceDTO>update()
                .eq("id", id)
                .set("status", normalizedStatus)
                .set("auditor_id", uid)
                .set("audit_time", new Date());
        if ("rejected".equals(normalizedStatus)) {
            update.set("reject_reason", reason == null || reason.isBlank() ? "内容不符合资源共享规范" : reason.trim());
        } else {
            update.set("reject_reason", null);
        }
        resourceService.update(update);
    }

    private String normalizeAuditStatus(String status) {
        if (status == null) return null;
        String normalized = status.trim();
        return AUDIT_STATUSES.contains(normalized) ? normalized : null;
    }

    private Map<String, Object> resourceRecord(ResourceDTO resource) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", resource.getId());
        record.put("title", resource.getTitle());
        record.put("category", resource.getCategory());
        record.put("fileName", resource.getFileName());
        record.put("fileSize", resource.getFileSize());
        record.put("downloadCount", resource.getDownloadCount());
        record.put("description", resource.getDescription());
        record.put("uploaderId", resource.getUploaderId());
        record.put("status", resource.getStatus());
        record.put("rejectReason", resource.getRejectReason());
        record.put("auditTime", resource.getAuditTime());
        record.put("auditorId", resource.getAuditorId());
        record.put("createTime", resource.getCreateTime());
        AccountDTO uploader = accountMapper.selectById(resource.getUploaderId());
        record.put("uploaderName", uploader != null ? uploader.getUsername() : "未知用户");
        AccountDTO auditor = resource.getAuditorId() == null ? null : accountMapper.selectById(resource.getAuditorId());
        record.put("auditorName", auditor != null ? auditor.getUsername() : null);
        return record;
    }

    public RestBean<List<SensitiveWordDTO>> sensitiveWords(int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        return RestBean.success(sensitiveWordMapper.selectList(null), null);
    }

    public RestBean<Void> addSensitiveWord(String word, String type, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        if (word == null || word.isBlank()) return RestBean.failure(400, "词语不能为空");
        String normalizedType = "forbidden".equals(type) ? "forbidden" : "sensitive";
        long exists = sensitiveWordMapper.selectCount(Wrappers.<SensitiveWordDTO>query().eq("word", word.trim()));
        if (exists > 0) return RestBean.failure(400, "该词语已存在");
        SensitiveWordDTO dto = new SensitiveWordDTO();
        dto.setWord(word.trim());
        dto.setType(normalizedType);
        sensitiveWordMapper.insert(dto);
        return RestBean.success(null, "添加成功");
    }

    public RestBean<Void> deleteSensitiveWord(int id, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        sensitiveWordMapper.deleteById(id);
        return RestBean.success(null, "删除成功");
    }

    public RestBean<List<Map<String, Object>>> feedbacks(int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
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

    public RestBean<Void> resolveFeedback(int id, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        feedbackMapper.update(null,
                Wrappers.<FeedbackDTO>update().eq("id", id).set("status", "resolved"));
        return RestBean.success(null, "已标记为已处理");
    }

    public RestBean<Void> deleteFeedback(int id, int uid) {
        if (!guard.isAdmin(uid)) return RestBean.failure(403, "无权限");
        feedbackMapper.deleteById(id);
        return RestBean.success(null, "反馈已删除");
    }
}
