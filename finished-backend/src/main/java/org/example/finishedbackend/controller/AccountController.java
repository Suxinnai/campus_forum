package org.example.finishedbackend.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import org.example.finishedbackend.entity.VO.response.UserCenterStatsVO;
import org.example.finishedbackend.entity.VO.response.UserCenterVO;
import org.example.finishedbackend.mapper.TopicCommentMapper;
import org.example.finishedbackend.mapper.TopicMapper;
import org.example.finishedbackend.service.AccountDetailsService;
import org.example.finishedbackend.service.AccountPrivacyService;
import org.example.finishedbackend.service.AccountService;
import org.example.finishedbackend.service.TopicService;
import org.springframework.web.bind.annotation.*;

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
                bookmarks
        );
        return RestBean.success(vo, null);
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
