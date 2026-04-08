-- =====================================================
-- 插入真实帖子示例数据
-- 依赖: db_account 中存在 id=1 (admin) 和 id=2 (test)
-- 帖子 content 字段为 Quill Delta JSON 格式
-- =====================================================

USE campus_forum;

-- 插入更多测试账号（昵称更真实）
INSERT IGNORE INTO db_account (id, username, password, email, role) VALUES
(3, '计科王同学', '$2a$10$2se33xOEcd.5z.5F9EB3h.mL1e4X2ey3AnUtIwdKbY/CeiEt/ou7G', 'wang@qingyanshe.com', 'user'),
(4, '软工李萌', '$2a$10$2se33xOEcd.5z.5F9EB3h.mL1e4X2ey3AnUtIwdKbY/CeiEt/ou7G', 'li@qingyanshe.com', 'user'),
(5, '数学系张成', '$2a$10$2se33xOEcd.5z.5F9EB3h.mL1e4X2ey3AnUtIwdKbY/CeiEt/ou7G', 'zhang@qingyanshe.com', 'user');

INSERT IGNORE INTO db_account_details (id, gender, phone, qq, `desc`) VALUES
(3, 1, NULL, NULL, '计算机科学与技术大三在读'),
(4, 2, NULL, NULL, '软件工程大二，喜欢前端开发'),
(5, 1, NULL, NULL, '数学系研一，热爱算法竞赛');

INSERT IGNORE INTO db_account_privacy (id, phone, email, qq, gender) VALUES
(3, 1, 1, 1, 1),
(4, 1, 1, 1, 1),
(5, 1, 1, 1, 1);

-- =====================================================
-- 插入帖子（Quill Delta JSON 格式）
-- =====================================================

INSERT INTO db_topic (title, content, type, time, uid) VALUES

-- 1. 日常类帖子
('图书馆四楼自习室真的太难抢了！！',
 '{"ops":[{"insert":"每天早上六点半就要去排队占座，昨天八点去已经全满了😭\n"},{"insert":"特别是期末前两个月，简直是"},{"attributes":{"bold":true},"insert":"人间地狱"},{"insert":"，大家有没有什么好的占座技巧可以分享？\n\n另外求问：有没有人知道哪个教学楼的自习室比较好占？我现在一般去主楼三楼，但也开始变得很热门了。\n"}]}',
 1, DATE_SUB(NOW(), INTERVAL 2 HOUR), 2),

-- 2. 学习类帖子
('分享一下我备考期末的高效学习方法',
 '{"ops":[{"insert":"考完期末，总结一下这学期用到的学习方法，供大家参考：\n\n"},{"attributes":{"bold":true},"insert":"1. 番茄工作法"},{"insert":"\n25分钟专注 + 5分钟休息，效率翻倍。推荐用 Forest App 防止刷手机。\n\n"},{"attributes":{"bold":true},"insert":"2. 费曼学习法"},{"insert":"\n把知识点用自己的话讲给别人听（或者写下来），不懂的地方会立刻暴露出来。\n\n"},{"attributes":{"bold":true},"insert":"3. 艾宾浩斯复习曲线"},{"insert":"\n新知识学完后，1天/3天/7天/14天分别复习一次，记忆效果极佳。\n\n希望对正在备考的同学有帮助！有问题欢迎在评论区交流 📚\n"}]}',
 2, DATE_SUB(NOW(), INTERVAL 5 HOUR), 3),

-- 3. 活动类帖子
('【通知】4月15日春季运动会报名开始！',
 '{"ops":[{"attributes":{"bold":true},"insert":"2026年春季运动会"},{"insert":" 报名正式开始啦！🏃‍♂️\n\n"},{"attributes":{"bold":true},"insert":"活动时间："},{"insert":"4月15日（周三）全天\n"},{"attributes":{"bold":true},"insert":"活动地点："},{"insert":"校田径场\n"},{"attributes":{"bold":true},"insert":"报名方式："},{"insert":"在本帖评论区留下姓名+学号+参赛项目\n\n"},{"attributes":{"bold":true},"insert":"参赛项目包括："},{"insert":"\n• 100米、200米、400米短跑\n• 跳远、铅球\n• 4×100米接力（组队参加）\n• 趣味项目：三人四足、拔河\n\n欢迎踊跃报名，为院系争光！🥇\n"}]}',
 3, DATE_SUB(NOW(), INTERVAL 1 DAY), 1),

