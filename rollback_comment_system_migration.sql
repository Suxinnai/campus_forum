-- ===================================================================
-- 回滚脚本：评论系统数据库迁移
-- 用途：撤销评论系统迁移的所有更改
-- 警告：此操作将删除所有点赞和举报数据，请谨慎执行！
-- ===================================================================

USE campus_forum;

-- -------------------------------------------------------------------
-- 警告提示
-- -------------------------------------------------------------------
-- 执行此脚本前，请确认：
-- 1. 已备份数据库
-- 2. 已停止应用服务
-- 3. 确实需要回滚迁移
-- -------------------------------------------------------------------

-- -------------------------------------------------------------------
-- 1. 删除评论举报表
-- -------------------------------------------------------------------
DROP TABLE IF EXISTS db_comment_report;

-- 验证：表应该不存在
-- SELECT COUNT(*) FROM information_schema.TABLES 
-- WHERE TABLE_SCHEMA = 'campus_forum' AND TABLE_NAME = 'db_comment_report';
-- 预期结果：0

-- -------------------------------------------------------------------
-- 2. 删除评论点赞表
-- -------------------------------------------------------------------
DROP TABLE IF EXISTS db_comment_like;

-- 验证：表应该不存在
-- SELECT COUNT(*) FROM information_schema.TABLES 
-- WHERE TABLE_SCHEMA = 'campus_forum' AND TABLE_NAME = 'db_comment_like';
-- 预期结果：0

-- -------------------------------------------------------------------
-- 3. 删除 db_topic_comment 表的新增字段和索引
-- -------------------------------------------------------------------
-- 3.1 删除热度索引
ALTER TABLE db_topic_comment DROP INDEX IF EXISTS idx_hot_score;

-- 3.2 删除热度分数字段
ALTER TABLE db_topic_comment DROP COLUMN IF EXISTS hot_score;

-- 3.3 删除点赞数字段
ALTER TABLE db_topic_comment DROP COLUMN IF EXISTS like_count;

-- 验证：字段应该不存在
-- SELECT COUNT(*) FROM information_schema.COLUMNS 
-- WHERE TABLE_SCHEMA = 'campus_forum' 
-- AND TABLE_NAME = 'db_topic_comment' 
-- AND COLUMN_NAME IN ('like_count', 'hot_score');
-- 预期结果：0

-- -------------------------------------------------------------------
-- 4. 回滚验证
-- -------------------------------------------------------------------
SELECT 
    '回滚完成' AS status,
    (SELECT COUNT(*) FROM information_schema.TABLES 
     WHERE TABLE_SCHEMA = 'campus_forum' 
     AND TABLE_NAME IN ('db_comment_like', 'db_comment_report')) AS remaining_tables,
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = 'campus_forum' 
     AND TABLE_NAME = 'db_topic_comment' 
     AND COLUMN_NAME IN ('like_count', 'hot_score')) AS remaining_columns;

-- 预期结果：
-- remaining_tables: 0
-- remaining_columns: 0

-- -------------------------------------------------------------------
-- 5. 回滚后的数据库状态
-- -------------------------------------------------------------------
-- 回滚后，db_topic_comment 表应该恢复到原始状态：
-- - id: INT PRIMARY KEY AUTO_INCREMENT
-- - uid: INT NOT NULL
-- - tid: INT NOT NULL
-- - content: TEXT NOT NULL
-- - time: DATETIME NOT NULL DEFAULT NOW()
-- - quote: INT DEFAULT NULL
-- - INDEX idx_tid (tid)
-- - FOREIGN KEY (uid) REFERENCES db_account(id) ON DELETE CASCADE
-- - FOREIGN KEY (tid) REFERENCES db_topic(id) ON DELETE CASCADE
