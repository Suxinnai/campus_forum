package org.example.finishedbackend.service.filter;

import org.example.finishedbackend.entity.DTO.SensitiveWordDTO;
import org.example.finishedbackend.mapper.SensitiveWordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ContentFilter implementation
 * 
 * **Validates: Requirements 11.1, 11.2, 11.3, 11.4**
 * 
 * Tests cover:
 * - Forbidden word detection (exact match, case insensitive)
 * - Sensitive word replacement with asterisks
 * - Empty content and special character handling
 */
@SpringBootTest
@Transactional
public class ContentFilterTest {

    @Autowired
    private ContentFilter contentFilter;

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    @BeforeEach
    public void setUp() {
        // Clear existing words and insert test data
        sensitiveWordMapper.delete(null);
        
        // Insert test forbidden/sensitive words
        insertWord("违禁词");
        insertWord("敏感词");
        insertWord("测试");
    }

    // ========== Forbidden Word Detection Tests ==========

    /**
     * Test forbidden word detection with exact match
     * 
     * Validates: Requirement 11.1
     */
    @Test
    public void testCheckForbiddenWords_ExactMatch() {
        // Arrange
        String content = "这是一条包含违禁词的评论";
        
        // Act
        String result = contentFilter.checkForbiddenWords(content);
        
        // Assert
        assertNotNull(result, "Should detect forbidden word");
        assertEquals("违禁词", result, "Should return the exact forbidden word");
    }

    /**
     * Test forbidden word detection is case insensitive
     * 
     * Validates: Requirement 11.1
     */
    @Test
    public void testCheckForbiddenWords_CaseInsensitive() {
        // Arrange
        String content = "这是一条包含违禁词的评论";
        
        // Act
        String result = contentFilter.checkForbiddenWords(content);
        
        // Assert
        assertNotNull(result, "Should detect forbidden word regardless of case");
    }

    /**
     * Test forbidden word detection with uppercase content
     * 
     * Validates: Requirement 11.1
     */
    @Test
    public void testCheckForbiddenWords_UppercaseContent() {
        // Arrange: Insert English word for case testing
        insertWord("forbidden");
        String content = "This contains FORBIDDEN word";
        
        // Act
        String result = contentFilter.checkForbiddenWords(content);
        
        // Assert
        assertNotNull(result, "Should detect forbidden word in uppercase");
        assertEquals("forbidden", result.toLowerCase(), "Should return the forbidden word");
    }

    /**
     * Test no forbidden word detected in clean content
     * 
     * Validates: Requirement 11.1
     */
    @Test
    public void testCheckForbiddenWords_NoMatch() {
        // Arrange
        String content = "这是一条正常的评论内容";
        
        // Act
        String result = contentFilter.checkForbiddenWords(content);
        
        // Assert
        assertNull(result, "Should return null when no forbidden word is found");
    }

    /**
     * Test forbidden word detection with null content
     * 
     * Validates: Requirement 11.1
     */
    @Test
    public void testCheckForbiddenWords_NullContent() {
        // Act
        String result = contentFilter.checkForbiddenWords(null);
        
        // Assert
        assertNull(result, "Should return null for null content");
    }

    /**
     * Test forbidden word detection with empty content
     * 
     * Validates: Requirement 11.1
     */
    @Test
    public void testCheckForbiddenWords_EmptyContent() {
        // Act
        String result = contentFilter.checkForbiddenWords("");
        
        // Assert
        assertNull(result, "Should return null for empty content");
    }

    /**
     * Test forbidden word detection with blank content
     * 
     * Validates: Requirement 11.1
     */
    @Test
    public void testCheckForbiddenWords_BlankContent() {
        // Act
        String result = contentFilter.checkForbiddenWords("   ");
        
        // Assert
        assertNull(result, "Should return null for blank content");
    }

    // ========== Sensitive Word Replacement Tests ==========

