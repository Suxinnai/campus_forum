# 评论系统数据库迁移说明

## 概述

本迁移脚本为校园论坛的内嵌评论系统添加以下功能：
- 评论点赞功能
- 评论热度排序
- 评论举报和审核机制

## 迁移内容

### 1. 修改现有表
- **db_topic_comment**: 添加 `like_count` 和 `hot_score` 字段，以及热度排序索引

### 2. 新建表
- **db_comment_like**: 评论点赞关系表
- **db_comment_report**: 评论举报表

### 3. 数据完整性保障
- 外键约束：确保数据引用完整性
- 唯一约束：防止重复点赞和重复举报
- 级联删除：自动清理关联数据

## 执行方法

### 方法一：使用 MySQL 命令行

```bash
mysql -u root -p < comment_system_migration.sql
```

### 方法二：使用 MySQL Workbench 或其他 GUI 工具

1. 打开 `comment_system_migration.sql` 文件
2. 连接到 `campus_forum` 数据库
3. 执行整个脚本

### 方法三：使用 Docker（如果数据库在容器中）

```bash
docker exec -i mysql_container mysql -u root -p campus_forum < comment_system_migration.sql
```

## 验证迁移

执行以下 SQL 语句验证迁移是否成功：

```sql
-- 1. 检查 db_topic_comment 表的新字段
DESC db_topic_comment;

-- 2. 检查新表是否创建成功
SHOW TABLES LIKE 'db_comment_%';

-- 3. 检查索引是否创建成功
SHOW INDEX FROM db_topic_comment WHERE Key_name = 'idx_hot_score';
SHOW INDEX FROM db_comment_like;
SHOW INDEX FROM db_comment_report;

-- 4. 检查外键约束
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    REFERENCED_TABLE_NAME
FROM 
    information_schema.KEY_COLUMN_USAGE
WHERE 
    TABLE_SCHEMA = 'campus_forum'
    AND TABLE_NAME IN ('db_comment_like', 'db_comment_report');
```

## 回滚方法

如果需要回滚此迁移，执行以下 SQL：

```sql
USE campus_forum;

-- 删除新建的表
DROP TABLE IF EXISTS db_comment_report;
DROP TABLE IF EXISTS db_comment_like;

-- 删除新增的字段和索引
ALTER TABLE db_topic_comment 
DROP INDEX idx_hot_score,
DROP COLUMN hot_score,
DROP COLUMN like_count;
```

## 注意事项

1. **备份数据库**：执行迁移前，请务必备份 `campus_forum` 数据库
2. **停止应用**：建议在执行迁移时停止应用服务，避免数据不一致
3. **权限检查**：确保数据库用户有 ALTER TABLE 和 CREATE TABLE 权限
4. **测试环境**：建议先在测试环境执行迁移，验证无误后再在生产环境执行

## 相关需求

此迁移脚本实现了以下需求：
- 需求 15.1: 验证评论关联的帖子ID存在
- 需求 15.2: 帖子不存在时拒绝评论发布
- 需求 15.3: 验证回复关联的父评论ID存在
- 需求 15.4: 父评论不存在时拒绝回复发布
- 需求 15.5: 验证用户ID存在
- 需求 15.6: 用户不存在时拒绝评论发布
