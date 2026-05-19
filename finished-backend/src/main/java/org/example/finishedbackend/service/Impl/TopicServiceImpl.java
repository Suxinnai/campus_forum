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
import org.example.finishedbackend.entity.VO.response.TopicPageVO;
import org.example.finishedbackend.entity.VO.response.TopicPreviewVO;
import org.example.finishedbackend.entity.VO.response.TopicTopVO;
import org.example.finishedbackend.mapper.*;
import org.example.finishedbackend.service.NotificationService;
import org.example.finishedbackend.service.TopicService;
import org.example.finishedbackend.service.filter.ContentFilter;
import org.example.finishedbackend.service.filter.FilterResult;
import org.example.finishedbackend.utils.CacheUtils;
import org.example.finishedbackend.utils.Const;
import org.example.finishedbackend.utils.FlowUtils;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class TopicServiceImpl extends ServiceImpl<TopicMapper, TopicDTO> implements TopicService {

    private static final String STATUS_APPROVED = "approved";
    private static final String STATUS_PENDING = "pending";

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
    ContentFilter contentFilter;

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
        Integer type = resolveTopicType(vo.getType());
        if (type == null)
            return "文章类型非法！";
        if (!flowUtils.limitPeriodCounterCheck(Const.FORUM_TOPIC_CREATE_COUNTER + uid, 20, 3600))
            return "发文频繁, 请稍后再试";
        FilterResult titleFilter = filterText(vo.getTitle());
        String rawContent = vo.getContent().toJSONString();
        FilterResult contentFilterResult = filterText(rawContent);
        String filterError = topicFilterError(titleFilter, vo.getTitle(), contentFilterResult, rawContent, "发布");
        if (filterError != null)
            return filterError;
        TopicDTO dto = new TopicDTO(0, vo.getTitle(), rawContent, type, new Date(), uid, 0,
                initialTopicStatus(uid, type), 0);
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
        Integer type = resolveTopicType(vo.getType());
        if (type == null)
            return "文章类型非法！";
        TopicDTO existing = baseMapper.selectById(vo.getId());
        if (existing == null)
            return "帖子不存在";
        if (!Objects.equals(existing.getUid(), uid))
            return "只能编辑自己发布的帖子";
        FilterResult titleFilter = filterText(vo.getTitle());
        String rawContent = vo.getContent().toJSONString();
        FilterResult contentFilterResult = filterText(rawContent);
        String filterError = topicFilterError(titleFilter, vo.getTitle(), contentFilterResult, rawContent, "发布");
        if (filterError != null)
            return filterError;
        baseMapper.update(null, Wrappers.<TopicDTO>update()
                .eq("uid", uid)
                .eq("id", vo.getId())
                .set("title", vo.getTitle())
                .set("content", rawContent)
                .set("type", type)
                .set("status", initialTopicStatus(uid, type)));
        cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
        return null;
    }

    @Override
    public String createComment(int uid, AddCommentVO vo) {
        if (!flowUtils.limitPeriodCounterCheck(Const.FORUM_TOPIC_COMMENT_COUNTER + uid, 2, 60))
            return "发表评论频繁, 请稍后再试";
        if (!textLimitCheck(JSONObject.parseObject(vo.getContent()), 2000))
            return "评论内容字数过多, 请重新编辑";
        FilterResult commentFilter = filterText(vo.getContent());
        if (!commentFilter.isPassed())
            return "评论包含违禁词汇【" + commentFilter.getForbiddenWord() + "】，请修改后再提交";
        if (hasSensitiveWord(commentFilter, vo.getContent()))
            return "评论包含敏感词汇，请修改后再提交";
        TopicDTO topicDTO = baseMapper.selectById(vo.getTid());
        if (topicDTO == null)
            return "帖子不存在";
        if (!isApprovedTopic(topicDTO))
            return "帖子尚未通过审核，暂不能评论";
        AccountDTO accountDTO = accountMapper.selectById(uid);
        if (accountDTO == null)
            return "用户不存在";
        TopicCommentDTO quoteComment = null;
        if (vo.getQuote() > 0) {
            quoteComment = commentMapper.selectById(vo.getQuote());
            if (quoteComment == null)
                return "被回复的评论不存在";
        }
        TopicCommentDTO dto = new TopicCommentDTO();
        dto.setUid(uid);
        BeanUtils.copyProperties(vo, dto);
        dto.setTime(new Date());
        commentMapper.insert(dto);
        cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
        if (vo.getQuote() > 0) {
            if (!Objects.equals(accountDTO.getId(), quoteComment.getUid())) {
                notificationService.addNotification(quoteComment.getUid(), "您有新的评论回复",
                        accountDTO.getUsername() + "回复了你发表的评论", "success",
                        "/home/topic/" + quoteComment.getTid());
            }
        } else if (!Objects.equals(accountDTO.getId(), topicDTO.getUid())) {
            notificationService.addNotification(topicDTO.getUid(), "您有新的帖子评论回复",
                    accountDTO.getUsername() + "回复了你发表的主题:" + topicDTO.getTitle(), "success",
                    "/home/topic/" + topicDTO.getId());
        }
        return null;
    }

    private FilterResult filterText(String text) {
        try {
            return contentFilter.filter(text);
        } catch (Exception e) {
            log.warn("内容过滤失败，已按原文继续处理", e);
            return new FilterResult(true, null, text);
        }
    }

    private String topicFilterError(FilterResult titleFilter, String title,
                                    FilterResult contentFilterResult, String content, String action) {
        if (!titleFilter.isPassed())
            return "标题包含违禁词汇【" + titleFilter.getForbiddenWord() + "】，请修改后再" + action;
        if (hasSensitiveWord(titleFilter, title))
            return "标题包含敏感词汇，请修改后再" + action;
        if (!contentFilterResult.isPassed())
            return "内容包含违禁词汇【" + contentFilterResult.getForbiddenWord() + "】，请修改后再" + action;
        if (hasSensitiveWord(contentFilterResult, content))
            return "内容包含敏感词汇，请修改后再" + action;
        return null;
    }

    private boolean hasSensitiveWord(FilterResult result, String original) {
        return result.getFilteredContent() != null && !Objects.equals(result.getFilteredContent(), original);
    }

    @Override
    public List<CommentVO> comments(int tid, int page) {
        TopicDTO topic = baseMapper.selectById(tid);
        if (topic == null || !isApprovedTopic(topic)) return List.of();
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
        return pageTopics(pageNumber, type, 10).getRecords();
    }

    @Override
    public TopicPageVO pageTopics(int pageNumber, int type, int size) {
        int capacity = Math.max(5, Math.min(size, 30));
        List<TopicDTO> candidates = topicCandidates(type);
        List<List<TopicDTO>> visualPages = splitByVisualCapacity(candidates, capacity);
        List<TopicDTO> currentPage = pageNumber >= 0 && pageNumber < visualPages.size()
                ? visualPages.get(pageNumber)
                : List.of();
        List<TopicPreviewVO> voList = currentPage.stream().map(this::resolveToPreview).collect(Collectors.toList());
        return new TopicPageVO(voList, pageNumber, capacity, candidates.size(), visualPages.size());
    }

    private List<TopicDTO> topicCandidates(int type) {
        if (type == -1) {
            return baseMapper.selectHotTopics();
        }
        if (type == -2 || type == 0) {
            List<TopicDTO> pinned = baseMapper.selectList(Wrappers.<TopicDTO>query()
                    .eq("status", STATUS_APPROVED)
                    .eq("top", 1)
                    .orderByDesc("time"));
            List<TopicDTO> normal = baseMapper.selectList(Wrappers.<TopicDTO>query()
                    .eq("status", STATUS_APPROVED)
                    .eq("top", 0)
                    .orderByDesc("time"));
            List<TopicDTO> combined = new ArrayList<>(pinned.size() + normal.size());
            combined.addAll(pinned);
            combined.addAll(normal);
            return combined;
        }
        return baseMapper.selectList(Wrappers.<TopicDTO>query()
                .eq("status", STATUS_APPROVED)
                .eq("type", type)
                .orderByDesc("time"));
    }

    private List<List<TopicDTO>> splitByVisualCapacity(List<TopicDTO> candidates, int capacity) {
        List<List<TopicDTO>> pages = new ArrayList<>();
        List<TopicDTO> current = new ArrayList<>();
        int currentWeight = 0;
        for (TopicDTO topic : candidates) {
            int weight = topicVisualWeight(topic);
            if (!current.isEmpty() && currentWeight + weight > capacity) {
                pages.add(current);
                current = new ArrayList<>();
                currentWeight = 0;
            }
            current.add(topic);
            currentWeight += weight;
        }
        if (!current.isEmpty()) {
            pages.add(current);
        }
        return pages;
    }

    private int topicVisualWeight(TopicDTO topic) {
        return topicHasImage(topic) ? 2 : 1;
    }

    private boolean topicHasImage(TopicDTO topic) {
        try {
            JSONObject content = JSONObject.parseObject(topic.getContent());
            JSONArray ops = content.getJSONArray("ops");
            if (ops == null) return false;
            for (Object op : ops) {
                Object insert = JSONObject.from(op).get("insert");
                if (insert instanceof Map<?, ?> map && map.get("image") != null) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    @Override
    public List<TopicPreviewVO> listTopicCollects(int uid) {
        return baseMapper.collectTopics(uid).stream()
                .map(this::resolveToPreview)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<TopicTopVO> listTopTopics() {
        return baseMapper.selectTopHotTopics(5).stream()
                .map(topic -> new TopicTopVO(
                        topic.getId(),
                        topic.getTitle(),
                        topic.getTime(),
                        baseMapper.viewCount(topic.getId()),
                        Optional.ofNullable(topic.getHotScore()).orElse(0D)))
                .toList();
    }

    @Override
    public TopicDetailVO getTopic(int tid, int uid) {
        TopicDetailVO vo = new TopicDetailVO();
        TopicDTO dto = baseMapper.selectById(tid);
        if (dto == null) return null;
        if (!canViewTopic(dto, uid)) return null;
        BeanUtils.copyProperties(dto, vo);
        vo.setRawContent(dto.getContent());
        try {
            JSONObject content = JSONObject.parseObject(dto.getContent());
            vo.setTags(content.getList("tags", String.class));
            List<String> images = new LinkedList<>();
            vo.setContent(extractDisplayText(content, images));
            vo.setImages(images);
        } catch (Exception e) {
            vo.setContent(dto.getContent());
            vo.setImages(List.of());
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
        TopicDTO topic = baseMapper.selectById(interact.getTid());
        if (topic == null || !isApprovedTopic(topic)) return;
        synchronized (type.intern()) {
            try {
                template.opsForHash().put(type, interact.toKey(), Boolean.toString(state));
                cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
                this.saveInteractSchedule(type);
            } catch (Exception e) {
                log.warn("Redis 互动缓存不可用，直接写入数据库: type={}, tid={}, uid={}", type, interact.getTid(), interact.getUid());
                applyInteractToDatabase(interact, state);
            }
        }
    }

    private void applyInteractToDatabase(Interact interact, boolean state) {
        if (state) {
            baseMapper.addInteract(List.of(interact), interact.getType());
        } else {
            baseMapper.deleteInteract(List.of(interact), interact.getType());
        }
        cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
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
            try {
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
            } catch (Exception e) {
                log.warn("Redis 互动缓存同步失败，将等待后续请求重新触发: type={}", type);
            }
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
                .eq("status", STATUS_APPROVED)
                .and(q -> q.like("title", keyword).or().like("content", keyword))
                .orderByDesc("time"));
        if (p.getRecords().isEmpty()) return List.of();
        return p.getRecords().stream().map(this::resolveToPreview).collect(Collectors.toList());
    }

    @Override
    public TopicPreviewVO resolvePreviewById(int tid) {
        TopicDTO dto = this.getById(tid);
        if (dto == null || !isApprovedTopic(dto)) return null;
        return resolveToPreview(dto);
    }

    private Integer resolveTopicType(Integer requestedType) {
        if (requestedType == null || requestedType == 0) {
            return this.listTypes().stream()
                    .map(TopicTypeDTO::getId)
                    .filter(id -> id != null && id > 0)
                    .findFirst()
                    .orElse(null);
        }
        return types.contains(requestedType) ? requestedType : null;
    }

    private String initialTopicStatus(int uid, int type) {
        return STATUS_APPROVED;
    }

    private boolean canBypassTopicAudit(int uid, int type) {
        AccountDTO account = accountMapper.selectById(uid);
        if (account == null) return false;
        String role = account.getRole();
        if ("admin".equals(role) || "content_admin".equals(role)) return true;
        return "moderator".equals(role)
                && account.getModeratorType() != null
                && account.getModeratorType().equals(type);
    }

    private boolean canViewTopic(TopicDTO topic, int uid) {
        if (isApprovedTopic(topic)) return true;
        if (Objects.equals(topic.getUid(), uid)) return true;
        AccountDTO account = accountMapper.selectById(uid);
        if (account == null) return false;
        String role = account.getRole();
        if ("admin".equals(role) || "content_admin".equals(role)) return true;
        return "moderator".equals(role)
                && account.getModeratorType() != null
                && account.getModeratorType().equals(topic.getType());
    }

    private boolean isApprovedTopic(TopicDTO topic) {
        return STATUS_APPROVED.equals(Optional.ofNullable(topic.getStatus()).orElse(STATUS_APPROVED));
    }

    private TopicPreviewVO resolveToPreview(TopicDTO dto) {
        AccountDTO account = accountMapper.selectById(dto.getUid());
        TopicPreviewVO vo = new TopicPreviewVO(dto.getId(), dto.getType(), dto.getTitle(), null, null, dto.getTime(),
                dto.getUid(), account != null ? account.getUsername() : "已注销用户", account != null ? account.getAvatar() : null,
                interactCountWithPending(dto.getId(), "like"),
                interactCountWithPending(dto.getId(), "collect"),
                commentMapper.selectCount(Wrappers.<TopicCommentDTO>query().eq("tid", dto.getId())).intValue(),
                baseMapper.viewCount(dto.getId()),
                Optional.ofNullable(dto.getHotScore()).orElse(0D),
                null, dto.getTop(), dto.getFeatured());
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

    private String extractDisplayText(JSONObject content, List<String> images) {
        String md = content.getString("md");
        if (md != null) return md;
        StringBuilder previewText = new StringBuilder();
        shortContent(content.getJSONArray("ops"), previewText, obj -> images.add(obj.toString()));
        return previewText.toString();
    }

    private void shortContent(JSONArray ops, StringBuilder previewText, Consumer<Object> imagesHandler) {
        if (ops == null) return;
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
