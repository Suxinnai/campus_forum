-- =====================================================
-- 管理后台修复补丁 SQL
-- 确保 db_feedback 和 db_schedule 表存在
-- 执行方式: 在 MySQL 中直接执行此文件
-- =====================================================
USE campus_forum;

-- 创建用户反馈表（如果不存在）
CREATE TABLE IF NOT EXISTS db_feedback (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    uid         INT          NOT NULL COMMENT '提交人ID',
    type        VARCHAR(20)  NOT NULL DEFAULT 'suggestion' COMMENT 'bug/suggestion/other',
    title       VARCHAR(100) NOT NULL,
    content     TEXT         NOT NULL,
    contact     VARCHAR(100) DEFAULT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT 'pending/resolved',
    create_time DATETIME     NOT NULL DEFAULT NOW(),
    INDEX idx_uid (uid),
    FOREIGN KEY (uid) REFERENCES db_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建校历日程表（如果不存在）
CREATE TABLE IF NOT EXISTS db_schedule (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(100) NOT NULL COMMENT '日程标题',
    description VARCHAR(500) DEFAULT NULL COMMENT '日程描述',
    event_date  DATE         NOT NULL COMMENT '开始日期',
    end_date    DATE         DEFAULT NULL COMMENT '结束日期（可选）',
    type        VARCHAR(20)  NOT NULL DEFAULT 'event' COMMENT 'semester/exam/holiday/event',
    create_time DATETIME     NOT NULL DEFAULT NOW()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 校历日程初始数据（如果表为空则插入）
INSERT INTO db_schedule (title, event_date, end_date, type, description)
SELECT * FROM (VALUES
    ROW('2025-2026学年第二学期开学', '2026-02-24', NULL, 'semester', '新学期正式上课'),
    ROW('清明节放假', '2026-04-04', '2026-04-06', 'holiday', '清明节法定假日'),
    ROW('期中考试周', '2026-04-20', '2026-04-26', 'exam', '2025-2026学年第二学期期中考试'),
    ROW('五一劳动节放假', '2026-05-01', '2026-05-05', 'holiday', '劳动节法定假日'),
    ROW('期末考试周', '2026-06-20', '2026-06-30', 'exam', '2025-2026学年第二学期期末考试'),
    ROW('暑假开始', '2026-07-05', '2026-08-30', 'holiday', '暑假')
) AS tmp(title, event_date, end_date, type, description)
WHERE NOT EXISTS (SELECT 1 FROM db_schedule LIMIT 1);

-- 确保帖子表有 top, featured, status 字段
ALTER TABLE db_topic ADD COLUMN IF NOT EXISTS `top`      TINYINT(1)  NOT NULL DEFAULT 0    COMMENT '是否置顶 1=置顶 0=普通';
ALTER TABLE db_topic ADD COLUMN IF NOT EXISTS `status`   VARCHAR(20) NOT NULL DEFAULT 'approved' COMMENT '审核状态: pending/approved/rejected';
ALTER TABLE db_topic ADD COLUMN IF NOT EXISTS `featured` TINYINT(1)  NOT NULL DEFAULT 0    COMMENT '是否精华 1=精华 0=普通';

-- 确保用户表有 banned, moderator_type 字段
ALTER TABLE db_account ADD COLUMN IF NOT EXISTS `banned`         TINYINT(1) NOT NULL DEFAULT 0    COMMENT '是否封禁';
ALTER TABLE db_account ADD COLUMN IF NOT EXISTS `moderator_type` INT        DEFAULT NULL          COMMENT '版主负责的版块ID';

SELECT '✅ 管理后台数据库修复完成' AS result;
