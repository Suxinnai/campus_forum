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

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug Condition Exploration Test for Article Detail Garbled Text Fix
 * 
 * **Validates: Requirements 1.1, 1.3, 2.1, 2.2, 2.3**
 * 
 * CRITICAL: This test MUST FAIL on unfixed code - failure confirms the bug exists.
 * The test encodes the expected behavior and will validate the fix when it passes after implementation.
 * 
 * Bug Condition: Content field returns raw JSON string instead of formatted text
 */
@SpringBootTest
@Transactional
public class TopicServiceBugConditionTest {

    @Autowired
    private TopicService topicService;

    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private AccountMapper accountMapper;

    /**
     * Property 1: Bug Condition - Content field returns formatted text
     * 
     * This test verifies that getTopic() returns formatted text content
     * instead of raw JSON string from the database.
     * 
     * Expected on UNFIXED code: FAIL (content contains raw JSON)
     * Expected on FIXED code: PASS (content contains formatted text)
     */
    @Test
    public void testGetTopicReturnsFormattedTextNotRawJson() {
        // Arrange: Create a topic with Quill Delta format JSON content
        String jsonContent = createQuillDeltaJson("每天中午12点，食堂二楼的黄焖鸡窗口队伍能排到门外去");
        
        TopicDTO topic = new TopicDTO();
        topic.setTitle("测试帖子");
        topic.setContent(jsonContent);
        topic.setType(1);
        topic.setTime(new Date());
        topic.setUid(getTestUserId());
        topic.setTop(0);
        
        topicMapper.insert(topic);
        Integer topicId = topic.getId();
        
        // Act: Call getTopic() on UNFIXED code
        TopicDetailVO result = topicService.getTopic(topicId, getTestUserId());
        
        // Assert: Content should be formatted text, NOT raw JSON
        assertNotNull(result, "TopicDetailVO should not be null");
        assertNotNull(result.getContent(), "Content field should not be null");
        
        // Bug Condition Check: On unfixed code, content will contain raw JSON string
        // This assertion will FAIL on unfixed code (confirming bug exists)
        assertFalse(result.getContent().contains("\"ops\""), 
            "Content should not contain 'ops' keyword from JSON structure");
        assertFalse(result.getContent().contains("\"insert\""), 
            "Content should not contain 'insert' keyword from JSON structure");
        assertFalse(result.getContent().startsWith("{"), 
            "Content should not start with '{' (JSON object indicator)");
        
        // Expected behavior: Content should be the extracted text
        assertTrue(result.getContent().contains("每天中午12点"), 
            "Content should contain the actual text from ops array");
    }

    /**
     * Property 1 - Multiple Text Segments
     * 
     * Tests content with multiple insert operations
     */
    @Test
    public void testGetTopicWithMultipleTextSegments() {
        // Arrange: Create content with multiple text segments
        JSONObject content = new JSONObject();
        JSONArray ops = new JSONArray();
        
        JSONObject op1 = new JSONObject();
        op1.put("insert", "第一段文本\n");
        ops.add(op1);
        
        JSONObject op2 = new JSONObject();
        op2.put("insert", "第二段文本");
        ops.add(op2);
        
        content.put("ops", ops);
        content.put("tags", new JSONArray());
        
        TopicDTO topic = new TopicDTO();
        topic.setTitle("多段文本测试");
        topic.setContent(content.toJSONString());
        topic.setType(1);
        topic.setTime(new Date());
        topic.setUid(getTestUserId());
        topic.setTop(0);
        
        topicMapper.insert(topic);
        Integer topicId = topic.getId();
        
        // Act
        TopicDetailVO result = topicService.getTopic(topicId, getTestUserId());
        
        // Assert: Should return combined text, not JSON
        assertNotNull(result.getContent());
        assertFalse(result.getContent().contains("\"ops\""), 
            "Content should not contain JSON structure keywords");
        assertTrue(result.getContent().contains("第一段文本"), 
            "Content should contain first text segment");
        assertTrue(result.getContent().contains("第二段文本"), 
            "Content should contain second text segment");
    }

    /**
     * Property 1 - Content with Images
     * 
     * Tests content with both text and image objects
     */
    @Test
    public void testGetTopicWithImagesExtractsOnlyText() {
        // Arrange: Create content with text and image
        JSONObject content = new JSONObject();
        JSONArray ops = new JSONArray();
        
        JSONObject textOp = new JSONObject();
        textOp.put("insert", "文本内容");
        ops.add(textOp);
        
        JSONObject imageOp = new JSONObject();
        JSONObject imageData = new JSONObject();
        imageData.put("image", "http://example.com/image.jpg");
        imageOp.put("insert", imageData);
        ops.add(imageOp);
        
        JSONObject moreTextOp = new JSONObject();
        moreTextOp.put("insert", "更多文本");
        ops.add(moreTextOp);
        
        content.put("ops", ops);
        content.put("tags", new JSONArray());
        
        TopicDTO topic = new TopicDTO();
        topic.setTitle("图文混合测试");
        topic.setContent(content.toJSONString());
        topic.setType(1);
        topic.setTime(new Date());
        topic.setUid(getTestUserId());
        topic.setTop(0);
        
        topicMapper.insert(topic);
        Integer topicId = topic.getId();
        
        // Act
        TopicDetailVO result = topicService.getTopic(topicId, getTestUserId());
        
        // Assert: Should extract only text, skip images
        assertNotNull(result.getContent());
        assertFalse(result.getContent().contains("\"image\""), 
            "Content should not contain image JSON structure");
        assertTrue(result.getContent().contains("文本内容"), 
            "Content should contain first text");
        assertTrue(result.getContent().contains("更多文本"), 
            "Content should contain second text");
    }

    // Helper methods
    
    private String createQuillDeltaJson(String text) {
        JSONObject content = new JSONObject();
        JSONArray ops = new JSONArray();
        JSONObject op = new JSONObject();
        op.put("insert", text);
        ops.add(op);
        content.put("ops", ops);
        content.put("tags", new JSONArray());
        return content.toJSONString();
    }
    
    private Integer getTestUserId() {
        // Return the first available user ID from database
        // In a real scenario, this should be a test user
        return accountMapper.selectList(null).stream()
            .findFirst()
            .map(account -> account.getId())
            .orElse(1); // Fallback to ID 1
    }
}
