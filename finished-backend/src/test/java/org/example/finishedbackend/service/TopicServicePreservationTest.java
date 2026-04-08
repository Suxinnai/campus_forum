package org.example.finishedbackend.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.example.finishedbackend.entity.DTO.TopicDTO;
import org.example.finishedbackend.entity.VO.response.TopicDetailVO;
import org.example.finishedbackend.mapper.TopicMapper;
import org.example.finishedbackend.mapper.AccountMapper;
import org.example.finishedbackend.mapper.AccountDetailsMapper;
import org.example.finishedbackend.mapper.AccountPrivacyMapper;
import org.example.finishedbackend.mapper.TopicCommentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Preservation Property Tests for Article Detail Garbled Text Fix
 * 
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5**
 * 
 * These tests verify that the fix does NOT break existing behavior:
 * - Other fields (id, title, type, time, user, interact, comments, tags) remain unchanged
 * - Tags extraction continues to work correctly
 * - Exception handling continues to work
 * - Other API interfaces remain unaffected
 * 
 * IMPORTANT: These tests should PASS on UNFIXED code (baseline behavior)
 * and continue to PASS on FIXED code (no regressions).
 */
@SpringBootTest
@Transactional
public class TopicServicePreservationTest {

    @Autowired
    private TopicService topicService;

    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private TopicCommentMapper commentMapper;

    /**
     * Property 2: Preservation - ID field remains unchanged
     * 
     * Validates: Requirement 3.1
     */
    @Test
    public void testGetTopicPreservesIdField() {
        // Arrange
        String jsonContent = createQuillDeltaJsonWithTags("测试内容", Arrays.asList("标签1", "标签2"));
        TopicDTO topic = createAndInsertTopic("测试标题", jsonContent, 1);
        Integer expectedId = topic.getId();
        
        // Act
        TopicDetailVO result = topicService.getTopic(expectedId, getTestUserId());
        
        // Assert: ID field should be preserved exactly
        assertNotNull(result, "TopicDetailVO should not be null");
        assertEquals(expectedId, result.getId(), "ID field should be preserved");
    }

    /**
     * Property 2: Preservation - Title field remains unchanged
     * 
     * Validates: Requirement 3.1
     */
    @Test
    public void testGetTopicPreservesTitleField() {
        // Arrange
        String expectedTitle = "这是一个测试标题";
        String jsonContent = createQuillDeltaJsonWithTags("测试内容", Arrays.asList("标签1"));
        TopicDTO topic = createAndInsertTopic(expectedTitle, jsonContent, 1);
        
        // Act
        TopicDetailVO result = topicService.getTopic(topic.getId(), getTestUserId());
        
        // Assert: Title field should be preserved exactly
        assertNotNull(result);
        assertEquals(expectedTitle, result.getTitle(), "Title field should be preserved");
    }

    /**
     * Property 2: Preservation - Type field remains unchanged
     * 
     * Validates: Requirement 3.1
     */
    @Test
    public void testGetTopicPreservesTypeField() {
        // Arrange
        Integer expectedType = 2;
        String jsonContent = createQuillDeltaJsonWithTags("测试内容", Arrays.asList());
        TopicDTO topic = createAndInsertTopic("测试标题", jsonContent, expectedType);
        
        // Act
        TopicDetailVO result = topicService.getTopic(topic.getId(), getTestUserId());
        
        // Assert: Type field should be preserved exactly
        assertNotNull(result);
        assertEquals(expectedType, result.getType(), "Type field should be preserved");
    }

    /**
     * Property 2: Preservation - Time field remains unchanged
     * 
     * Validates: Requirement 3.1
     */
    @Test
    public void testGetTopicPreservesTimeField() {
        // Arrange
        String jsonContent = createQuillDeltaJsonWithTags("测试内容", Arrays.asList());
        TopicDTO topic = createAndInsertTopic("测试标题", jsonContent, 1);
        Date expectedTime = topic.getTime();
        
        // Act
        TopicDetailVO result = topicService.getTopic(topic.getId(), getTestUserId());
        
        // Assert: Time field should be preserved (within 1 second due to DB rounding)
        assertNotNull(result);
        assertNotNull(result.getTime(), "Time field should not be null");
        long timeDiff = Math.abs(expectedTime.getTime() - result.getTime().getTime());
        assertTrue(timeDiff < 1000, 
            "Time field should be preserved (within 1 second tolerance)");
    }

