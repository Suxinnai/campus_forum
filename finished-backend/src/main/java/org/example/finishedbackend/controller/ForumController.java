package org.example.finishedbackend.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.example.finishedbackend.entity.DTO.Interact;
import org.example.finishedbackend.entity.RestBean;
import org.example.finishedbackend.entity.VO.request.AddCommentVO;
import org.example.finishedbackend.entity.VO.request.TopicCreateVO;
import org.example.finishedbackend.entity.VO.request.TopicUpdateVO;
import org.example.finishedbackend.entity.VO.response.*;
import org.example.finishedbackend.service.TopicService;
import org.example.finishedbackend.service.WeatherService;
import org.example.finishedbackend.service.RecommendService;
import org.example.finishedbackend.service.NotificationService;
import org.example.finishedbackend.service.AccountService;
import org.example.finishedbackend.entity.DTO.AccountDTO;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/forum")
public class ForumController {

    @Resource
    WeatherService service;

    @Resource
    TopicService topic;

    @Resource
    RecommendService recommendService;

    @Resource
    NotificationService notificationService;

    @Resource
    AccountService accountService;

    @GetMapping("/weather")
    public RestBean<WeatherVO> weather(double longitude, double latitude) {
        WeatherVO vo = service.fetchWeather(longitude, latitude);
        return vo == null ? RestBean.failure(400, "获取地理位置与天气失败, 请稍后再试!") : RestBean.success(vo, "接口数据获取成功");
    }

    @GetMapping("/types")
    public RestBean<List<TopicTypeVO>> listTypes() {
        return RestBean.success(topic.listTypes().stream()
                .map(type -> new TopicTypeVO(type.getId(), type.getName(), type.getDesc(), type.getColor())).toList(),
                "获取typeList成功");
    }

    @PostMapping("/create-topic")
    public RestBean<Void> createTopic(@RequestAttribute("id") int id,
            @RequestBody @Valid TopicCreateVO vo) {
        String s = topic.createTopic(id, vo);
        return s == null ? RestBean.success("创建帖子成功") : RestBean.failure(400, s);
    }

    @GetMapping("/list-topic")
    public RestBean<List<TopicPreviewVO>> listTopic(@RequestParam @Min(0) int page,
            @RequestParam int type) {
        List<TopicPreviewVO> voList = topic.listTopicByPage(page, type);
        return voList != null ? RestBean.success(voList, null) : RestBean.failure(400, "已经到头了~");
    }

    @GetMapping("/page-topic")
    public RestBean<TopicPageVO> pageTopic(@RequestParam @Min(0) int page,
                                           @RequestParam int type,
                                           @RequestParam(defaultValue = "10") int size) {
        return RestBean.success(topic.pageTopics(page, type, size), null);
    }

    @GetMapping("/top-topic")
    public RestBean<List<TopicTopVO>> topTopic() {
        return RestBean.success(topic.listTopTopics(), null);
    }

    @GetMapping("/topic")
    public RestBean<TopicDetailVO> topic(@RequestParam @Min(0) int tid,
            @RequestAttribute("id") int id) {
        TopicDetailVO vo = topic.getTopic(tid, id);
        if (vo == null) return RestBean.failure(404, "帖子不存在");
        recommendService.recordBehavior(id, tid, "view");
        return RestBean.success(vo, null);
    }

    @PostMapping("/interact")
    public RestBean<Void> interact(@RequestParam @Min(0) int tid,
            @RequestParam @Pattern(regexp = "(like|collect)") String type,
            @RequestParam boolean state,
            @RequestAttribute("id") int id) {
        if (topic.resolvePreviewById(tid) == null) {
            return RestBean.failure(404, "帖子不存在或尚未通过审核");
        }
        topic.interact(new Interact(tid, id, new Date(), type), state);
        if (state) {
            recommendService.recordBehavior(id, tid, type);
            // 点赞通知
            if ("like".equals(type)) {
                TopicDetailVO detail = topic.getTopic(tid, id);
                if (detail != null && detail.getUser() != null) {
                    int authorId = detail.getUser().getId();
                    if (authorId != id) {
                        AccountDTO liker = accountService.findAccountById(id);
                        String likerName = liker != null ? liker.getUsername() : "有人";
                        notificationService.addNotification(authorId, "收到一个赞",
                                likerName + " 赞了你的帖子「" + detail.getTitle() + "」",
                                "warning", "/home/topic/" + tid);
                    }
                }
            }
        }
        return RestBean.success(null);
    }

    @GetMapping("/collects")
    public RestBean<List<TopicPreviewVO>> collect(@RequestAttribute("id") int id) {
        return RestBean.success(topic.listTopicCollects(id), null);
    }

    @PostMapping("/update-topic")
    public RestBean<Void> updateTopic(@RequestAttribute("id") int id,
            @RequestBody @Valid TopicUpdateVO vo) {
        String s = topic.updateTopic(id, vo);
        return s == null ? RestBean.success("帖子更新成功") : RestBean.failure(400, s);
    }

    @PostMapping("/add-comment")
    public RestBean<Void> addComment(@RequestAttribute("id") int id,
            @RequestBody @Valid AddCommentVO vo) {
        String s = topic.createComment(id, vo);
        if (s == null) {
            // 记录评论行为（权重 +4）
            recommendService.recordBehavior(id, vo.getTid(), "comment");
            return RestBean.success(null);
        }
        return RestBean.failure(400, s);
    }

    @GetMapping("/comments")
    public RestBean<List<CommentVO>> comments(@RequestParam @Min(0) int tid,
            @RequestParam @Min(0) int page) {
        return RestBean.success(topic.comments(tid, page), null);
    }

    @DeleteMapping("/delete-comment")
    public RestBean<Void> deleteComment(@RequestParam @Min(1) int id,
            @RequestAttribute("id") int uid) {
        topic.deleteComment(uid, id);
        return RestBean.success(null);
    }

    @DeleteMapping("/delete-topic")
    public RestBean<Void> deleteTopic(@RequestParam @Min(1) int id,
            @RequestAttribute("id") int uid) {
        String result = topic.deleteTopic(uid, id);
        return result == null ? RestBean.success("帖子删除成功") : RestBean.failure(403, result);
    }

    @GetMapping("/search")
    public RestBean<List<TopicPreviewVO>> search(@RequestParam String keyword,
            @RequestParam @Min(0) int page) {
        return RestBean.success(topic.searchTopics(keyword, page), null);
    }
}
