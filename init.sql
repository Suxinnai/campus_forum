-- =====================================================
-- Campus Forum 数据库初始化脚本
-- 基于 Spring Boot 后端 DTO 实体类逆向生成
-- =====================================================

CREATE DATABASE IF NOT EXISTS campus_forum DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_forum;

-- ---------------------------------------------------
-- 1. 用户账号表 (db_account)
-- 对应实体: AccountDTO.java
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS db_account (
    id            INT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    avatar        VARCHAR(255) DEFAULT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    role          VARCHAR(20)  NOT NULL DEFAULT 'user',
    register_time DATETIME     NOT NULL DEFAULT NOW(),
    INDEX idx_email (email),
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------
-- 2. 用户详情表 (db_account_details)
-- 对应实体: AccountDetailsDTO.java
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS db_account_details (
    id     INT PRIMARY KEY,
    gender INT          NOT NULL DEFAULT 0 COMMENT '0=未知, 1=男, 2=女',
    phone  VARCHAR(20)  DEFAULT NULL,
    qq     VARCHAR(20)  DEFAULT NULL,
    `desc` VARCHAR(500) DEFAULT NULL,
    cover  VARCHAR(255) DEFAULT NULL,
    FOREIGN KEY (id) REFERENCES db_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------
-- 3. 用户隐私设置表 (db_account_privacy)
-- 对应实体: AccountPrivacyDTO.java
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS db_account_privacy (
    id     INT PRIMARY KEY,
    phone  TINYINT(1) NOT NULL DEFAULT 1,
    email  TINYINT(1) NOT NULL DEFAULT 1,
    qq     TINYINT(1) NOT NULL DEFAULT 1,
    gender TINYINT(1) NOT NULL DEFAULT 1,
    FOREIGN KEY (id) REFERENCES db_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------
-- 4. 帖子分类表 (db_topic_type)
-- 对应实体: TopicTypeDTO.java
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS db_topic_type (
    id    INT PRIMARY KEY AUTO_INCREMENT,
    name  VARCHAR(50)  NOT NULL,
    `desc` VARCHAR(255) DEFAULT NULL,
    color VARCHAR(20)  DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 预置一些帖子分类
INSERT INTO db_topic_type (name, `desc`, color) VALUES
    ('日常', '校园日常生活分享', '#1E90FF'),
    ('学习', '学习资料与经验交流', '#32CD32'),
    ('活动', '校园活动发布与讨论', '#FF8C00'),
    ('吐槽', '校园吐槽与趣事', '#FF4500'),
    ('求助', '问题咨询与互相帮助', '#9370DB');

-- ---------------------------------------------------
-- 5. 帖子表 (db_topic)
-- 对应实体: TopicDTO.java
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS db_topic (
    id      INT PRIMARY KEY AUTO_INCREMENT,
    title   VARCHAR(255) NOT NULL,
    content TEXT         NOT NULL,
    type    INT          DEFAULT NULL COMMENT '分类ID, 关联 db_topic_type',
    time    DATETIME     NOT NULL DEFAULT NOW(),
    uid     INT          NOT NULL COMMENT '发帖人ID',
    INDEX idx_uid (uid),
    INDEX idx_type (type),
    INDEX idx_time (time),
    FOREIGN KEY (uid) REFERENCES db_account(id) ON DELETE CASCADE,
    FOREIGN KEY (type) REFERENCES db_topic_type(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------
-- 6. 帖子评论表 (db_topic_comment)
-- 对应实体: TopicCommentDTO.java
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS db_topic_comment (
    id      INT PRIMARY KEY AUTO_INCREMENT,
    uid     INT      NOT NULL COMMENT '评论人ID',
    tid     INT      NOT NULL COMMENT '帖子ID',
    content TEXT     NOT NULL,
    time    DATETIME NOT NULL DEFAULT NOW(),
    quote   INT      DEFAULT NULL COMMENT '引用的评论ID, 可为空',
    INDEX idx_tid (tid),
    FOREIGN KEY (uid) REFERENCES db_account(id) ON DELETE CASCADE,
    FOREIGN KEY (tid) REFERENCES db_topic(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------
-- 7. 帖子互动 - 点赞表 (db_topic_interact_like)
-- 对应 TopicMapper: db_topic_interact_${type} where type='like'
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS db_topic_interact_like (
    tid  INT      NOT NULL,
    uid  INT      NOT NULL,
    time DATETIME NOT NULL DEFAULT NOW(),
    PRIMARY KEY (tid, uid),
    FOREIGN KEY (tid) REFERENCES db_topic(id) ON DELETE CASCADE,
    FOREIGN KEY (uid) REFERENCES db_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------
-- 8. 帖子互动 - 收藏表 (db_topic_interact_collect)
-- 对应 TopicMapper: db_topic_interact_${type} where type='collect'
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS db_topic_interact_collect (
    tid  INT      NOT NULL,
    uid  INT      NOT NULL,
    time DATETIME NOT NULL DEFAULT NOW(),
    PRIMARY KEY (tid, uid),
    FOREIGN KEY (tid) REFERENCES db_topic(id) ON DELETE CASCADE,
    FOREIGN KEY (uid) REFERENCES db_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------
-- 9. 图片存储记录表 (db_image_store)
-- 对应实体: StoreImageDTO.java
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS db_image_store (
    uid  INT          NOT NULL,
    name VARCHAR(255) NOT NULL,
    time DATETIME     NOT NULL DEFAULT NOW(),
    INDEX idx_uid (uid),
    FOREIGN KEY (uid) REFERENCES db_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------
-- 10. 通知表 (db_notification)
-- 对应实体: NotificationDTO.java
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS db_notification (
    id      INT PRIMARY KEY AUTO_INCREMENT,
    uid     INT          NOT NULL,
    title   VARCHAR(255) NOT NULL,
    content TEXT         DEFAULT NULL,
    type    VARCHAR(50)  DEFAULT NULL,
    url     VARCHAR(255) DEFAULT NULL,
    time    DATETIME     NOT NULL DEFAULT NOW(),
    INDEX idx_uid (uid),
    FOREIGN KEY (uid) REFERENCES db_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 以下为毕设新增表
-- =====================================================

-- ---------------------------------------------------
-- 11. 资源共享表 (db_resource) 【新增】
-- 用于第二阶段 - 资源共享模块
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS db_resource (
    id             INT PRIMARY KEY AUTO_INCREMENT,
    title          VARCHAR(100) NOT NULL COMMENT '资源标题',
    category       VARCHAR(50)  DEFAULT NULL COMMENT '分类: 课件/论文/笔记/其他',
    file_url       VARCHAR(255) DEFAULT NULL COMMENT 'MinIO 对象存储路径',
    file_name      VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
    file_size      BIGINT       DEFAULT 0 COMMENT '文件大小(字节)',
    uploader_id    INT          NOT NULL COMMENT '上传者ID',
    download_count INT          DEFAULT 0 COMMENT '下载次数',
    description    VARCHAR(500) DEFAULT NULL COMMENT '资源描述',
    status         VARCHAR(20)  NOT NULL DEFAULT 'approved' COMMENT '审核状态: pending/approved/rejected',
    reject_reason  VARCHAR(255) DEFAULT NULL COMMENT '驳回原因',
    auditor_id     INT          DEFAULT NULL COMMENT '审核人ID',
    audit_time     DATETIME     DEFAULT NULL COMMENT '审核时间',
    create_time    DATETIME     NOT NULL DEFAULT NOW(),
    INDEX idx_uploader (uploader_id),
    INDEX idx_category (category),
    INDEX idx_resource_status (status),
    FOREIGN KEY (uploader_id) REFERENCES db_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------
-- 12. 用户行为记录表 (db_user_behavior) 【新增】
-- 用于第四阶段 - 协同过滤推荐
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS db_user_behavior (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    uid         INT         NOT NULL COMMENT '用户ID',
    tid         INT         NOT NULL COMMENT '帖子ID',
    type        VARCHAR(20) NOT NULL COMMENT '行为类型: view/like/collect/comment',
    score       INT         NOT NULL DEFAULT 1 COMMENT '行为权重分值',
    create_time DATETIME    NOT NULL DEFAULT NOW(),
    INDEX idx_uid (uid),
    INDEX idx_tid (tid),
    UNIQUE KEY uk_user_topic_type (uid, tid, type),
    FOREIGN KEY (uid) REFERENCES db_account(id) ON DELETE CASCADE,
    FOREIGN KEY (tid) REFERENCES db_topic(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 以下为方案二新增表
-- =====================================================

-- ---------------------------------------------------
-- 13. 失物招领表 (db_lost_found)
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS db_lost_found (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    uid         INT          NOT NULL COMMENT '发布人ID',
    title       VARCHAR(100) NOT NULL,
    content     TEXT         NOT NULL,
    type        VARCHAR(10)  NOT NULL COMMENT 'lost=丢失 / found=捡到',
    contact     VARCHAR(100) DEFAULT NULL,
    images      VARCHAR(500) DEFAULT NULL,
    status      VARCHAR(10)  NOT NULL DEFAULT 'open' COMMENT 'open/closed',
    create_time DATETIME     NOT NULL DEFAULT NOW(),
    INDEX idx_uid (uid),
    FOREIGN KEY (uid) REFERENCES db_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------
-- 14. 校园活动表 (db_activity)
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS db_activity (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    title           VARCHAR(100) NOT NULL,
    content         TEXT         NOT NULL,
    location        VARCHAR(100) DEFAULT NULL,
    start_time      DATETIME     NOT NULL,
    end_time        DATETIME     NOT NULL,
    organizer       VARCHAR(100) DEFAULT NULL,
    max_people      INT          DEFAULT 0,
    current_people  INT          DEFAULT 0,
    image           VARCHAR(255) DEFAULT NULL,
    create_time     DATETIME     NOT NULL DEFAULT NOW()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO db_activity (title, content, location, start_time, end_time, organizer, max_people) VALUES
('2026年春季运动会', '一年一度的校园春季运动会，欢迎报名参加各项体育赛事！', '校田径场', '2026-04-15 08:00:00', '2026-04-16 17:00:00', '体育部', 500),
('编程马拉松 Hackathon', '48小时极限编程挑战赛，组队参加赢取丰厚奖品。', '计算机学院B栋', '2026-03-20 09:00:00', '2026-03-22 09:00:00', '计算机学院', 100),
('校园歌手大赛', '展示你的歌喉！海选→复赛→决赛，等你来唱。', '大学生活动中心', '2026-05-01 19:00:00', '2026-05-01 22:00:00', '校团委', 50);

-- ---------------------------------------------------
-- 15. 表白墙表 (db_confession)
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS db_confession (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    uid         INT     NOT NULL COMMENT '发布人ID',
    content     TEXT    NOT NULL,
    anonymous   TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1=匿名 0=实名',
    likes       INT     NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT NOW(),
    INDEX idx_uid (uid),
    FOREIGN KEY (uid) REFERENCES db_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------
-- 16. 成绩表 (db_grade)
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS db_grade (
    id           INT PRIMARY KEY AUTO_INCREMENT,
    student_id   VARCHAR(20)  NOT NULL COMMENT '学号',
    course_name  VARCHAR(100) NOT NULL,
    score        DECIMAL(5,1) NOT NULL,
    semester     VARCHAR(20)  NOT NULL COMMENT '如 2025-2026-1',
    create_time  DATETIME     NOT NULL DEFAULT NOW(),
    INDEX idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO db_grade (student_id, course_name, score, semester) VALUES
('2022001', '高等数学', 92.0, '2025-2026-1'),
('2022001', '大学英语', 85.5, '2025-2026-1'),
('2022001', '数据结构', 88.0, '2025-2026-1'),
('2022001', '操作系统', 91.0, '2025-2026-1'),
('2022001', '计算机网络', 78.5, '2025-2026-1'),
('2022002', 'Java程序设计', 95.0, '2025-2026-1'),
('2022002', '线性代数', 82.0, '2025-2026-1'),
('2022002', '数据库原理', 89.0, '2025-2026-1');

-- ---------------------------------------------------
-- 17. 教务通知表 (db_notice)
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS db_notice (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(200) NOT NULL,
    content     TEXT         NOT NULL,
    publisher   VARCHAR(50)  DEFAULT '教务处',
    is_top      TINYINT(1)   NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT NOW()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO db_notice (title, content, publisher, is_top) VALUES
('关于2025-2026学年第二学期选课的通知', '各位同学请注意，本学期选课时间为3月1日至3月5日，请登录教务系统完成选课操作。逾期不再补选。', '教务处', 1),
('2026年春季学期期末考试安排', '期末考试将于6月20日开始，请各位同学提前做好复习准备。具体考试安排将在考前两周公布。', '教务处', 0),
('关于开展大学生创新创业训练计划的通知', '为培养创新型人才，现启动2026年度大学生创新创业训练计划项目申报工作，详见附件。', '创新创业学院', 0),
('图书馆开放时间调整通知', '自3月1日起，图书馆开放时间调整为每日8:00-22:00，周末及节假日9:00-21:00。', '图书馆', 0);

-- ---------------------------------------------------
-- 18. 图书表 (db_book)
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS db_book (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(200) NOT NULL,
    author      VARCHAR(100) NOT NULL,
    isbn        VARCHAR(20)  DEFAULT NULL,
    category    VARCHAR(50)  DEFAULT NULL,
    description TEXT         DEFAULT NULL,
    cover_url   VARCHAR(255) DEFAULT NULL,
    location    VARCHAR(50)  DEFAULT NULL COMMENT '馆藏位置',
    available   TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1=可借 0=已借出',
    create_time DATETIME     NOT NULL DEFAULT NOW()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO db_book (title, author, isbn, category, description, location, available) VALUES
('深入理解计算机系统', 'Randal E. Bryant', '978-7-111-54493-7', '计算机科学', '从程序员视角全面剖析计算机系统', '三楼A区-12架', 1),
('算法导论', 'Thomas H. Cormen', '978-7-111-40701-0', '计算机科学', '算法领域的经典教材', '三楼A区-15架', 0),
('Java编程思想', 'Bruce Eckel', '978-7-111-21382-6', '编程语言', 'Java语言的经典入门与进阶读物', '三楼B区-03架', 1),
('数据库系统概念', 'Abraham Silberschatz', '978-7-111-58415-5', '计算机科学', '数据库领域权威教材', '三楼A区-18架', 1),
('Spring实战', 'Craig Walls', '978-7-115-47293-7', '编程语言', 'Spring框架开发实战指南', '三楼B区-07架', 1),
('三体', '刘慈欣', '978-7-229-03093-3', '文学小说', '中国科幻文学里程碑式的作品', '二楼C区-22架', 0),
('活着', '余华', '978-7-5063-3843-4', '文学小说', '一部充满力量的经典文学作品', '二楼C区-25架', 1),
('高等数学（第七版）', '同济大学数学系', '978-7-04-039663-8', '教材', '工科高等数学经典教材', '一楼教材区-01架', 1);

-- ---------------------------------------------------
-- 19. 敏感词库表 (db_sensitive_word) 【新增】
-- 用于发帖/评论敏感词精确匹配过滤
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS db_sensitive_word (
    id   INT PRIMARY KEY AUTO_INCREMENT,
    word VARCHAR(50) NOT NULL UNIQUE COMMENT '敏感词（唯一）',
    type VARCHAR(20) NOT NULL DEFAULT 'sensitive' COMMENT '词类型: sensitive/forbidden',
    INDEX idx_word (word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 预置初始敏感词（可在管理后台添加/删除）
INSERT INTO db_sensitive_word (word, type) VALUES ('垃圾', 'sensitive'), ('广告', 'sensitive'), ('骗子', 'sensitive');

-- ---------------------------------------------------
-- 20. 资源收藏表 (db_resource_collect) 【新增】
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS db_resource_collect (
    rid  INT      NOT NULL COMMENT '资源ID',
    uid  INT      NOT NULL COMMENT '用户ID',
    time DATETIME NOT NULL DEFAULT NOW(),
    PRIMARY KEY (rid, uid),
    FOREIGN KEY (rid) REFERENCES db_resource(id) ON DELETE CASCADE,
    FOREIGN KEY (uid) REFERENCES db_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------
-- 21. 用户反馈表 (db_feedback) 【新增】
-- ---------------------------------------------------
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

-- ---------------------------------------------------
-- 22. 置顶字段补丁
-- ---------------------------------------------------
DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER //
CREATE PROCEDURE add_column_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_column_if_missing('db_topic', 'top', 'TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否置顶 1=置顶 0=普通''');

-- ---------------------------------------------------
-- 23. 用户封禁字段补丁
-- ---------------------------------------------------
CALL add_column_if_missing('db_account', 'banned', 'TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否封禁 1=封禁 0=正常''');

-- ---------------------------------------------------
-- 24. 帖子审核与精华字段补丁
-- ---------------------------------------------------
CALL add_column_if_missing('db_topic', 'status', 'VARCHAR(20) NOT NULL DEFAULT ''approved'' COMMENT ''审核状态: pending/approved/rejected''');
CALL add_column_if_missing('db_topic', 'featured', 'TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否精华 1=精华 0=普通''');

-- ---------------------------------------------------
-- 25. 资源审核字段补丁
-- ---------------------------------------------------
CALL add_column_if_missing('db_resource', 'status', 'VARCHAR(20) NOT NULL DEFAULT ''approved'' COMMENT ''审核状态: pending/approved/rejected''');
CALL add_column_if_missing('db_resource', 'reject_reason', 'VARCHAR(255) DEFAULT NULL COMMENT ''驳回原因''');
CALL add_column_if_missing('db_resource', 'auditor_id', 'INT DEFAULT NULL COMMENT ''审核人ID''');
CALL add_column_if_missing('db_resource', 'audit_time', 'DATETIME DEFAULT NULL COMMENT ''审核时间''');

-- ---------------------------------------------------
-- 26. 版主版块字段补丁
-- ---------------------------------------------------
CALL add_column_if_missing('db_account', 'moderator_type', 'INT DEFAULT NULL COMMENT ''版主负责的版块ID''');

DROP PROCEDURE IF EXISTS add_column_if_missing;

-- ---------------------------------------------------
-- 27. 校历日程表 (db_schedule) 【新增】
-- 管理学期安排、假期、考试等校历事件
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS db_schedule (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(100) NOT NULL COMMENT '日程标题',
    description VARCHAR(500) DEFAULT NULL COMMENT '日程描述',
    event_date  DATE         NOT NULL COMMENT '开始日期',
    end_date    DATE         DEFAULT NULL COMMENT '结束日期（可选）',
    type        VARCHAR(20)  NOT NULL DEFAULT 'event' COMMENT 'semester/exam/holiday/event',
    create_time DATETIME     NOT NULL DEFAULT NOW()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO db_schedule (title, event_date, end_date, type, description) VALUES
('2025-2026学年第二学期开学', '2026-02-24', NULL, 'semester', '新学期正式上课'),
('清明节放假', '2026-04-04', '2026-04-06', 'holiday', '清明节法定假日'),
('期中考试周', '2026-04-20', '2026-04-26', 'exam', '2025-2026学年第二学期期中考试'),
('五一劳动节放假', '2026-05-01', '2026-05-05', 'holiday', '劳动节法定假日'),
('期末考试周', '2026-06-20', '2026-06-30', 'exam', '2025-2026学年第二学期期末考试'),
('暑假开始', '2026-07-05', '2026-08-30', 'holiday', '暑假');

