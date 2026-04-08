-- ===================================================================
-- 验证脚本：评论系统数据库迁移
-- 用途：验证迁移脚本是否成功执行
-- ===================================================================

USE campus_forum;

-- -------------------------------------------------------------------
-- 1. 验证 db_topic_comment 表的新字段
-- -------------------------------------------------------------------
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM 
    information_schema.COLUMNS
WHERE 
    TABLE_SCHEMA = 'campus_forum'
    AND TABLE_NAME = 'db_topic_comment'
    AND COLUMN_NAME IN ('like_count', 'hot_score');

-- 预期结果：应该返回 2 行记录
-- like_count: INT, DEFAULT 0, COMMENT '点赞数'
-- hot_score: DOUBLE, DEFAULT 0, COMMENT '热度分数（基于点赞数和时间计算）'

-- -------------------------------------------------------------------
-- 2. 验证新表是否创建成功
-- -------------------------------------------------------------------
SELECT 
    TABLE_NAME,
    TABLE_COMMENT
FROM 
    information_schema.TABLES
WHERE 
    TABLE_SCHEMA = 'campus_forum'
    AND TABLE_NAME IN ('db_comment_like', 'db_comment_report');

-- 预期结果：应该返回 2 行记录
-- db_comment_like: 评论点赞关系表
-- db_comment_report: 评论举报表

-- -------------------------------------------------------------------
-- 3. 验证索引是否创建成功
-- -------------------------------------------------------------------
-- 3.1 验证 db_topic_comment 的热度索引
SELECT 
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX
FROM 
    information_schema.STATISTICS
WHERE 
    TABLE_SCHEMA = 'campus_forum'
    AND TABLE_NAME = 'db_topic_comment'
    AND INDEX_NAME = 'idx_hot_score';

-- 预期结果：应该返回 2 行记录（tid, hot_score）

-- 3.2 验证 db_comment_like 的索引
SELECT 
    INDEX_NAME,
    COLUMN_NAME,
    NON_UNIQUE
FROM 
    information_schema.STATISTICS
WHERE 
    TABLE_SCHEMA = 'campus_forum'
    AND TABLE_NAME = 'db_comment_like';

-- 预期结果：应该包含 PRIMARY (comment_id, uid) 和 idx_uid (uid)

-- 3.3 验证 db_comment_report 的索引
SELECT 
    INDEX_NAME,
    COLUMN_NAME,
    NON_UNIQUE
FROM 
    information_schema.STATISTICS
WHERE 
    TABLE_SCHEMA = 'campus_forum'
    AND TABLE_NAME = 'db_comment_report';

-- 预期结果：应该包含 PRIMARY, idx_status, idx_comment, uk_reporter_comment

-- -------------------------------------------------------------------
-- 4. 验证外键约束
-- -------------------------------------------------------------------
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME,
    DELETE_RULE
FROM 
    information_schema.KEY_COLUMN_USAGE
    JOIN information_schema.REFERENTIAL_CONSTRAINTS USING (CONSTRAINT_NAME, CONSTRAINT_SCHEMA)
WHERE 
    CONSTRAINT_SCHEMA = 'campus_forum'
    AND TABLE_NAME IN ('db_comment_like', 'db_comment_report');

-- 预期结果：应该返回 5 条外键约束记录
-- db_comment_like: comment_id -> db_topic_comment.id (CASCADE)
-- db_comment_like: uid -> db_account.id (CASCADE)
-- db_comment_report: comment_id -> db_topic_comment.id (CASCADE)
-- db_comment_report: reporter_uid -> db_account.id (CASCADE)
-- db_comment_report: handler_uid -> db_account.id (SET NULL)

-- -------------------------------------------------------------------
-- 5. 验证唯一约束
-- -------------------------------------------------------------------
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME
FROM 
    information_schema.KEY_COLUMN_USAGE
WHERE 
    TABLE_SCHEMA = 'campus_forum'
    AND CONSTRAINT_NAME = 'uk_reporter_comment';

-- 预期结果：应该返回 2 行记录（reporter_uid, comment_id）

-- -------------------------------------------------------------------
-- 6. 完整性测试（可选）
-- -------------------------------------------------------------------
-- 注意：以下测试会插入和删除数据，仅在测试环境执行

-- 6.1 测试点赞功能（需要有效的 comment_id 和 uid）
-- INSERT INTO db_comment_like (comment_id, uid) VALUES (1, 1);
-- SELECT * FROM db_comment_like WHERE comment_id = 1 AND uid = 1;
-- DELETE FROM db_comment_like WHERE comment_id = 1 AND uid = 1;

-- 6.2 测试举报功能（需要有效的 comment_id 和 reporter_uid）
-- INSERT INTO db_comment_report (comment_id, reporter_uid, reason) VALUES (1, 1, '测试举报');
-- SELECT * FROM db_comment_report WHERE comment_id = 1 AND reporter_uid = 1;
-- DELETE FROM db_comment_report WHERE comment_id = 1 AND reporter_uid = 1;

-- -------------------------------------------------------------------
-- 7. 汇总报告
-- -------------------------------------------------------------------
SELECT 
    '迁移验证完成' AS status,
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = 'campus_forum' 
     AND TABLE_NAME = 'db_topic_comment' 
     AND COLUMN_NAME IN ('like_count', 'hot_score')) AS new_columns_count,
    (SELECT COUNT(*) FROM information_schema.TABLES 
     WHERE TABLE_SCHEMA = 'campus_forum' 
     AND TABLE_NAME IN ('db_comment_like', 'db_comment_report')) AS new_tables_count,
    (SELECT COUNT(*) FROM information_schema.STATISTICS 
     WHERE TABLE_SCHEMA = 'campus_forum' 
     AND TABLE_NAME = 'db_topic_comment' 
     AND INDEX_NAME = 'idx_hot_score') AS hot_score_index_count;

-- 预期结果：
-- new_columns_count: 2
-- new_tables_count: 2
-- hot_score_index_count: 2 (tid 和 hot_score 两列)