-- 4. 吐槽类帖子
('吐槽一下食堂二楼的排队问题',
 '{"ops":[{"insert":"每天中午12点，食堂二楼的黄焖鸡窗口队伍能排到门外去😤\n\n我掐着时间11:55到，已经排了20多个人了。等到拿到饭，12:25了，然后下午一点还有课……\n\n建议食堂能不能多开一个窗口？或者搞一个提前点餐的小程序？\n\n"},{"attributes":{"italic":true},"insert":"其实我就是想说：黄焖鸡米饭真的好好吃，所以大家才这么排队的哈哈哈。"},{"insert":"\n"}]}',
 4, DATE_SUB(NOW(), INTERVAL 3 HOUR), 4),

-- 5. 求助类帖子
('求助：Spring Boot整合Redis报错，有大佬帮看看吗',
 '{"ops":[{"insert":"我在做毕设的时候整合 Redis 遇到了问题，报错如下：\n\n"},{"attributes":{"code-block":true},"insert":"Cannot get Jedis connection; nested exception is redis.clients.jedis.exceptions.JedisConnectionException: Could not get a resource from the pool"},{"insert":"\n\n我的配置是：\n"},{"attributes":{"code-block":true},"insert":"spring:\n  redis:\n    host: localhost\n    port: 6379\n    timeout: 3000ms"},{"insert":"\n\n本地 Redis 已经启动了（redis-cli ping 返回 PONG），但 Spring Boot 就是连不上。\n\n有没有大佬遇到过同样的问题？在线等，急！🆘\n"}]}',
 5, DATE_SUB(NOW(), INTERVAL 6 HOUR), 5),

-- 6. 日常类帖子
('宿舍楼门口的樱花开了，真的好美🌸',
 '{"ops":[{"insert":"今天傍晚回宿舍，看到宿舍楼门口的樱花全开了！粉粉的，配上夕阳，整个画面美得像电影里的场景。\n\n赶紧拍了好多照片，分享给大家 🌸\n\n趁着花期还没过，推荐大家去打卡！最佳观赏时间应该是下午四五点，光线最好看。\n\n顺便问一下，有没有同学知道校园内还有哪些地方有樱花或者其他好看的花？\n"}]}',
 1, DATE_SUB(NOW(), INTERVAL 4 HOUR), 4),

-- 7. 学习类帖子
('leetcode刷题经验分享：从零到通过大厂笔试',
 '{"ops":[{"insert":"经过半年的刷题，终于拿到了几个互联网大厂的offer，在这里分享一下我的刷题路线：\n\n"},{"attributes":{"bold":true},"insert":"第一阶段（1-2个月）：打基础"},{"insert":"\n把数组、字符串、链表、栈队列的基础题刷完，每类至少30题。\n\n"},{"attributes":{"bold":true},"insert":"第二阶段（2-3个月）：专项突破"},{"insert":"\n重点攻克 二叉树、动态规划、图论，这三块是面试高频考点。\n\n"},{"attributes":{"bold":true},"insert":"第三阶段（最后冲刺）：模拟面试"},{"insert":"\n每天限时做2-3道题，模拟真实面试环境，培养手速和代码规范。\n\n总计刷了约"},{"attributes":{"bold":true},"insert":"450题"},{"insert":"，其中中等难度占60%，感兴趣的同学可以评论区交流！💪\n"}]}',
 2, DATE_SUB(NOW(), INTERVAL 2 DAY), 3),

-- 8. 活动类帖子
('下周五举办读书会：《人类简史》共读分享',
 '{"ops":[{"insert":"📖 "},{"attributes":{"bold":true},"insert":"青研社读书会"},{"insert":" 第12期活动通知\n\n本期共读书目：《人类简史》—— 尤瓦尔·赫拉利\n\n"},{"attributes":{"bold":true},"insert":"时间："},{"insert":"4月12日（周五）晚7:30\n"},{"attributes":{"bold":true},"insert":"地点："},{"insert":"图书馆三楼讨论室B\n"},{"attributes":{"bold":true},"insert":"人数限制："},{"insert":"20人以内（先到先得）\n\n"},{"attributes":{"bold":true},"insert":"讨论主题："},{"insert":"\n1. 认知革命如何让智人脱颖而出？\n2. 农业革命是人类历史上最大的骗局吗？\n3. 未来的人类将走向何方？\n\n不需要提前看完全书，欢迎带着问题来参与！🙌\n"}]}',
 3, DATE_SUB(NOW(), INTERVAL 18 HOUR), 2),