    /**
     * Property 2: Preservation - User field remains unchanged
     * 
     * Validates: Requirement 3.1
     */
    @Test
    public void testGetTopicPreservesUserField() {
        // Arrange
        String jsonContent = createQuillDeltaJsonWithTags("测试内容", Arrays.asList());
        TopicDTO topic = createAndInsertTopic("测试标题", jsonContent, 1);
        Integer expectedUserId = topic.getUid();
        
        // Act
        TopicDetailVO result = topicService.getTopic(topic.getId(), getTestUserId());
        
        // Assert: User field should be populated correctly
        assertNotNull(result);
        assertNotNull(result.getUser(), "User field should not be null");
        assertEquals(expectedUserId, result.getUser().getId(), 
            "User ID should match the topic's UID");
        assertNotNull(result.getUser().getUsername(), 
            "Username should be populated");
    }

    /**
     * Property 2: Preservation - Interact field remains unchanged
     * 
     * Validates: Requirement 3.1
     */
    @Test
    public void testGetTopicPreservesInteractField() {
        // Arrange
        String jsonContent = createQuillDeltaJsonWithTags("测试内容", Arrays.asList());
        TopicDTO topic = createAndInsertTopic("测试标题", jsonContent, 1);
        Integer testUserId = getTestUserId();
        
        // Act
        TopicDetailVO result = topicService.getTopic(topic.getId(), testUserId);
        
        // Assert: Interact field should be populated correctly
        assertNotNull(result);
        assertNotNull(result.getInteract(), "Interact field should not be null");
        assertNotNull(result.getInteract().getLike(), 
            "Like status should be populated (true or false)");
        assertNotNull(result.getInteract().getCollect(), 
            "Collect status should be populated (true or false)");
    }

    /**
     * Property 2: Preservation - Comments count remains unchanged
     * 
     * Validates: Requirement 3.1
     */
    @Test
    public void testGetTopicPreservesCommentsCount() {
        // Arrange
        String jsonContent = createQuillDeltaJsonWithTags("测试内容", Arrays.asList());
        TopicDTO topic = createAndInsertTopic("测试标题", jsonContent, 1);
        
        // Act
        TopicDetailVO result = topicService.getTopic(topic.getId(), getTestUserId());
        
        // Assert: Comments count should be present and valid
        assertNotNull(result);
        assertNotNull(result.getComments(), "Comments count should not be null");
        assertTrue(result.getComments() >= 0, 
            "Comments count should be non-negative");
    }

    /**
     * Property 2: Preservation - Tags extraction continues to work
     * 
     * Validates: Requirement 3.2
     */
    @Test
    public void testGetTopicPreservesTagsExtraction() {
        // Arrange: Create content with tags
        List<String> expectedTags = Arrays.asList("校园生活", "美食", "推荐");
        String jsonContent = createQuillDeltaJsonWithTags("测试内容", expectedTags);
        TopicDTO topic = createAndInsertTopic("测试标题", jsonContent, 1);
        
        // Act
        TopicDetailVO result = topicService.getTopic(topic.getId(), getTestUserId());
        
        // Assert: Tags should be extracted correctly from JSON
        assertNotNull(result);
        assertNotNull(result.getTags(), "Tags field should not be null");
        assertEquals(expectedTags.size(), result.getTags().size(), 
            "Tags count should match");
        assertTrue(result.getTags().containsAll(expectedTags), 
            "All tags should be extracted correctly");
    }

    /**
     * Property 2: Preservation - Empty tags list is handled correctly
     * 
     * Validates: Requirement 3.2
     */
    @Test
    public void testGetTopicPreservesEmptyTagsList() {
        // Arrange: Create content with empty tags
        String jsonContent = createQuillDeltaJsonWithTags("测试内容", Arrays.asList());
        TopicDTO topic = createAndInsertTopic("测试标题", jsonContent, 1);
        
        // Act
        TopicDetailVO result = topicService.getTopic(topic.getId(), getTestUserId());
        
        // Assert: Empty tags list should be handled correctly
        assertNotNull(result);
        assertNotNull(result.getTags(), "Tags field should not be null even when empty");
        assertTrue(result.getTags().isEmpty(), "Tags list should be empty");
    }

    /**
     * Property 2: Preservation - Exception handling for empty content
     * 
     * Validates: Requirement 3.3
     */
    @Test
    public void testGetTopicHandlesEmptyContentGracefully() {
        // Arrange: Create topic with empty content (database doesn't allow null)
        TopicDTO topic = new TopicDTO();
        topic.setTitle("空内容测试");
        topic.setContent("");
        topic.setType(1);
        topic.setTime(new Date());
        topic.setUid(getTestUserId());
        topic.setTop(0);
        topicMapper.insert(topic);
        
        // Act: Should not throw exception
        TopicDetailVO result = topicService.getTopic(topic.getId(), getTestUserId());
        
        // Assert: Other fields should still be populated
        assertNotNull(result, "TopicDetailVO should not be null even with empty content");
        assertEquals(topic.getId(), result.getId(), "ID should be preserved");
        assertEquals(topic.getTitle(), result.getTitle(), "Title should be preserved");
        assertNotNull(result.getUser(), "User field should still be populated");
    }

