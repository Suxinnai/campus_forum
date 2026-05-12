package org.example.finishedbackend.service.Impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.example.finishedbackend.entity.DTO.*;
import org.example.finishedbackend.entity.VO.request.AddCommentVO;
import org.example.finishedbackend.entity.VO.request.TopicCreateVO;
import org.example.finishedbackend.entity.VO.request.TopicUpdateVO;
import org.example.finishedbackend.entity.VO.response.CommentVO;
import org.example.finishedbackend.entity.VO.response.TopicDetailVO;
import org.example.finishedbackend.entity.VO.response.TopicPreviewVO;
import org.example.finishedbackend.entity.VO.response.TopicTopVO;
import org.example.finishedbackend.mapper.*;
import org.example.finishedbackend.service.NotificationService;
import org.example.finishedbackend.service.TopicService;
import org.example.finishedbackend.utils.CacheUtils;
import org.example.finishedbackend.utils.Const;
import org.example.finishedbackend.utils.FlowUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class TopicServiceImpl extends ServiceImpl<TopicMapper, TopicDTO> implements TopicService {

    @Resource
    TopicTypeMapper typeMapper;

    @Resource
    FlowUtils flowUtils;

    @Resource
    CacheUtils cacheUtils;

    @Resource
    AccountMapper accountMapper;

    @Resource
    AccountPrivacyMapper privacyMapper;

    @Resource
    TopicCommentMapper commentMapper;

    @Resource
    NotificationService notificationService;

    @Resource
    StringRedisTemplate template;

    @Resource
    SensitiveWordMapper sensitiveWordMapper;

    // 预处理listTypes的Id;
    private Set<Integer> types = null;
    @Autowired
    private AccountDetailsMapper accountDetailsMapper;

    @PostConstruct
    private void init() {
        types = this.listTypes().stream().map(TopicTypeDTO::getId).collect(Collectors.toSet());
    }

    @Override
    public List<TopicTypeDTO> listTypes() {
        return typeMapper.selectList(null);
    }

    @Override
    public String createTopic(int uid, TopicCreateVO vo) {
        if (vo.getContent() == null)
            return "文章内容不能为空";
        if (!textLimitCheck(vo.getContent(), 20000))
            return "文章字数过多, 请稍后再试";
        if (vo.getType() == 0) {
            // tag系统替代分类后，type字段仅作兼容，默认取第一个有效分类
            List<TopicTypeDTO> typeList = this.listTypes();
            if (!typeList.isEmpty()) {
                vo.setType(typeList.get(0).getId());
            }
        } else if (!types.contains(vo.getType()))
            return "文章类型非法！";
        if (!flowUtils.limitPeriodCounterCheck(Const.FORUM_TOPIC_CREATE_COUNTER + uid, 20, 3600))
            return "发文频繁, 请稍后再试";
        String sensitiveHit = sensitiveCheck(vo.getTitle() + " " + vo.getContent().toJSONString());
        if (sensitiveHit != null)
            return "内容包含违禁词汇【" + sensitiveHit + "】，请修改后再发布";
        TopicDTO dto = new TopicDTO(0, vo.getTitle(), vo.getContent().toJSONString(), vo.getType(), new Date(), uid, 0,
                "approved", 0);
        if (this.save(dto)) {
            cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
            return null;
        } else {
            return "内部错误, 请联系管理员";
        }
    }

    @Override
    public String updateTopic(int uid, TopicUpdateVO vo) {
        if (vo.getContent() == null)
            return "文章内容不能为空";
        if (!textLimitCheck(vo.getContent(), 20000))
            return "文章字数过多, 请稍后再试";
        if (vo.getType() != 0 && !types.contains(vo.getType()))
            return "文章类型非法！";
        baseMapper.update(null, Wrappers.<TopicDTO>update()
                .eq("uid", uid)
                .eq("id", vo.getId())
                .set("title", vo.getTitle())
                .set("content", vo.getContent().toString())
                .set("type", vo.getType()));
        cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
        return null;
    }

    @Override
    public String createComment(int uid, AddCommentVO vo) {
        if (!flowUtils.limitPeriodCounterCheck(Const.FORUM_TOPIC_COMMENT_COUNTER + uid, 2, 60))
            return "发表评论频繁, 请稍后再试";
        if (!textLimitCheck(JSONObject.parseObject(vo.getContent()), 2000))
            return "评论内容字数过多, 请重新编辑";
        String sensitiveHit = sensitiveCheck(vo.getContent());
        if (sensitiveHit != null)
            return "评论包含违禁词汇【" + sensitiveHit + "】，请修改后再提交";
        TopicCommentDTO dto = new TopicCommentDTO();
        dto.setUid(uid);
        BeanUtils.copyProperties(vo, dto);
        dto.setTime(new Date());
        commentMapper.insert(dto);
        cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
        TopicDTO topicDTO = baseMapper.selectById(vo.getTid());
        AccountDTO accountDTO = accountMapper.selectById(uid);
        if (vo.getQuote() > 0) {
            TopicCommentDTO commentDTO = commentMapper.selectById(vo.getQuote());
            if (!Objects.equals(accountDTO.getId(), commentDTO.getUid())) {
                notificationService.addNotification(commentDTO.getUid(), "您有新的评论回复",
                        accountDTO.getUsername() + "回复了你发表的评论", "success",
                        "/home/topic/" + commentDTO.getTid());
            }
        } else if (!Objects.equals(accountDTO.getId(), topicDTO.getUid())) {
            notificationService.addNotification(topicDTO.getUid(), "您有新的帖子评论回复",
                    accountDTO.getUsername() + "回复了你发表的主题:" + topicDTO.getTitle(), "success",
                    "/home/topic/" + topicDTO.getId());
        }
        return null;
    }

    /**
     * 精确匹配敏感词检测（大小写不敏感）
     */
    private String sensitiveCheck(String text) {
        if (text == null || text.isBlank()) return null;
        List<SensitiveWordDTO> words = safeLoadSensitiveWords();
        String lowerText = text.toLowerCase();
        for (SensitiveWordDTO sw : words) {
            if (sw.getWord() != null && lowerText.contains(sw.getWord().toLowerCase())) {
                return sw.getWord();
            }
        }
        return null;
    }

    private List<SensitiveWordDTO> safeLoadSensitiveWords() {
        try {
            return sensitiveWordMapper.selectList(null);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<CommentVO> comments(int tid, int page) {
        Page<TopicCommentDTO> comments = Page.of(page + 1, 10);
        commentMapper.selectPage(comments, Wrappers.<TopicCommentDTO>query().eq("tid", tid));
        return comments.getRecords().stream().map(dto -> {
            CommentVO vo = new CommentVO();
            BeanUtils.copyProperties(dto, vo);
            if (dto.getQuote() != null && dto.getQuote() > 0) {
                TopicCommentDTO commentDTO = commentMapper
                        .selectOne(Wrappers.<TopicCommentDTO>query().eq("id", dto.getQuote()).orderByAsc("time"));
                if (commentDTO != null) {
                    try {
                        JSONObject object = JSONObject.parseObject(commentDTO.getContent());
                        StringBuilder builder = new StringBuilder();
                        this.shortContent(object.getJSONArray("ops"), builder, ignore -> { });
                        vo.setQuote(builder.toString());
                    } catch (Exception e) {
                        vo.setQuote("评论格式解析失败");
                    }
                } else {
                    vo.setQuote("此评论已被删除");
                }
            }
            CommentVO.User user = new CommentVO.User();
            vo.setUser(this.fillUserDetailsByPrivacy(user, dto.getUid()));
            return vo;
        }).toList();
    }

    @Override
    public void deleteComment(int uid, int id) {
        commentMapper.delete(Wrappers.<TopicCommentDTO>query().eq("id", id).eq("uid", uid));
        cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
    }

    @Override
    public String deleteTopic(int uid, int id) {
        TopicDTO dto = baseMapper.selectById(id);
        if (dto == null) return "帖子不存在";
        AccountDTO operator = accountMapper.selectById(uid);
        boolean isAdmin = operator != null && ("admin".equals(operator.getRole()) || "moderator".equals(operator.getRole()));
        if (dto.getUid() != uid && !isAdmin) return "只能删除自己发布的帖子";
        // 删除该帖子的所有评论
        commentMapper.delete(Wrappers.<TopicCommentDTO>query().eq("tid", id));
        // 删除帖子本身
        baseMapper.deleteById(id);
        // 清除预览缓存
        cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
        return null;
    }

    @Override
    public List<TopicPreviewVO> listTopicByPage(int pageNumber, int type) {
        String key = Const.FORUM_TOPIC_PREVIEW_CACHE + pageNumber + ":" + type;
        List<TopicPreviewVO> voList = cacheUtils.getListFromCache(key, TopicPreviewVO.class);
        if (voList != null)
            return voList;
        Page<TopicDTO> page = Page.of(pageNumber + 1, 10);
        if (type == -1) {
            baseMapper.selectPage(page, Wrappers.<TopicDTO>query().orderByDesc("time"));
        } else if (type == -2 || type == 0) {
            baseMapper.selectPage(page, Wrappers.<TopicDTO>query().orderByDesc("time"));
        } else {
            baseMapper.selectPage(page, Wrappers.<TopicDTO>query().eq("type", type).orderByDesc("time"));
        }
        List<TopicDTO> list = page.getRecords();
        if (list.isEmpty())
            return null;
        voList = list.stream().map(this::resolveToPreview).collect(Collectors.toList());
        if (type == -1) {
            voList.sort((a, b) -> b.getLike() - a.getLike());
        }
        // 第一页且全部/最新tab时，将置顶帖排在最前面
        if (pageNumber == 0 && (type == 0 || type == -2)) {
            List<TopicDTO> pinnedDtos = baseMapper.selectList(Wrappers.<TopicDTO>query()
                    .eq("top", 1).orderByDesc("time"));
            if (pinnedDtos != null && !pinnedDtos.isEmpty()) {
                Set<Integer> pinnedIds = pinnedDtos.stream().map(TopicDTO::getId).collect(Collectors.toSet());
                voList.removeIf(v -> pinnedIds.contains(v.getId()));
                List<TopicPreviewVO> pinnedVos = pinnedDtos.stream()
                        .map(this::resolveToPreview).collect(Collectors.toList());
                pinnedVos.addAll(voList);
                voList = pinnedVos;
            }
        }
        cacheUtils.saveListToCache(key, voList, 60);
        return voList;
    }

    @Override
    public List<TopicPreviewVO> listTopicCollects(int uid) {
        return baseMapper.collectTopics(uid).stream().map(topic -> {
            TopicPreviewVO vo = new TopicPreviewVO();
            BeanUtils.copyProperties(topic, vo);
            return vo;
        }).toList();
    }

    @Override
    public List<TopicTopVO> listTopTopics() {
        // 只取当天发布的帖子，按点赞数降序取前5作为今日热门
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date todayStart = cal.getTime();

        List<TopicDTO> topics = baseMapper.selectList(Wrappers.<TopicDTO>query()
                .select("id", "title", "time")
                .ge("time", todayStart)
                .orderByDesc("time"));
        // 如果当天帖子不足5条，补充最近的帖子
        if (topics.size() < 5) {
            Set<Integer> existIds = topics.stream().map(TopicDTO::getId).collect(Collectors.toSet());
            List<TopicDTO> recent = baseMapper.selectList(Wrappers.<TopicDTO>query()
                    .select("id", "title", "time")
                    .lt("time", todayStart)
                    .orderByDesc("time")
                    .last("LIMIT " + (5 - topics.size())));
            recent.stream().filter(t -> !existIds.contains(t.getId())).forEach(topics::add);
        }
        return topics.stream()
                .sorted((a, b) -> baseMapper.interactCount(b.getId(), "like")
                        - baseMapper.interactCount(a.getId(), "like"))
                .limit(5)
                .map(topic -> new TopicTopVO(topic.getId(), topic.getTitle(), topic.getTime()))
                .toList();
    }

    @Override
    public TopicDetailVO getTopic(int tid, int uid) {
        TopicDetailVO vo = new TopicDetailVO();
        TopicDTO dto = baseMapper.selectById(tid);
        if (dto == null) return null;
        BeanUtils.copyProperties(dto, vo);
        try {
            JSONObject content = JSONObject.parseObject(dto.getContent());
            vo.setTags(content.getList("tags", String.class));
            // 直接返回原始内容 JSON 字符串，让前端负责富文本/图片的渲染
            vo.setContent(dto.getContent());
        } catch (Exception e) {
            vo.setContent(dto.getContent());
        }
        TopicDetailVO.User user = new TopicDetailVO.User();
        TopicDetailVO.Interact interact = new TopicDetailVO.Interact(
                hasInteract(tid, uid, "like"),
                hasInteract(tid, uid, "collect"));
        vo.setInteract(interact);
        vo.setUser(this.fillUserDetailsByPrivacy(user, dto.getUid()));
        vo.setComments(commentMapper.selectCount(Wrappers.<TopicCommentDTO>query().eq("tid", tid)));
        vo.setLike(interactCountWithPending(dto.getId(), "like"));
        vo.setCollect(interactCountWithPending(dto.getId(), "collect"));
        return vo;
    }

    private boolean hasInteract(int tid, int uid, String type) {
        try {
            String key = tid + ":" + uid;
            if (template.opsForHash().hasKey(type, key))
                return Boolean.parseBoolean(template.opsForHash().entries(type).get(key).toString());
        } catch (Exception e) {
            // Redis 连接失败，降级到数据库查询
            // e.printStackTrace();
        }
        return baseMapper.userInteractCount(tid, uid, type) > 0;
    }

    @Override
    public void interact(Interact interact, boolean state) {
        String type = interact.getType();
        synchronized (type.intern()) {
            try {
                template.opsForHash().put(type, interact.toKey(), Boolean.toString(state));
                cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
                this.saveInteractSchedule(type);
            } catch (Exception e) {
                // Redis 失败，直接同步到数据库（可选，或者直接报错）
                // 这里为了稳健，如果交互失败，我们可以抛出异常让用户重试
                // 但为了不让页面崩溃，我们暂不处理，等待下次同步
            }
        }
    }

    private final Map<String, Boolean> map = new HashMap<>();
    ScheduledExecutorService service = Executors.newScheduledThreadPool(2);

    private void saveInteractSchedule(String type) {
        if (!map.getOrDefault(type, false)) {
            map.put(type, true);
            service.schedule(() -> {
                this.saveInteract(type);
                map.put(type, false);
            }, 3, TimeUnit.SECONDS);
        }
    }

    private void saveInteract(String type) {
        synchronized (type.intern()) {
            List<Interact> check = new LinkedList<>();
            List<Interact> uncheck = new LinkedList<>();
            template.opsForHash().entries(type).forEach((k, v) -> {
                if (Boolean.parseBoolean(v.toString()))
                    check.add(Interact.parseInteract(k.toString(), type));
                else
                    uncheck.add(Interact.parseInteract(k.toString(), type));
            });
            if (!check.isEmpty())
                baseMapper.addInteract(check, type);
            if (!uncheck.isEmpty())
                baseMapper.deleteInteract(uncheck, type);
            template.delete(type);
        }
    }

    private int interactCountWithPending(int tid, String type) {
        int count = baseMapper.interactCount(tid, type);
        try {
            Map<Object, Object> entries = template.opsForHash().entries(type);
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String[] parts = key.split(":");
                if (parts.length != 2 || Integer.parseInt(parts[0]) != tid) {
                    continue;
                }
                int uid = Integer.parseInt(parts[1]);
                boolean pendingState = Boolean.parseBoolean(String.valueOf(entry.getValue()));
                boolean dbState = baseMapper.userInteractCount(tid, uid, type) > 0;
                if (pendingState && !dbState) {
                    count++;
                } else if (!pendingState && dbState) {
                    count--;
                }
            }
        } catch (Exception ignored) {
        }
        return Math.max(count, 0);
    }

    private <T> T fillUserDetailsByPrivacy(T target, int uid) {
        AccountDetailsDTO details = accountDetailsMapper.selectById(uid);
        AccountDTO account = accountMapper.selectById(uid);
        AccountPrivacyDTO privacy = privacyMapper.selectById(uid);
        String[] ignores = (privacy == null) ? new String[0] : privacy.hiddenFields();
        if (account != null) BeanUtils.copyProperties(account, target, ignores);
        if (details != null) BeanUtils.copyProperties(details, target, ignores);
        return target;
    }

    @Override
    public List<TopicPreviewVO> searchTopics(String keyword, int page) {
        if (keyword == null || keyword.isBlank()) return List.of();
        Page<TopicDTO> p = Page.of(page + 1, 10);
        baseMapper.selectPage(p, Wrappers.<TopicDTO>query()
                .like("title", keyword)
                .or()
                .like("content", keyword)
                .orderByDesc("time"));
        if (p.getRecords().isEmpty()) return List.of();
        return p.getRecords().stream().map(this::resolveToPreview).collect(Collectors.toList());
    }

    @Override
    public TopicPreviewVO resolvePreviewById(int tid) {
        TopicDTO dto = this.getById(tid);
        if (dto == null) return null;
        return resolveToPreview(dto);
    }

    private TopicPreviewVO resolveToPreview(TopicDTO dto) {
        AccountDTO account = accountMapper.selectById(dto.getUid());
        TopicPreviewVO vo = new TopicPreviewVO(dto.getId(), dto.getType(), dto.getTitle(), null, null, dto.getTime(),
                dto.getUid(), account != null ? account.getUsername() : "已注销用户", account != null ? account.getAvatar() : null,
                interactCountWithPending(dto.getId(), "like"),
                interactCountWithPending(dto.getId(), "collect"),
                commentMapper.selectCount(Wrappers.<TopicCommentDTO>query().eq("tid", dto.getId())).intValue(),
                null, dto.getTop());
        List<String> images = new LinkedList<>();
        StringBuilder previewText = new StringBuilder();
        try {
            JSONObject content = JSONObject.parseObject(dto.getContent());
            vo.setTags(content.getList("tags", String.class));
            JSONArray ops = content.getJSONArray("ops");
            this.shortContent(ops, previewText, obj -> images.add(obj.toString()));
        } catch (Exception e) {}
        vo.setText(previewText.length() > 300 ? previewText.substring(0, 300) : previewText.toString());
        vo.setImages(images);
        vo.setTop(dto.getTop());
        return vo;
    }

    private void shortContent(JSONArray ops, StringBuilder previewText, Consumer<Object> imagesHandler) {
        for (Object op : ops) {
            Object insert = JSONObject.from(op).get("insert");
            if (insert instanceof String text) {
                if (previewText.length() >= 300)
                    continue;
                previewText.append(text);
            } else if (insert instanceof Map<?, ?> map) {
                Optional.ofNullable(map.get("image")).ifPresent(imagesHandler);
            }
        }
    }

    private boolean textLimitCheck(JSONObject object, int max) {
        if (object == null || object.getJSONArray("ops") == null) return true;
        long length = 0;
        for (Object ops : object.getJSONArray("ops")) {
            String insertStr = JSONObject.from(ops).getString("insert");
            if (insertStr != null) {
                length += insertStr.length();
            }
            if (length > max)
                return false;
        }
        return true;
    }

}