    /**
     * Test sensitive word replacement with asterisks
     * 
     * Validates: Requirement 11.3, 11.4
     */
    @Test
    public void testReplaceSensitiveWords_BasicReplacement() {
        // Arrange
        String content = "这是一条包含敏感词的评论";
        
        // Act
        String result = contentFilter.replaceSensitiveWords(content);
        
        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.contains("***"), "Sensitive word should be replaced with asterisks");
        assertFalse(result.contains("敏感词"), "Original sensitive word should be removed");
        assertEquals("这是一条包含***的评论", result, "Should replace sensitive word with correct number of asterisks");
    }

    /**
     * Test sensitive word replacement is case insensitive
     * 
     * Validates: Requirement 11.3, 11.4
     */
    @Test
    public void testReplaceSensitiveWords_CaseInsensitive() {
        // Arrange: Insert English word for case testing
        insertWord("bad");
        String content = "This is BAD and Bad content";
        
        // Act
        String result = contentFilter.replaceSensitiveWords(content);
        
        // Assert
        assertNotNull(result, "Result should not be null");
        assertFalse(result.toLowerCase().contains("bad"), "All variations of sensitive word should be replaced");
        assertTrue(result.contains("***"), "Should contain asterisks");
    }

    /**
     * Test multiple sensitive words replacement
     * 
     * Validates: Requirement 11.3, 11.4
     */
    @Test
    public void testReplaceSensitiveWords_MultipleWords() {
        // Arrange
        String content = "这条评论包含敏感词和测试词汇";
        
        // Act
        String result = contentFilter.replaceSensitiveWords(content);
        
        // Assert
        assertNotNull(result, "Result should not be null");
        assertFalse(result.contains("敏感词"), "First sensitive word should be replaced");
        assertFalse(result.contains("测试"), "Second sensitive word should be replaced");
        assertTrue(result.contains("***"), "Should contain asterisks for first word");
        assertTrue(result.contains("**"), "Should contain asterisks for second word");
    }

    /**
     * Test sensitive word replacement with no matches
     * 
     * Validates: Requirement 11.3, 11.4
     */
    @Test
    public void testReplaceSensitiveWords_NoMatch() {
        // Arrange
        String content = "这是一条正常的评论内容";
        
        // Act
        String result = contentFilter.replaceSensitiveWords(content);
        
        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(content, result, "Content should remain unchanged when no sensitive words found");
    }

    /**
     * Test sensitive word replacement with null content
     * 
     * Validates: Requirement 11.3, 11.4
     */
    @Test
    public void testReplaceSensitiveWords_NullContent() {
        // Act
        String result = contentFilter.replaceSensitiveWords(null);
        
        // Assert
        assertNull(result, "Should return null for null content");
    }

    /**
     * Test sensitive word replacement with empty content
     * 
     * Validates: Requirement 11.3, 11.4
     */
    @Test
    public void testReplaceSensitiveWords_EmptyContent() {
        // Act
        String result = contentFilter.replaceSensitiveWords("");
        
        // Assert
        assertEquals("", result, "Should return empty string for empty content");
    }

    /**
     * Test sensitive word replacement with blank content
     * 
     * Validates: Requirement 11.3, 11.4
     */
    @Test
    public void testReplaceSensitiveWords_BlankContent() {
        // Arrange
        String content = "   ";
        
        // Act
        String result = contentFilter.replaceSensitiveWords(content);
        
        // Assert
        assertEquals(content, result, "Should return blank content unchanged");
    }

    /**
     * Test sensitive word replacement with special characters
     * 
     * Validates: Requirement 11.3, 11.4
     */
    @Test
    public void testReplaceSensitiveWords_SpecialCharacters() {
        // Arrange
        String content = "特殊字符!@#$%^&*()敏感词[]{}";
        
        // Act
        String result = contentFilter.replaceSensitiveWords(content);
        
        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.contains("!@#$%^&*()"), "Special characters should be preserved");
        assertTrue(result.contains("[]{}"), "Special characters should be preserved");
        assertFalse(result.contains("敏感词"), "Sensitive word should be replaced");
    }

    @Test
    public void testReplaceSensitiveWords_SkipsForbiddenTypedWords() {
        // Arrange
        insertWord("禁止词", "forbidden");
        String content = "这条评论包含禁止词";

        // Act
        String result = contentFilter.replaceSensitiveWords(content);

        // Assert
        assertEquals(content, result, "Forbidden typed words should be blocked by filter instead of masked");
    }

    // ========== Comprehensive Filter Tests ==========

    /**
     * Test comprehensive filter rejects content with forbidden words
     * 
     * Validates: Requirement 11.1, 11.2
     */
    @Test
    public void testFilter_RejectsForbiddenWords() {
        // Arrange
        String content = "这条评论包含违禁词";
        
        // Act
        FilterResult result = contentFilter.filter(content);
        
        // Assert
        assertNotNull(result, "FilterResult should not be null");
        assertFalse(result.isPassed(), "Filter should not pass with forbidden word");
        assertEquals("违禁词", result.getForbiddenWord(), "Should return the forbidden word");
        assertNull(result.getFilteredContent(), "Filtered content should be null when rejected");
    }

    /**
     * Test comprehensive filter replaces sensitive words
     * 
     * Validates: Requirement 11.3, 11.4
     */
    @Test
    public void testFilter_ReplacesSensitiveWords() {
        // Arrange
        String content = "这条评论包含敏感词";
        
        // Act
        FilterResult result = contentFilter.filter(content);
        
        // Assert
        assertNotNull(result, "FilterResult should not be null");
        assertTrue(result.isPassed(), "Filter should pass with only sensitive words");
        assertNull(result.getForbiddenWord(), "Forbidden word should be null");
        assertNotNull(result.getFilteredContent(), "Filtered content should not be null");
        assertEquals("这条评论包含***", result.getFilteredContent(), "Sensitive words should be replaced");
    }

    /**
     * Test comprehensive filter with clean content
     * 
     * Validates: Requirement 11.1, 11.3
     */
    @Test
    public void testFilter_CleanContent() {
        // Arrange
        String content = "这是一条正常的评论内容";
        
        // Act
        FilterResult result = contentFilter.filter(content);
        
        // Assert
        assertNotNull(result, "FilterResult should not be null");
        assertTrue(result.isPassed(), "Filter should pass with clean content");
        assertNull(result.getForbiddenWord(), "Forbidden word should be null");
        assertEquals(content, result.getFilteredContent(), "Content should remain unchanged");
    }

    /**
     * Test comprehensive filter with both forbidden and sensitive words
     * (forbidden words take precedence)
     * 
     * Validates: Requirement 11.1, 11.2
     */
    @Test
    public void testFilter_ForbiddenWordTakesPrecedence() {
        // Arrange
        String content = "这条评论包含违禁词和敏感词";
        
        // Act
        FilterResult result = contentFilter.filter(content);
        
        // Assert
        assertNotNull(result, "FilterResult should not be null");
        assertFalse(result.isPassed(), "Filter should not pass with forbidden word");
        assertEquals("违禁词", result.getForbiddenWord(), "Should detect forbidden word first");
        assertNull(result.getFilteredContent(), "Should not process sensitive words when forbidden word found");
    }

    /**
     * Test comprehensive filter with empty content
     * 
     * Validates: Requirement 11.1, 11.3
     */
    @Test
    public void testFilter_EmptyContent() {
        // Act
        FilterResult result = contentFilter.filter("");
        
        // Assert
        assertNotNull(result, "FilterResult should not be null");
        assertTrue(result.isPassed(), "Filter should pass with empty content");
        assertNull(result.getForbiddenWord(), "Forbidden word should be null");
        assertEquals("", result.getFilteredContent(), "Filtered content should be empty");
    }

    /**
     * Test comprehensive filter with null content
     * 
     * Validates: Requirement 11.1, 11.3
     */
    @Test
    public void testFilter_NullContent() {
        // Act
        FilterResult result = contentFilter.filter(null);
        
        // Assert
        assertNotNull(result, "FilterResult should not be null");
        assertTrue(result.isPassed(), "Filter should pass with null content");
        assertNull(result.getForbiddenWord(), "Forbidden word should be null");
        assertNull(result.getFilteredContent(), "Filtered content should be null");
    }

    @Test
    public void testFilter_UsesExplicitWordType() {
        // Arrange
        insertWord("明确拦截", "forbidden");
        insertWord("明确替换", "sensitive");

        // Act
        FilterResult blocked = contentFilter.filter("这条评论需要明确拦截");
        FilterResult masked = contentFilter.filter("这条评论需要明确替换");

        // Assert
        assertFalse(blocked.isPassed(), "Forbidden typed word should reject the content");
        assertEquals("明确拦截", blocked.getForbiddenWord(), "Should report the explicitly forbidden word");
        assertTrue(masked.isPassed(), "Sensitive typed word should pass after masking");
        assertEquals("这条评论需要****", masked.getFilteredContent(), "Sensitive typed word should be masked");
    }

    // ========== Helper Methods ==========

    private void insertWord(String word) {
        insertWord(word, null);
    }

    private void insertWord(String word, String type) {
        SensitiveWordDTO dto = new SensitiveWordDTO();
        dto.setWord(word);
        dto.setType(type);
        sensitiveWordMapper.insert(dto);
    }
}
