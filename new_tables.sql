USE campus_forum;

CREATE TABLE IF NOT EXISTS db_lost_found (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    uid         INT          NOT NULL COMMENT '发布人ID',
    title       VARCHAR(100) NOT NULL,
    content     TEXT         NOT NULL,
    type        VARCHAR(10)  NOT NULL COMMENT 'lost / found',
    contact     VARCHAR(100) DEFAULT NULL,
    images      VARCHAR(500) DEFAULT NULL,
    status      VARCHAR(10)  NOT NULL DEFAULT 'open',
    create_time DATETIME     NOT NULL DEFAULT NOW(),
    INDEX idx_uid (uid),
    FOREIGN KEY (uid) REFERENCES db_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

INSERT IGNORE INTO db_activity (id, title, content, location, start_time, end_time, organizer, max_people) VALUES
(1, '2026年春季运动会', '一年一度的校园春季运动会，欢迎报名参加各项体育赛事！', '校田径场', '2026-04-15 08:00:00', '2026-04-16 17:00:00', '体育部', 500),
(2, '编程马拉松 Hackathon', '48小时极限编程挑战赛，组队参加赢取丰厚奖品。', '计算机学院B栋', '2026-03-20 09:00:00', '2026-03-22 09:00:00', '计算机学院', 100),
(3, '校园歌手大赛', '展示你的歌喉！海选到决赛，等你来唱。', '大学生活动中心', '2026-05-01 19:00:00', '2026-05-01 22:00:00', '校团委', 50);

CREATE TABLE IF NOT EXISTS db_confession (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    uid         INT     NOT NULL COMMENT '发布人ID',
    content     TEXT    NOT NULL,
    anonymous   TINYINT(1) NOT NULL DEFAULT 1,
    likes       INT     NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT NOW(),
    INDEX idx_uid (uid),
    FOREIGN KEY (uid) REFERENCES db_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS db_grade (
    id           INT PRIMARY KEY AUTO_INCREMENT,
    student_id   VARCHAR(20)  NOT NULL,
    course_name  VARCHAR(100) NOT NULL,
    score        DECIMAL(5,1) NOT NULL,
    semester     VARCHAR(20)  NOT NULL,
    create_time  DATETIME     NOT NULL DEFAULT NOW(),
    INDEX idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO db_grade (id, student_id, course_name, score, semester) VALUES
(1, '2022001', '高等数学', 92.0, '2025-2026-1'),
(2, '2022001', '大学英语', 85.5, '2025-2026-1'),
(3, '2022001', '数据结构', 88.0, '2025-2026-1'),
(4, '2022001', '操作系统', 91.0, '2025-2026-1'),
(5, '2022001', '计算机网络', 78.5, '2025-2026-1'),
(6, '2022002', 'Java程序设计', 95.0, '2025-2026-1'),
(7, '2022002', '线性代数', 82.0, '2025-2026-1'),
(8, '2022002', '数据库原理', 89.0, '2025-2026-1');

CREATE TABLE IF NOT EXISTS db_notice (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(200) NOT NULL,
    content     TEXT         NOT NULL,
    publisher   VARCHAR(50)  DEFAULT '教务处',
    is_top      TINYINT(1)   NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT NOW()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO db_notice (id, title, content, publisher, is_top) VALUES
(1, '关于2025-2026学年第二学期选课的通知', '各位同学请注意，本学期选课时间为3月1日至3月5日，请登录教务系统完成选课操作。逾期不再补选。', '教务处', 1),
(2, '2026年春季学期期末考试安排', '期末考试将于6月20日开始，请各位同学提前做好复习准备。具体考试安排将在考前两周公布。', '教务处', 0),
(3, '关于开展大学生创新创业训练计划的通知', '为培养创新型人才，现启动2026年度大学生创新创业训练计划项目申报工作，详见附件。', '创新创业学院', 0),
(4, '图书馆开放时间调整通知', '自3月1日起，图书馆开放时间调整为每日8:00-22:00，周末及节假日9:00-21:00。', '图书馆', 0);

CREATE TABLE IF NOT EXISTS db_book (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(200) NOT NULL,
    author      VARCHAR(100) NOT NULL,
    isbn        VARCHAR(20)  DEFAULT NULL,
    category    VARCHAR(50)  DEFAULT NULL,
    description TEXT         DEFAULT NULL,
    cover_url   VARCHAR(255) DEFAULT NULL,
    location    VARCHAR(50)  DEFAULT NULL,
    available   TINYINT(1)   NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL DEFAULT NOW()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO db_book (id, title, author, isbn, category, description, location, available) VALUES
(1, '深入理解计算机系统', 'Randal E. Bryant', '978-7-111-54493-7', '计算机科学', '从程序员视角全面剖析计算机系统', '三楼A区-12架', 1),
(2, '算法导论', 'Thomas H. Cormen', '978-7-111-40701-0', '计算机科学', '算法领域的经典教材', '三楼A区-15架', 0),
(3, 'Java编程思想', 'Bruce Eckel', '978-7-111-21382-6', '编程语言', 'Java语言的经典入门与进阶读物', '三楼B区-03架', 1),
(4, '数据库系统概念', 'Abraham Silberschatz', '978-7-111-58415-5', '计算机科学', '数据库领域权威教材', '三楼A区-18架', 1),
(5, 'Spring实战', 'Craig Walls', '978-7-115-47293-7', '编程语言', 'Spring框架开发实战指南', '三楼B区-07架', 1),
(6, '三体', '刘慈欣', '978-7-229-03093-3', '文学小说', '中国科幻文学里程碑式的作品', '二楼C区-22架', 0),
(7, '活着', '余华', '978-7-5063-3843-4', '文学小说', '一部充满力量的经典文学作品', '二楼C区-25架', 1),
(8, '高等数学（第七版）', '同济大学数学系', '978-7-04-039663-8', '教材', '工科高等数学经典教材', '一楼教材区-01架', 1);

SELECT 'All tables created and data inserted!' AS result;
