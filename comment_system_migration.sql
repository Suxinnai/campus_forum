-- ===================================================================
-- 数据库迁移脚本：内嵌评论系统
-- 描述：为评论系统添加点赞、热度排序和举报功能
-- 相关需求：15.1, 15.2, 15.3, 15.4, 15.5, 15.6
-- ===================================================================

USE campus_forum;

-- -------------------------------------------------------------------
-- 1. 修改 db_topic_comment 表，添加点赞数和热度分数字段
-- -------------------------------------------------------------------
ALTER TABLE db_topic_comment 
ADD COLUMN like_count INT DEFAULT 0 COMMENT '点赞数',
ADD COLUMN hot_score DOUBLE DEFAULT 0 COMMENT '热度分数（基于点赞数和时间计算）';

-- 为热度排序添加复合索引
ALTER TABLE db_topic_comment 
ADD INDEX idx_hot_score (tid, hot_score DESC);

-- -------------------------------------------------------------------
-- 2. 创建评论点赞表 (db_comment_like)
-- 用途：记录用户对评论的点赞关系，防止重复点赞
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS db_comment_like (
    comment_id INT NOT NULL COMMENT '评论ID',
    uid        INT NOT NULL COMMENT '用户ID',
    time       DATETIME NOT NULL DEFAULT NOW() COMMENT '点赞时间',
    PRIMARY KEY (comment_id, uid),
    INDEX idx_uid (uid),
    FOREIGN KEY (comment_id) REFERENCES db_topic_comment(id) ON DELETE CASCADE,
    FOREIGN KEY (uid) REFERENCES db_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞关系表';

-- -------------------------------------------------------------------
-- 3. 创建评论举报表 (db_comment_report)
-- 用途：记录用户举报的评论，供管理员审核处理
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS db_comment_report (
    id           INT PRIMARY KEY AUTO_INCREMENT,
    comment_id   INT          NOT NULL COMMENT '被举报的评论ID',
    reporter_uid INT          NOT NULL COMMENT '举报人ID',
    reason       VARCHAR(500) NOT NULL COMMENT '举报原因',
    status       VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '状态: pending=待处理, handled=已处理',
    time         DATETIME     NOT NULL DEFAULT NOW() COMMENT '举报时间',
    handler_uid  INT          DEFAULT NULL COMMENT '处理人ID（管理员）',
    handle_time  DATETIME     DEFAULT NULL COMMENT '处理时间',
    handle_action VARCHAR(20) DEFAULT NULL COMMENT '处理动作: delete=删除评论, reject=驳回举报',
    INDEX idx_status (status, time DESC),
    INDEX idx_comment (comment_id),
    UNIQUE KEY uk_reporter_comment (reporter_uid, comment_id),
    FOREIGN KEY (comment_id) REFERENCES db_topic_comment(id) ON DELETE CASCADE,
    FOREIGN KEY (reporter_uid) REFERENCES db_account(id) ON DELETE CASCADE,
    FOREIGN KEY (handler_uid) REFERENCES db_account(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论举报表';

-- -------------------------------------------------------------------
-- 4. 数据完整性说明
-- -------------------------------------------------------------------
-- 外键约束说明：
-- - db_comment_like.comment_id -> db_topic_comment.id (级联删除)
-- - db_comment_like.uid -> db_account.id (级联删除)
-- - db_comment_report.comment_id -> db_topic_comment.id (级联删除)
-- - db_comment_report.reporter_uid -> db_account.id (级联删除)
-- - db_comment_report.handler_uid -> db_account.id (删除时设为NULL)
--
-- 唯一约束说明：
-- - db_comment_like: (comment_id, uid) 防止用户重复点赞同一评论
-- - db_comment_report: (reporter_uid, comment_id) 防止用户重复举报同一评论
--
-- 索引说明：
-- - idx_hot_score: 支持按热度排序查询评论列表
-- - idx_uid: 支持查询用户的点赞记录
-- - idx_status: 支持管理员查询待处理举报列表
-- - idx_comment: 支持查询特定评论的举报记录
