-- =====================================================
-- 重置账号脚本
-- 密码统一为: 123456
-- BCrypt Hash: $2a$10$2se33xOEcd.5z.5F9EB3h.mL1e4X2ey3AnUtIwdKbY/CeiEt/ou7G
-- =====================================================

USE campus_forum;

-- 先清空所有关联表数据 (由于外键约束)
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM db_topic_comment;
DELETE FROM db_topic_interact_like;
DELETE FROM db_topic_interact_collect;
DELETE FROM db_image_store;
DELETE FROM db_notification;
DELETE FROM db_user_behavior;
DELETE FROM db_confession;
DELETE FROM db_lost_found;
DELETE FROM db_topic;
DELETE FROM db_resource;
DELETE FROM db_account_privacy;
DELETE FROM db_account_details;
DELETE FROM db_account;
SET FOREIGN_KEY_CHECKS = 1;

-- 重置自增计数器
ALTER TABLE db_account AUTO_INCREMENT = 1;

-- 插入管理员账号
INSERT INTO db_account (username, password, email, role) VALUES
('admin', '$2a$10$2se33xOEcd.5z.5F9EB3h.mL1e4X2ey3AnUtIwdKbY/CeiEt/ou7G', 'admin@qingyanshe.com', 'admin');

-- 插入测试账号
INSERT INTO db_account (username, password, email, role) VALUES
('test', '$2a$10$2se33xOEcd.5z.5F9EB3h.mL1e4X2ey3AnUtIwdKbY/CeiEt/ou7G', 'test@qingyanshe.com', 'user');

-- 为新账号创建详情和隐私记录
INSERT INTO db_account_details (id, gender, phone, qq, `desc`) VALUES
(1, 1, '13800000001', '10001', '系统管理员'),
(2, 0, NULL, NULL, '测试用户');

INSERT INTO db_account_privacy (id, phone, email, qq, gender) VALUES
(1, 1, 1, 1, 1),
(2, 1, 1, 1, 1);

-- 验证结果
SELECT id, username, email, role FROM db_account;
