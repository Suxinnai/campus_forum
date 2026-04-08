package org.example.finishedbackend.service.Impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.finishedbackend.entity.DTO.*;
import org.example.finishedbackend.entity.VO.request.AddCommentVO;
import org.example.finishedbackend.entity.VO.response.*;
import org.example.finishedbackend.mapper.*;
import org.example.finishedbackend.service.CommentService;
import org.example.finishedbackend.service.NotificationService;
import org.example.finishedbackend.service.filter.ContentFilter;
import org.example.finishedbackend.service.filter.FilterResult;
import org.example.finishedbackend.utils.FlowUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {
    
    @Resource
    TopicCommentMapper commentMapper;
    
    @Resource
    CommentLikeMapper likeMapper;
    
    @Resource
    CommentReportMapper reportMapper;
    
    @Resource
    TopicMapper topicMapper;
    
    @Resource
    AccountMapper accountMapper;
    
    @Resource
    AccountDetailsMapper accountDetailsMapper;
    
    @Resource
    AccountPrivacyMapper privacyMapper;
    
    @Resource
    ContentFilter contentFilter;
    
    @Resource
    NotificationService notificationService;
    
    @Resource
    FlowUtils flowUtils;
    
    @Override
    @Transactional
    public String createComment(int uid, AddCommentVO vo) {
        // 频率限制
        if (!flowUtils.limitPeriodCounterCheck("comment_create_" + uid, 2, 60))
            return "评论频繁，请稍后再试";
        
        // 验证内容长度
        String content = vo.getContent();
        if (content == null || content.isBlank())
            return "评论内容不能为空";
        
        // 解析JSON内容获取实际文本
        String actualText;
        try {
            JSONObject contentObj = JSONObject.parseObject(content);
            StringBuilder textBuilder = new StringBuilder();
            contentObj.getJSONArray("ops").forEach(op -> {
                Object insert = JSONObject.from(op).get("insert");
                if (insert instanceof String) {
                    textBuilder.append(insert);
                }
            });
            actualText = textBuilder.toString();
        } catch (Exception e) {
            actualText = content;
        }
        
        if (actualText.length() > 5000)
            return "评论内容超过5000字符限制";
        
        // 内容过滤
        FilterResult filterResult = contentFilter.filter(actualText);
        if (!filterResult.isPassed()) {
            return "内容包含违禁词汇【" + filterResult.getForbiddenWord() + "】，请修改后再提交";
        }
        
        // 验证帖子存在
        TopicDTO topic = topicMapper.selectById(vo.getTid());
        if (topic == null)
            return "帖子不存在";
        
        // 验证父评论存在（如果是回复）
        if (vo.getQuote() > 0) {
            TopicCommentDTO parentComment = commentMapper.selectById(vo.getQuote());
            if (parentComment == null)
                return "被回复的评论不存在";
        }
        
        // 创建评论
        TopicCommentDTO comment = new TopicCommentDTO();
        comment.setUid(uid);
        comment.setTid(vo.getTid());
        comment.setContent(content);
        comment.setTime(new Date());
        comment.setQuote(vo.getQuote());
        comment.setLikeCount(0);
        comment.setHotScore(0.0);
        
        commentMapper.insert(comment);
        
        // 发送回复通知
        sendReplyNotification(uid, vo.getTid(), vo.getQuote());
        
        // 发送@提及通知
        sendMentionNotifications(uid, actualText, vo.getTid());
        
        return null;
    }
    
    private void sendReplyNotification(int uid, int tid, Integer quoteId) {
        AccountDTO commenter = accountMapper.selectById(uid);
        if (commenter == null) return;
        
        if (quoteId != null && quoteId > 0) {
            // 二级回复：通知父评论作者
            TopicCommentDTO parentComment = commentMapper.selectById(quoteId);
            if (parentComment != null && !parentComment.getUid().equals(uid)) {
                TopicDTO topic = topicMapper.selectById(tid);
                notificationService.addNotification(
                    parentComment.getUid(),
                    "您有新的评论回复",
                    commenter.getUsername() + " 回复了你发表的评论",
                    "success",
                    "/home/topic/" + tid
                );
            }
        } else {
            // 一级评论：通知帖子作者
            TopicDTO topic = topicMapper.selectById(tid);
            if (topic != null && !topic.getUid().equals(uid)) {
                notificationService.addNotification(
                    topic.getUid(),
                    "您有新的帖子评论",
                    commenter.getUsername() + " 评论了你的帖子：" + topic.getTitle(),
                    "success",
                    "/home/topic/" + tid
                );
            }
        }
    }
    
    private void sendMentionNotifications(int uid, String content, int tid) {
        List<Integer> mentionedUserIds = parseMentions(content);
        AccountDTO mentioner = accountMapper.selectById(uid);
        if (mentioner == null) return;
        
        for (Integer mentionedUid : mentionedUserIds) {
            if (!mentionedUid.equals(uid)) {
                notificationService.addNotification(
                    mentionedUid,
                    "有人@了你",
                    mentioner.getUsername() + " 在评论中提到了你",
                    "success",
                    "/home/topic/" + tid
                );
            }
        }
    }
    
    @Override
    @Transactional
    public String deleteComment(int uid, int commentId) {
        TopicCommentDTO comment = commentMapper.selectById(commentId);
        if (comment == null)
            return "评论不存在";
        
        // 验证权限
        AccountDTO operator = accountMapper.selectById(uid);
        boolean isAdmin = operator != null && "admin".equals(operator.getRole());
        
        if (!comment.getUid().equals(uid) && !isAdmin)
            return "只能删除自己的评论";
        
        // 删除所有二级回复
        commentMapper.delete(Wrappers.<TopicCommentDTO>query().eq("quote", commentId));
        
        // 删除所有点赞记录
        likeMapper.delete(Wrappers.<CommentLikeDTO>query().eq("comment_id", commentId));
        
        // 删除评论本身
        commentMapper.deleteById(commentId);
        
        return null;
    }
    
    @Override
    public CommentListVO listComments(int tid, int page, String sortBy, int uid) {
        Page<TopicCommentDTO> commentPage = Page.of(page + 1, 10);
        
        // 构建查询条件
        var query = Wrappers.<TopicCommentDTO>query()
            .eq("tid", tid)
            .isNull("quote"); // 只查询一级评论
        
        // 排序
        if ("hot".equals(sortBy)) {
            query.orderByDesc("hot_score");
        } else {
            query.orderByDesc("time");
        }
        
        commentMapper.selectPage(commentPage, query);
        
        // 转换为VO
        List<CommentVO> commentVOs = commentPage.getRecords().stream()
            .map(dto -> convertToCommentVO(dto, uid))
            .collect(Collectors.toList());
        
        // 查询二级回复
        for (CommentVO commentVO : commentVOs) {
            List<TopicCommentDTO> replies = commentMapper.selectList(
                Wrappers.<TopicCommentDTO>query()
                    .eq("quote", commentVO.getId())
                    .orderByAsc("time")
            );
            commentVO.setReplies(replies.stream()
                .map(dto -> convertToCommentVO(dto, uid))
                .collect(Collectors.toList()));
        }
        
        // 构建返回结果
        CommentListVO result = new CommentListVO();
        result.setComments(commentVOs);
        result.setHasMore(commentPage.hasNext());
        result.setTotal((int) commentPage.getTotal());
        
        return result;
    }
    
    private CommentVO convertToCommentVO(TopicCommentDTO dto, int currentUid) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(dto, vo);
        
        // 填充用户信息
        CommentVO.User user = new CommentVO.User();
        fillUserDetails(user, dto.getUid());
        vo.setUser(user);
        
        // 填充点赞信息
        vo.setLikeCount(dto.getLikeCount() != null ? dto.getLikeCount() : 0);
        vo.setLiked(isCommentLiked(dto.getId(), currentUid));
        
        // 处理引用内容
        if (dto.getQuote() != null && dto.getQuote() > 0) {
            TopicCommentDTO quotedComment = commentMapper.selectById(dto.getQuote());
            if (quotedComment != null) {
                try {
                    JSONObject content = JSONObject.parseObject(quotedComment.getContent());
                    StringBuilder builder = new StringBuilder();
                    content.getJSONArray("ops").forEach(op -> {
                        Object insert = JSONObject.from(op).get("insert");
                        if (insert instanceof String) {
                            builder.append(insert);
                        }
                    });
                    String quoteText = builder.toString();
                    vo.setQuote(quoteText.length() > 50 ? quoteText.substring(0, 50) + "..." : quoteText);
                } catch (Exception e) {
                    vo.setQuote("引用内容解析失败");
                }
            } else {
                vo.setQuote("此评论已被删除");
            }
        }
        
        return vo;
    }
    
    private void fillUserDetails(CommentVO.User user, int uid) {
        AccountDTO account = accountMapper.selectById(uid);
        AccountDetailsDTO details = accountDetailsMapper.selectById(uid);
        AccountPrivacyDTO privacy = privacyMapper.selectById(uid);
        
        String[] ignores = (privacy == null) ? new String[0] : privacy.hiddenFields();
        BeanUtils.copyProperties(account, user, ignores);
        if (details != null) {
            BeanUtils.copyProperties(details, user, ignores);
        }
    }
    
    private boolean isCommentLiked(int commentId, int uid) {
        return likeMapper.selectCount(
            Wrappers.<CommentLikeDTO>query()
                .eq("comment_id", commentId)
                .eq("uid", uid)
        ) > 0;
    }
    
    @Override
    @Transactional
    public void likeComment(int uid, int commentId, boolean state) {
        boolean currentlyLiked = isCommentLiked(commentId, uid);
        
        if (state && !currentlyLiked) {
            // 点赞
            CommentLikeDTO like = new CommentLikeDTO();
            like.setCommentId(commentId);
            like.setUid(uid);
            like.setTime(new Date());
            likeMapper.insert(like);
            
            // 增加点赞数
            commentMapper.update(null, Wrappers.<TopicCommentDTO>update()
                .eq("id", commentId)
                .setSql("like_count = like_count + 1"));
            
            // 更新热度分数
            updateHotScore(commentId);
            
        } else if (!state && currentlyLiked) {
            // 取消点赞
            likeMapper.delete(Wrappers.<CommentLikeDTO>query()
                .eq("comment_id", commentId)
                .eq("uid", uid));
            
            // 减少点赞数
            commentMapper.update(null, Wrappers.<TopicCommentDTO>update()
                .eq("id", commentId)
                .setSql("like_count = GREATEST(like_count - 1, 0)"));
            
            // 更新热度分数
            updateHotScore(commentId);
        }
        // 如果state与当前状态一致，保持幂等性，不做操作
    }
    
    private void updateHotScore(int commentId) {
        TopicCommentDTO comment = commentMapper.selectById(commentId);
        if (comment != null) {
            double hotScore = calculateHotScore(
                comment.getLikeCount() != null ? comment.getLikeCount() : 0,
                comment.getTime()
            );
            commentMapper.update(null, Wrappers.<TopicCommentDTO>update()
                .eq("id", commentId)
                .set("hot_score", hotScore));
        }
    }
    
    @Override
    public double calculateHotScore(int likeCount, Date createTime) {
        long hoursSinceCreation = (System.currentTimeMillis() - createTime.getTime()) / (1000 * 60 * 60);
        return likeCount / Math.pow(hoursSinceCreation + 2, 1.5);
    }
    
    @Override
    public List<Integer> parseMentions(String content) {
        List<Integer> mentionedUserIds = new ArrayList<>();
        if (content == null || content.isBlank()) return mentionedUserIds;
        
        // 正则匹配 @用户名
        Pattern pattern = Pattern.compile("@([a-zA-Z0-9_\\u4e00-\\u9fa5]+)");
        Matcher matcher = pattern.matcher(content);
        
        Set<String> usernames = new HashSet<>();
        while (matcher.find()) {
            usernames.add(matcher.group(1));
        }
        
        // 查询用户ID
        for (String username : usernames) {
            AccountDTO account = accountMapper.selectOne(
                Wrappers.<AccountDTO>query().eq("username", username)
            );
            if (account != null) {
                mentionedUserIds.add(account.getId());
            }
        }
        
        return mentionedUserIds;
    }
    
    @Override
    @Transactional
    public String reportComment(int uid, int commentId, String reason) {
        // 验证评论存在
        TopicCommentDTO comment = commentMapper.selectById(commentId);
        if (comment == null)
            return "评论不存在";
        
        // 检查是否已举报
        long count = reportMapper.selectCount(
            Wrappers.<CommentReportDTO>query()
                .eq("comment_id", commentId)
                .eq("reporter_uid", uid)
        );
        if (count > 0)
            return "您已经举报过该评论";
        
        // 创建举报记录
        CommentReportDTO report = new CommentReportDTO();
        report.setCommentId(commentId);
        report.setReporterUid(uid);
        report.setReason(reason);
        report.setStatus("pending");
        report.setTime(new Date());
        
        reportMapper.insert(report);
        
        return null;
    }
    
    @Override
    public ReportListVO listReports(int page) {
        Page<CommentReportDTO> reportPage = Page.of(page + 1, 20);
        
        reportMapper.selectPage(reportPage, 
            Wrappers.<CommentReportDTO>query()
                .eq("status", "pending")
                .orderByDesc("time")
        );
        
        List<ReportVO> reportVOs = reportPage.getRecords().stream()
            .map(this::convertToReportVO)
            .collect(Collectors.toList());
        
        ReportListVO result = new ReportListVO();
        result.setReports(reportVOs);
        result.setTotal((int) reportPage.getTotal());
        
        return result;
    }
    
    private ReportVO convertToReportVO(CommentReportDTO dto) {
        ReportVO vo = new ReportVO();
        BeanUtils.copyProperties(dto, vo);
        
        // 填充评论信息
        TopicCommentDTO comment = commentMapper.selectById(dto.getCommentId());
        if (comment != null) {
            try {
                JSONObject content = JSONObject.parseObject(comment.getContent());
                StringBuilder builder = new StringBuilder();
                content.getJSONArray("ops").forEach(op -> {
                    Object insert = JSONObject.from(op).get("insert");
                    if (insert instanceof String) {
                        builder.append(insert);
                    }
                });
                String text = builder.toString();
                vo.setCommentContent(text.length() > 100 ? text.substring(0, 100) + "..." : text);
            } catch (Exception e) {
                vo.setCommentContent("内容解析失败");
            }
            
            AccountDTO commentAuthor = accountMapper.selectById(comment.getUid());
            vo.setCommentAuthor(commentAuthor != null ? commentAuthor.getUsername() : "未知用户");
        }
        
        // 填充举报人信息
        AccountDTO reporter = accountMapper.selectById(dto.getReporterUid());
        vo.setReporterName(reporter != null ? reporter.getUsername() : "未知用户");
        
        return vo;
    }
    
    @Override
    @Transactional
    public String handleReport(int adminUid, int reportId, String action) {
        // 验证管理员权限
        AccountDTO admin = accountMapper.selectById(adminUid);
        if (admin == null || !"admin".equals(admin.getRole()))
            return "无权限执行此操作";
        
        // 验证举报记录存在
        CommentReportDTO report = reportMapper.selectById(reportId);
        if (report == null)
            return "举报记录不存在";
        
        if ("delete".equals(action)) {
            // 删除被举报的评论
            deleteComment(adminUid, report.getCommentId());
        }
        
        // 更新举报状态
        report.setStatus("handled");
        report.setHandlerUid(adminUid);
        report.setHandleTime(new Date());
        report.setHandleAction(action);
        reportMapper.updateById(report);
        
        return null;
    }
}