-- 9. 吐槽类帖子
('期末周的图书馆众生相',
 '{"ops":[{"insert":"期末周图书馆真的是"},{"attributes":{"bold":true},"insert":"魔幻现实主义"},{"insert":"大舞台——\n\n🎭 "},{"attributes":{"bold":true},"insert":"凌晨两点还在看书的学霸"},{"insert":"：人家已经在复习下周的内容了\n🛌 "},{"attributes":{"bold":true},"insert":"头碰桌睡着了还没合书的同学"},{"insert":"：精神可嘉，身体诚实\n🎧 "},{"attributes":{"bold":true},"insert":"戴着耳机看剧的同学"},{"insert":"：这图书馆是你的自留地吗？（但我理解）\n📱 "},{"attributes":{"bold":true},"insert":"手机刷了三小时抖音然后后悔的同学"},{"insert":"：就是我本人\n\n大家期末周都怎么熬过来的？欢迎分享你的魔幻故事 😂\n"}]}',
 4, DATE_SUB(NOW(), INTERVAL 12 HOUR), 5),

-- 10. 求助类帖子
('有没有人有大学物理笔记可以分享一下',
 '{"ops":[{"insert":"大二物理期末要到了，我这学期物理课听课状态不太好，笔记记得乱七八糟。\n\n想问一下有没有同学有比较完整的"},{"attributes":{"bold":true},"insert":"大学物理（上册）"},{"insert":"笔记愿意分享？\n\n重点是：\n• 质点运动学\n• 刚体转动\n• 静电场\n• 稳恒电流\n\n可以的话私信我，或者在评论区贴百度网盘链接也行！感激不尽🙏\n"}]}',
 5, DATE_SUB(NOW(), INTERVAL 8 HOUR), 4);

-- =====================================================
-- 添加点赞数据（让帖子更真实）
-- =====================================================

INSERT IGNORE INTO db_topic_interact_like (tid, uid, time) VALUES
(1, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(1, 3, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(1, 5, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(2, 1, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(2, 2, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(2, 4, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(2, 5, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(3, 2, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(3, 3, DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(3, 4, DATE_SUB(NOW(), INTERVAL 15 HOUR)),
(4, 1, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(4, 5, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(5, 1, DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(5, 3, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(6, 2, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(6, 3, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(7, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(7, 2, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(7, 4, DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(7, 5, DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(8, 1, DATE_SUB(NOW(), INTERVAL 15 HOUR)),
(8, 3, DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(9, 1, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(9, 2, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(9, 3, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(10, 1, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(10, 5, DATE_SUB(NOW(), INTERVAL 4 HOUR));

-- =====================================================
-- 添加评论数据
-- =====================================================

INSERT INTO db_topic_comment (uid, tid, content, time) VALUES
(3, 1, '同感！我每天七点去，发现已经有人把书包放那里占座了，到底几点开始占的😭', DATE_SUB(NOW(), INTERVAL 90 MINUTE)),
(1, 1, '建议去教学楼找自习室，我最近发现理工楼5楼有几个空教室，基本没人去', DATE_SUB(NOW(), INTERVAL 60 MINUTE)),
(4, 2, '太感谢了！特别是艾宾浩斯曲线这个方法，我一直听说但没认真用过，这次要试试', DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(5, 2, '番茄工作法确实有用，但我总是做着做着就忘记计时了哈哈', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(2, 3, '报名！李明 2022030142 100米+4×100接力', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(4, 3, '王小红 2022030158 趣味三人四足！', DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(1, 5, '你的application.yaml里面有没有配置连接池？试试加上 lettuce 或者 jedis 的连接池配置', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(3, 5, '检查一下Redis是不是只监听了127.0.0.1，有时候需要改成0.0.0.0', DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(1, 7, '大佬！请问动态规划这块有没有推荐的题单？感觉这块是我最薄弱的', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(3, 7, 'DP可以先从背包问题入手，labuladong的博客讲得很好，建议看看', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(2, 9, '哈哈哈第四条说的就是我自己！每次都下决心来图书馆好好学，结果刷了两小时视频', DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(4, 9, '还有一种：拿着厚厚的资料书，实际上在想别的事情发呆的人（也是我）', DATE_SUB(NOW(), INTERVAL 8 HOUR));

SELECT '帖子数据插入完成！' AS result;
SELECT COUNT(*) AS topic_count FROM db_topic;
SELECT COUNT(*) AS like_count FROM db_topic_interact_like;
SELECT COUNT(*) AS comment_count FROM db_topic_comment;
