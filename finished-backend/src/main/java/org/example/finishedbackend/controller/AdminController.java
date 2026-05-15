package org.example.finishedbackend.controller;

import jakarta.annotation.Resource;
import org.example.finishedbackend.entity.DTO.ActivityDTO;
import org.example.finishedbackend.entity.DTO.NoticeDTO;
import org.example.finishedbackend.entity.DTO.ScheduleDTO;
import org.example.finishedbackend.entity.DTO.SensitiveWordDTO;
import org.example.finishedbackend.entity.RestBean;
import org.example.finishedbackend.service.admin.AdminDashboardService;
import org.example.finishedbackend.service.admin.AdminOperationsService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Resource
    AdminDashboardService dashboardService;

    @Resource
    AdminOperationsService operationsService;

    @GetMapping("/stats")
    public RestBean<Map<String, Object>> stats() {
        return RestBean.success(dashboardService.stats(), null);
    }

    @GetMapping("/users")
    public RestBean<Map<String, Object>> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword) {
        return operationsService.users(page, keyword);
    }

    @DeleteMapping("/user")
    public RestBean<Void> deleteUser(@RequestParam int id, @RequestAttribute("id") int uid) {
        return operationsService.deleteUser(id, uid);
    }

    @PostMapping("/user/ban")
    public RestBean<Void> banUser(@RequestParam int id, @RequestParam boolean banned,
                                  @RequestAttribute("id") int uid) {
        return operationsService.banUser(id, banned, uid);
    }

    @PostMapping("/user/role")
    public RestBean<Void> changeRole(@RequestParam int id, @RequestParam String role,
                                     @RequestParam(required = false) Integer moderatorType,
                                     @RequestAttribute("id") int uid) {
        return operationsService.changeRole(id, role, moderatorType, uid);
    }

    @PostMapping("/user/batch-delete")
    public RestBean<Void> batchDeleteUsers(@RequestBody List<Integer> ids,
                                           @RequestAttribute("id") int uid) {
        return operationsService.batchDeleteUsers(ids, uid);
    }

    @PostMapping("/user/batch-ban")
    public RestBean<Void> batchBanUsers(@RequestBody List<Integer> ids,
                                        @RequestParam boolean banned,
                                        @RequestAttribute("id") int uid) {
        return operationsService.batchBanUsers(ids, banned, uid);
    }

    @GetMapping("/topics")
    public RestBean<Map<String, Object>> topics(@RequestParam(defaultValue = "0") int page,
                                                @RequestAttribute("id") int uid) {
        return operationsService.topics(page, uid);
    }

    @DeleteMapping("/topic")
    public RestBean<Void> deleteTopic(@RequestParam int id, @RequestAttribute("id") int uid) {
        return operationsService.deleteTopic(id, uid);
    }

    @PostMapping("/topic/top")
    public RestBean<Void> toggleTopTopic(@RequestParam int id, @RequestParam int top,
                                         @RequestAttribute("id") int uid) {
        return operationsService.toggleTopTopic(id, top, uid);
    }

    @PostMapping("/topic/status")
    public RestBean<Void> auditTopic(@RequestParam int id, @RequestParam String status,
                                     @RequestAttribute("id") int uid) {
        return operationsService.auditTopic(id, status, uid);
    }

    @PostMapping("/topic/featured")
    public RestBean<Void> toggleFeatured(@RequestParam int id, @RequestParam int featured,
                                         @RequestAttribute("id") int uid) {
        return operationsService.toggleFeatured(id, featured, uid);
    }

    @PostMapping("/topic/batch-delete")
    public RestBean<Void> batchDeleteTopics(@RequestBody List<Integer> ids,
                                            @RequestAttribute("id") int uid) {
        return operationsService.batchDeleteTopics(ids, uid);
    }

    @GetMapping("/notices")
    public RestBean<List<NoticeDTO>> notices(@RequestAttribute("id") int uid) {
        return operationsService.notices(uid);
    }

    @PostMapping("/notice/add")
    public RestBean<Void> addNotice(@RequestBody NoticeDTO dto, @RequestAttribute("id") int uid) {
        return operationsService.addNotice(dto, uid);
    }

    @PutMapping("/notice/update")
    public RestBean<Void> updateNotice(@RequestBody NoticeDTO dto, @RequestAttribute("id") int uid) {
        return operationsService.updateNotice(dto, uid);
    }

    @PostMapping("/notice/top")
    public RestBean<Void> toggleNoticeTop(@RequestParam int id, @RequestParam int isTop,
                                          @RequestAttribute("id") int uid) {
        return operationsService.toggleNoticeTop(id, isTop, uid);
    }

    @DeleteMapping("/notice/delete")
    public RestBean<Void> deleteNotice(@RequestParam int id, @RequestAttribute("id") int uid) {
        return operationsService.deleteNotice(id, uid);
    }

    @GetMapping("/activities")
    public RestBean<List<ActivityDTO>> activities(@RequestAttribute("id") int uid) {
        return operationsService.activities(uid);
    }

    @PostMapping("/activity/add")
    public RestBean<Void> addActivity(@RequestBody ActivityDTO dto, @RequestAttribute("id") int uid) {
        return operationsService.addActivity(dto, uid);
    }

    @PutMapping("/activity/update")
    public RestBean<Void> updateActivity(@RequestBody ActivityDTO dto, @RequestAttribute("id") int uid) {
        return operationsService.updateActivity(dto, uid);
    }

    @DeleteMapping("/activity/delete")
    public RestBean<Void> deleteActivity(@RequestParam int id, @RequestAttribute("id") int uid) {
        return operationsService.deleteActivity(id, uid);
    }

    @GetMapping("/schedules")
    public RestBean<List<ScheduleDTO>> schedules(@RequestAttribute("id") int uid) {
        return operationsService.schedules(uid);
    }

    @PostMapping("/schedule/add")
    public RestBean<Void> addSchedule(@RequestBody ScheduleDTO dto, @RequestAttribute("id") int uid) {
        return operationsService.addSchedule(dto, uid);
    }

    @PutMapping("/schedule/update")
    public RestBean<Void> updateSchedule(@RequestBody ScheduleDTO dto, @RequestAttribute("id") int uid) {
        return operationsService.updateSchedule(dto, uid);
    }

    @DeleteMapping("/schedule/delete")
    public RestBean<Void> deleteSchedule(@RequestParam int id, @RequestAttribute("id") int uid) {
        return operationsService.deleteSchedule(id, uid);
    }

    @GetMapping("/resources")
    public RestBean<Map<String, Object>> resources(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestAttribute("id") int uid) {
        return operationsService.resources(page, status, keyword, uid);
    }

    @DeleteMapping("/resource/delete")
    public RestBean<Void> deleteResource(@RequestParam int id, @RequestAttribute("id") int uid) {
        return operationsService.deleteResource(id, uid);
    }

    @PostMapping("/resource/status")
    public RestBean<Void> auditResource(@RequestParam int id,
                                        @RequestParam String status,
                                        @RequestParam(required = false) String reason,
                                        @RequestAttribute("id") int uid) {
        return operationsService.auditResource(id, status, reason, uid);
    }

    @PostMapping("/resource/batch-status")
    public RestBean<Void> batchAuditResources(@RequestBody List<Integer> ids,
                                              @RequestParam String status,
                                              @RequestParam(required = false) String reason,
                                              @RequestAttribute("id") int uid) {
        return operationsService.batchAuditResources(ids, status, reason, uid);
    }

    @PostMapping("/resource/batch-delete")
    public RestBean<Void> batchDeleteResources(@RequestBody List<Integer> ids,
                                               @RequestAttribute("id") int uid) {
        return operationsService.batchDeleteResources(ids, uid);
    }

    @GetMapping("/sensitive-words")
    public RestBean<List<SensitiveWordDTO>> sensitiveWords(@RequestAttribute("id") int uid) {
        return operationsService.sensitiveWords(uid);
    }

    @PostMapping("/sensitive-word/add")
    public RestBean<Void> addSensitiveWord(@RequestParam String word,
                                           @RequestParam(defaultValue = "sensitive") String type,
                                           @RequestAttribute("id") int uid) {
        return operationsService.addSensitiveWord(word, type, uid);
    }

    @DeleteMapping("/sensitive-word/delete")
    public RestBean<Void> deleteSensitiveWord(@RequestParam int id, @RequestAttribute("id") int uid) {
        return operationsService.deleteSensitiveWord(id, uid);
    }

    @GetMapping("/feedbacks")
    public RestBean<List<Map<String, Object>>> feedbacks(@RequestAttribute("id") int uid) {
        return operationsService.feedbacks(uid);
    }

    @PostMapping("/feedback/resolve")
    public RestBean<Void> resolveFeedback(@RequestParam int id, @RequestAttribute("id") int uid) {
        return operationsService.resolveFeedback(id, uid);
    }

    @DeleteMapping("/feedback/delete")
    public RestBean<Void> deleteFeedback(@RequestParam int id, @RequestAttribute("id") int uid) {
        return operationsService.deleteFeedback(id, uid);
    }
}
