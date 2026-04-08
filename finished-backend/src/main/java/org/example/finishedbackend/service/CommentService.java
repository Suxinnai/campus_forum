package org.example.finishedbackend.service;

import org.example.finishedbackend.entity.VO.request.AddCommentVO;
import org.example.finishedbackend.entity.VO.response.CommentListVO;
import org.example.finishedbackend.entity.VO.response.ReportListVO;

import java.util.List;

public interface CommentService {
    /**
     * 创建评论
     * @param uid 用户ID
     * @param vo 评论内容
     * @return 错误信息，成功返回null
     */
    String createComment(int uid, AddCommentVO vo);
    
    /**
     * 删除评论
     * @param uid 用户ID
     * @param commentId 评论ID
     * @return 错误信息，成功返回null
     */
    String deleteComment(int uid, int commentId);
    
    /**
     * 查询评论列表
     * @param tid 帖子ID
     * @param page 页码
     * @param sortBy 排序方式（time/hot）
     * @param uid 当前用户ID
     * @return 评论列表
     */
    CommentListVO listComments(int tid, int page, String sortBy, int uid);
    
    /**
     * 点赞/取消点赞评论
     * @param uid 用户ID
     * @param commentId 评论ID
     * @param state true=点赞，false=取消点赞
     */
    void likeComment(int uid, int commentId, boolean state);
    
    /**
     * 举报评论
     * @param uid 举报人ID
     * @param commentId 评论ID
     * @param reason 举报原因
     * @return 错误信息，成功返回null
     */
    String reportComment(int uid, int commentId, String reason);
    
    /**
     * 查询举报列表（管理员）
     * @param page 页码
     * @return 举报列表
     */
    ReportListVO listReports(int page);
    
    /**
     * 处理举报（管理员）
     * @param adminUid 管理员ID
     * @param reportId 举报ID
     * @param action 处理动作（delete/reject）
     * @return 错误信息，成功返回null
     */
    String handleReport(int adminUid, int reportId, String action);
    
    /**
     * 解析@提及
     * @param content 评论内容
     * @return 被提及用户的ID列表
     */
    List<Integer> parseMentions(String content);
    
    /**
     * 计算热度分数
     * @param likeCount 点赞数
     * @param createTime 创建时间
     * @return 热度分数
     */
    double calculateHotScore(int likeCount, java.util.Date createTime);
}
