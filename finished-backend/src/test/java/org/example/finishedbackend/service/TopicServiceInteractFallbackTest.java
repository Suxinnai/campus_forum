package org.example.finishedbackend.service;

import org.example.finishedbackend.entity.DTO.Interact;
import org.example.finishedbackend.entity.DTO.TopicDTO;
import org.example.finishedbackend.entity.VO.request.TopicCreateVO;
import org.example.finishedbackend.mapper.TopicMapper;
import org.example.finishedbackend.service.Impl.TopicServiceImpl;
import org.example.finishedbackend.service.filter.ContentFilter;
import org.example.finishedbackend.service.filter.FilterResult;
import org.example.finishedbackend.utils.CacheUtils;
import org.example.finishedbackend.utils.Const;
import org.example.finishedbackend.utils.FlowUtils;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TopicServiceInteractFallbackTest {

    @Test
    public void testNewTopicsAreApprovedImmediately() {
        TopicServiceImpl service = new TopicServiceImpl();

        String status = ReflectionTestUtils.invokeMethod(service, "initialTopicStatus", 2, 1);

        assertEquals("approved", status);
    }

    @Test
    public void testCreateTopicRejectsSensitiveWordsBeforeSaving() {
        TopicMapper topicMapper = mock(TopicMapper.class);
        CacheUtils cacheUtils = mock(CacheUtils.class);
        FlowUtils flowUtils = mock(FlowUtils.class);
        ContentFilter contentFilter = mock(ContentFilter.class);
        TopicServiceImpl service = createPublishService(topicMapper, cacheUtils, flowUtils, contentFilter);

        JSONObject content = new JSONObject();
        content.put("ops", new Object[]{JSONObject.of("insert", "hello sb")});
        TopicCreateVO vo = new TopicCreateVO();
        vo.setTitle("SB title");
        vo.setType(1);
        vo.setContent(content);
        String rawContent = content.toJSONString();
        String filteredContent = rawContent.replace("sb", "**");
        when(flowUtils.limitPeriodCounterCheck(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(contentFilter.filter("SB title")).thenReturn(new FilterResult(true, null, "** title"));
        when(contentFilter.filter(rawContent)).thenReturn(new FilterResult(true, null, filteredContent));

        String result = service.createTopic(2, vo);

        verify(topicMapper, never()).insert(any(TopicDTO.class));
        assertEquals("标题包含敏感词汇，请修改后再发布", result);
    }

    @Test
    public void testCreateTopicRejectsForbiddenWordsBeforeSaving() {
        TopicMapper topicMapper = mock(TopicMapper.class);
        CacheUtils cacheUtils = mock(CacheUtils.class);
        FlowUtils flowUtils = mock(FlowUtils.class);
        ContentFilter contentFilter = mock(ContentFilter.class);
        TopicServiceImpl service = createPublishService(topicMapper, cacheUtils, flowUtils, contentFilter);

        JSONObject content = new JSONObject();
        content.put("ops", new Object[]{JSONObject.of("insert", "normal")});
        TopicCreateVO vo = new TopicCreateVO();
        vo.setTitle("blocked title");
        vo.setType(1);
        vo.setContent(content);
        when(flowUtils.limitPeriodCounterCheck(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(contentFilter.filter("blocked title")).thenReturn(new FilterResult(false, "blocked", null));
        when(contentFilter.filter(content.toJSONString())).thenReturn(new FilterResult(true, null, content.toJSONString()));

        String result = service.createTopic(2, vo);

        verify(topicMapper, never()).insert(any(TopicDTO.class));
        assertEquals("标题包含违禁词汇【blocked】，请修改后再发布", result);
    }

    @Test
    public void testInteractFallsBackToDatabaseWhenRedisWriteFails() {
        TopicMapper topicMapper = mock(TopicMapper.class);
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        HashOperations hashOperations = mock(HashOperations.class);
        CacheUtils cacheUtils = mock(CacheUtils.class);
        TopicServiceImpl service = createService(topicMapper, template, cacheUtils);

        TopicDTO topic = approvedTopic(10);
        when(topicMapper.selectById(10)).thenReturn(topic);
        when(template.opsForHash()).thenReturn(hashOperations);
        doThrow(new RuntimeException("redis down"))
                .when(hashOperations).put(eq("like"), eq("10:5"), eq("true"));

        service.interact(new Interact(10, 5, new Date(), "like"), true);

        verify(topicMapper).addInteract(anyList(), eq("like"));
        verify(cacheUtils).deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
    }

    @Test
    public void testCancelInteractFallsBackToDatabaseWhenRedisWriteFails() {
        TopicMapper topicMapper = mock(TopicMapper.class);
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        HashOperations hashOperations = mock(HashOperations.class);
        CacheUtils cacheUtils = mock(CacheUtils.class);
        TopicServiceImpl service = createService(topicMapper, template, cacheUtils);

        TopicDTO topic = approvedTopic(10);
        when(topicMapper.selectById(10)).thenReturn(topic);
        when(template.opsForHash()).thenReturn(hashOperations);
        doThrow(new RuntimeException("redis down"))
                .when(hashOperations).put(eq("collect"), eq("10:5"), eq("false"));

        service.interact(new Interact(10, 5, new Date(), "collect"), false);

        verify(topicMapper).deleteInteract(anyList(), eq("collect"));
        verify(cacheUtils).deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
    }

    private TopicServiceImpl createService(TopicMapper topicMapper, StringRedisTemplate template, CacheUtils cacheUtils) {
        TopicServiceImpl service = new TopicServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", topicMapper);
        ReflectionTestUtils.setField(service, "template", template);
        ReflectionTestUtils.setField(service, "cacheUtils", cacheUtils);
        return service;
    }

    private TopicServiceImpl createPublishService(TopicMapper topicMapper, CacheUtils cacheUtils,
                                                 FlowUtils flowUtils, ContentFilter contentFilter) {
        TopicServiceImpl service = new TopicServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", topicMapper);
        ReflectionTestUtils.setField(service, "cacheUtils", cacheUtils);
        ReflectionTestUtils.setField(service, "flowUtils", flowUtils);
        ReflectionTestUtils.setField(service, "contentFilter", contentFilter);
        ReflectionTestUtils.setField(service, "types", Set.of(1));
        return service;
    }

    private TopicDTO approvedTopic(int id) {
        TopicDTO topic = new TopicDTO();
        topic.setId(id);
        topic.setStatus("approved");
        return topic;
    }
}
