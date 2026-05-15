package org.example.finishedbackend.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.finishedbackend.entity.DTO.AccountDTO;
import org.example.finishedbackend.entity.DTO.AccountDetailsDTO;
import org.example.finishedbackend.entity.DTO.AccountPrivacyDTO;
import org.example.finishedbackend.entity.DTO.FeedbackDTO;
import org.example.finishedbackend.entity.DTO.TopicCommentDTO;
import org.example.finishedbackend.entity.DTO.TopicDTO;
import org.example.finishedbackend.entity.RestBean;
import org.example.finishedbackend.entity.VO.request.ChangePasswordVO;
import org.example.finishedbackend.entity.VO.request.DetailsSaveVO;
import org.example.finishedbackend.entity.VO.request.ModifyEmailVO;
import org.example.finishedbackend.entity.VO.request.PrivacySaveVO;
import org.example.finishedbackend.entity.VO.response.AccountDetailsVO;
import org.example.finishedbackend.entity.VO.response.AccountPrivacyVO;
import org.example.finishedbackend.entity.VO.response.AccountVO;
import org.example.finishedbackend.entity.VO.response.TopicPreviewVO;
import org.example.finishedbackend.entity.VO.response.UserCenterCommentVO;
import org.example.finishedbackend.entity.VO.response.UserCenterLikeVO;
import org.example.finishedbackend.entity.VO.response.UserCenterStatsVO;
import org.example.finishedbackend.entity.VO.response.UserCenterVO;
import org.example.finishedbackend.mapper.TopicCommentMapper;
import org.example.finishedbackend.mapper.TopicMapper;
import org.example.finishedbackend.service.AccountDetailsService;
import org.example.finishedbackend.service.AccountPrivacyService;
import org.example.finishedbackend.service.AccountService;
import org.example.finishedbackend.service.TopicService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class AccountController {

    @Resource
    AccountService accountService;

    @Resource
    AccountDetailsService accountDetailsService;

    @Resource
    AccountPrivacyService accountPrivacyService;

    @Resource
    TopicService topicService;

    @Resource
    TopicMapper topicMapper;

    @Resource
    TopicCommentMapper topicCommentMapper;

    @Resource
    org.example.finishedbackend.mapper.FeedbackMapper feedbackMapper;

    @GetMapping("/info")
    public RestBean<AccountVO> info(@RequestAttribute("id") int id) {
        AccountDTO dto = accountService.findAccountById(id);
        AccountVO vo = new AccountVO(dto.getId(), dto.getUsername(), dto.getEmail(), dto.getRole(), dto.getAvatar(), dto.getCreate_time());
        return RestBean.success(vo, "account info get success");
    }

    @GetMapping("/details")
    public RestBean<AccountDetailsVO> details(@RequestAttribute("id") int id) {
        AccountDetailsDTO dto = Optional.ofNullable(accountDetailsService.findAccountDetailsById(id)).orElseGet(AccountDetailsDTO::new);
        AccountDetailsVO vo = new AccountDetailsVO(dto.getGender(), dto.getPhone(), dto.getQq(), dto.getDesc(), dto.getCover());
        return RestBean.success(vo, "account details get success");
    }

    @GetMapping("/center")
    public RestBean<UserCenterVO> center(@RequestAttribute("id") int id) {
        AccountDTO account = accountService.findAccountById(id);
        AccountDetailsDTO details = Optional.ofNullable(accountDetailsService.findAccountDetailsById(id))
                .orElseGet(AccountDetailsDTO::new);

        Page<TopicDTO> page = Page.of(1, 20);
        topicService.page(page, Wrappers.<TopicDTO>query().eq("uid", id).orderByDesc("time"));
        List<TopicPreviewVO> posts = page.getRecords().stream()
                .map(topic -> topicService.resolvePreviewById(topic.getId()))
                .filter(Objects::nonNull)
                .toList();

        List<TopicPreviewVO> bookmarks = topicService.listTopicCollects(id).stream()
                .limit(6)
                .toList();

        Page<TopicCommentDTO> commentPage = Page.of(1, 20);
        topicCommentMapper.selectPage(commentPage, Wrappers.<TopicCommentDTO>query()
                .eq("uid", id)
                .orderByDesc("time"));
        List<UserCenterCommentVO> comments = commentPage.getRecords().stream()
                .map(comment -> {
                    TopicDTO topic = topicService.getById(comment.getTid());
                    return new UserCenterCommentVO(
                            comment.getId(),
                            comment.getTid(),
                            topic == null ? "原帖已删除" : topic.getTitle(),
                            limitText(extractRichText(comment.getContent()), 240),
                            resolveQuoteText(comment.getQuote()),
                            comment.getTime(),
                            Optional.ofNullable(comment.getLikeCount()).orElse(0)
                    );
                })
                .toList();

        List<UserCenterLikeVO> likeItems = new ArrayList<>();
        topicService.list(Wrappers.<TopicDTO>query().eq("uid", id).orderByDesc("time")).stream()
                .map(topic -> topicService.resolvePreviewById(topic.getId()))
                .filter(Objects::nonNull)
                .filter(topic -> topic.getLike() > 0)
                .forEach(topic -> likeItems.add(new UserCenterLikeVO(
                        "topic",
                        topic.getId(),
                        topic.getId(),
                        topic.getTitle(),
                        topic.getTitle(),
                        limitText(topic.getText(), 240),
                        topic.getTime(),
                        topic.getLike()
                )));

        Page<TopicCommentDTO> likedCommentPage = Page.of(1, 20);
        topicCommentMapper.selectPage(likedCommentPage, Wrappers.<TopicCommentDTO>query()
                .eq("uid", id)
                .gt("like_count", 0)
                .orderByDesc("like_count")
                .orderByDesc("time"));
        likedCommentPage.getRecords().forEach(comment -> {
            TopicDTO topic = topicService.getById(comment.getTid());
            likeItems.add(new UserCenterLikeVO(
                    "comment",
                    comment.getId(),
                    comment.getTid(),
                    topic == null ? "原帖已删除" : topic.getTitle(),
                    topic == null ? "评论获赞" : topic.getTitle(),
                    limitText(extractRichText(comment.getContent()), 240),
                    comment.getTime(),
                    Optional.ofNullable(comment.getLikeCount()).orElse(0)
            ));
        });
        likeItems.sort((a, b) -> {
            int countCompare = Integer.compare(b.getLikeCount(), a.getLikeCount());
            if (countCompare != 0) return countCompare;
            return Long.compare(timeOf(b.getTime()), timeOf(a.getTime()));
        });
        List<UserCenterLikeVO> likes = likeItems.stream().limit(20).toList();

        UserCenterStatsVO stats = new UserCenterStatsVO(
                (int) topicService.count(Wrappers.<TopicDTO>query().eq("uid", id)),
                topicCommentMapper.selectCount(Wrappers.<TopicCommentDTO>query().eq("uid", id)).intValue(),
                topicMapper.receivedLikeCount(id) + Optional.ofNullable(topicCommentMapper.sumReceivedLikeCount(id)).orElse(0),
                topicMapper.collectCountByUid(id)
        );

        UserCenterVO vo = new UserCenterVO(
                new AccountVO(account.getId(), account.getUsername(), account.getEmail(), account.getRole(), account.getAvatar(), account.getCreate_time()),
                new AccountDetailsVO(details.getGender(), details.getPhone(), details.getQq(), details.getDesc(), details.getCover()),
                stats,
                posts,
                bookmarks,
                comments,
                likes
        );
        return RestBean.success(vo, null);
    }

    private String resolveQuoteText(Integer quoteId) {
        if (quoteId == null || quoteId <= 0) return null;
        TopicCommentDTO quoted = topicCommentMapper.selectById(quoteId);
        if (quoted == null) return "引用评论已删除";
        return limitText(extractRichText(quoted.getContent()), 80);
    }

    private String extractRichText(String content) {
        if (content == null || content.isBlank()) return "";
        try {
            JSONObject object = JSONObject.parseObject(content);
            JSONArray ops = object.getJSONArray("ops");
            if (ops == null) return content;
            StringBuilder builder = new StringBuilder();
            ops.forEach(op -> {
                Object insert = JSONObject.from(op).get("insert");
                if (insert instanceof String) {
                    builder.append((String) insert);
                }
            });
            String text = builder.toString().trim();
            return text.isBlank() ? content : text;
        } catch (Exception ignored) {
            return content;
        }
    }

    private String limitText(String text, int maxLength) {
        if (text == null) return "";
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) + "..." : normalized;
    }

    private long timeOf(Date date) {
        return date == null ? 0L : date.getTime();
    }

    @PostMapping("/save-details")
    public RestBean<Void> saveDetails(@RequestAttribute("id") int id,
                                      @RequestBody DetailsSaveVO vo) {
        return accountDetailsService.saveAccountDetails(id, vo) ? RestBean.success("修改成功") : RestBean.failure(401, "此用户名已被其他用户使用!");
    }

    @PostMapping("/modify-email")
    public RestBean<Void> modifyEmail(@RequestAttribute("id") int id,
                                      @RequestBody @Valid ModifyEmailVO vo) {
        String result = accountService.modifyEmail(id, vo);
        return result == null ? RestBean.success("修改成功") : RestBean.failure(400, result);
    }

    @PostMapping("/change-password")
    public RestBean<Void> changePassword(@RequestAttribute("id") int id,
                                         @RequestBody @Valid ChangePasswordVO vo) {
        String s = accountService.changePassword(id, vo);
        return s == null ? RestBean.success("修改密码成功") : RestBean.failure(401, s);
    }

    @PostMapping("/save-privacy")
    public RestBean<Void> savePrivacy(@RequestAttribute("id") int id,
                                      @RequestBody @Valid PrivacySaveVO vo) {
        accountPrivacyService.savePrivacy(id, vo);
        return RestBean.success("修改成功");
    }

    @GetMapping("/privacy")
    public RestBean<AccountPrivacyVO> privacy(@RequestAttribute("id") int id) {
        AccountPrivacyDTO dto = accountPrivacyService.accountPrivacy(id);
        return RestBean.success(new AccountPrivacyVO(dto.isPhone(), dto.isEmail(), dto.isQq(), dto.isGender()), "获取成功");
    }

    @PostMapping("/feedback")
    public RestBean<Void> submitFeedback(@RequestAttribute("id") int uid,
                                         @RequestBody FeedbackDTO feedback) {
        feedback.setUid(uid);
        feedback.setStatus("pending");
        feedback.setCreateTime(new java.util.Date());
        feedbackMapper.insert(feedback);
        return RestBean.success("反馈提交成功");
    }
}
