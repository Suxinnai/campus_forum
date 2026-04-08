package org.example.finishedbackend.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.finishedbackend.entity.RestBean;
import org.example.finishedbackend.entity.VO.request.AddCommentVO;
import org.example.finishedbackend.entity.VO.response.CommentListVO;
import org.example.finishedbackend.entity.VO.response.ReportListVO;
import org.example.finishedbackend.service.CommentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forum/comment")
public class CommentController {
    
    @Resource
    CommentService commentService;
    
    @PostMapping("/add")
    public RestBean<Void> addComment(@RequestBody @Valid AddCommentVO vo,
                                      @RequestAttribute("id") int uid) {
        String result = commentService.createComment(uid, vo);
        return result == null ? RestBean.success("评论成功") : RestBean.failure(400, result);
    }
    
    @DeleteMapping("/delete/{id}")
    public RestBean<Void> deleteComment(@PathVariable int id,
                                         @RequestAttribute("id") int uid) {
        String result = commentService.deleteComment(uid, id);
        return result == null ? RestBean.success("删除成功") : RestBean.failure(400, result);
    }
    
    @GetMapping("/list")
    public RestBean<CommentListVO> listComments(@RequestParam int tid,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "time") String sortBy,
                                                 @RequestAttribute("id") int uid) {
        return RestBean.success(commentService.listComments(tid, page, sortBy, uid), null);
    }
    
    @PostMapping("/like")
    public RestBean<Void> likeComment(@RequestBody LikeRequest request,
                                       @RequestAttribute("id") int uid) {
        commentService.likeComment(uid, request.commentId, request.state);
        return RestBean.success("操作成功");
    }
    
    @PostMapping("/report")
    public RestBean<Void> reportComment(@RequestBody ReportRequest request,
                                         @RequestAttribute("id") int uid) {
        String result = commentService.reportComment(uid, request.commentId, request.reason);
        return result == null ? RestBean.success("举报成功") : RestBean.failure(400, result);
    }
    
    @GetMapping("/report/list")
    public RestBean<ReportListVO> listReports(@RequestParam(defaultValue = "0") int page,
                                               @RequestAttribute("id") int uid) {
        // 验证管理员权限在Service层处理
        return RestBean.success(commentService.listReports(page), null);
    }
    
    @PostMapping("/report/handle")
    public RestBean<Void> handleReport(@RequestBody HandleReportRequest request,
                                        @RequestAttribute("id") int uid) {
        String result = commentService.handleReport(uid, request.reportId, request.action);
        return result == null ? RestBean.success("处理成功") : RestBean.failure(400, result);
    }
    
    // 内部请求类
    public static class LikeRequest {
        public int commentId;
        public boolean state;
    }
    
    public static class ReportRequest {
        public int commentId;
        public String reason;
    }
    
    public static class HandleReportRequest {
        public int reportId;
        public String action;
    }
}