    /**
     * Property 2: Preservation - Exception handling for malformed JSON
     * 
     * Validates: Requirement 3.3
     */
    @Test
    public void testGetTopicHandlesMalformedJsonGracefully() {
        // Arrange: Create topic with malformed JSON content
        TopicDTO topic = new TopicDTO();
        topic.setTitle("格式错误测试");
        topic.setContent("{invalid json content}");
        topic.setType(1);
        topic.setTime(new Date());
        topic.setUid(getTestUserId());
        topic.setTop(0);
        topicMapper.insert(topic);
        
        // Act: Should not throw exception
        TopicDetailVO result = topicService.getTopic(topic.getId(), getTestUserId());
        
        // Assert: Other fields should still be populated
        assertNotNull(result, "TopicDetailVO should not be null even with malformed JSON");
        assertEquals(topic.getId(), result.getId(), "ID should be preserved");
        assertEquals(topic.getTitle(), result.getTitle(), "Title should be preserved");
        assertNotNull(result.getUser(), "User field should still be populated");
    }

    /**
     * Property 2: Preservation - All fields together remain consistent
     * 
     * This is a comprehensive test that validates multiple fields at once
     * 
     * Validates: Requirements 3.1, 3.2, 3.3
     */
    @Test
    public void testGetTopicPreservesAllFieldsTogether() {
        // Arrange: Create a complete topic with all fields
        List<String> expectedTags = Arrays.asList("综合", "测试");
        String jsonContent = createQuillDeltaJsonWithTags("完整测试内容", expectedTags);
        Integer expectedType = 3;
        String expectedTitle = "完整字段测试";
        
        TopicDTO topic = createAndInsertTopic(expectedTitle, jsonContent, expectedType);
        Integer expectedId = topic.getId();
        Date expectedTime = topic.getTime();
        Integer expectedUserId = topic.getUid();
        
        // Act
        TopicDetailVO result = topicService.getTopic(expectedId, getTestUserId());
        
        // Assert: All fields should be preserved
        assertNotNull(result, "TopicDetailVO should not be null");
        
        // Verify all fields
        assertEquals(expectedId, result.getId(), "ID should be preserved");
        assertEquals(expectedTitle, result.getTitle(), "Title should be preserved");
        assertEquals(expectedType, result.getType(), "Type should be preserved");
        long timeDiff = Math.abs(expectedTime.getTime() - result.getTime().getTime());
        assertTrue(timeDiff < 1000, 
            "Time should be preserved (within 1 second tolerance)");
        
        assertNotNull(result.getUser(), "User should be populated");
        assertEquals(expectedUserId, result.getUser().getId(), "User ID should match");
        
        assertNotNull(result.getInteract(), "Interact should be populated");
        assertNotNull(result.getComments(), "Comments count should be populated");
        
        assertNotNull(result.getTags(), "Tags should be populated");
        assertEquals(expectedTags.size(), result.getTags().size(), 
            "Tags count should match");
        assertTrue(result.getTags().containsAll(expectedTags), 
            "All tags should be extracted");
    }

    // Helper methods
    
    private String createQuillDeltaJsonWithTags(String text, List<String> tags) {
        JSONObject content = new JSONObject();
        JSONArray ops = new JSONArray();
        JSONObject op = new JSONObject();
        op.put("insert", text);
        ops.add(op);
        content.put("ops", ops);
        
        JSONArray tagsArray = new JSONArray();
        tagsArray.addAll(tags);
        content.put("tags", tagsArray);
        
        return content.toJSONString();
    }
    
    private TopicDTO createAndInsertTopic(String title, String content, Integer type) {
        TopicDTO topic = new TopicDTO();
        topic.setTitle(title);
        topic.setContent(content);
        topic.setType(type);
        topic.setTime(new Date());
        topic.setUid(getTestUserId());
        topic.setTop(0);
        topicMapper.insert(topic);
        return topic;
    }
    
    private Integer getTestUserId() {
        // Return the first available user ID from database
        return accountMapper.selectList(null).stream()
            .findFirst()
            .map(account -> account.getId())
            .orElse(1); // Fallback to ID 1
    }
}
