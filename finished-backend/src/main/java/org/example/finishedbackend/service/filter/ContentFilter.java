package org.example.finishedbackend.service.filter;

public interface ContentFilter {
    /**
     * 检测违禁词
     * @param content 待检测内容
     * @return 命中的违禁词，无则返回null
     */
    String checkForbiddenWords(String content);
    
    /**
     * 替换敏感词为星号
     * @param content 待处理内容
     * @return 替换后的内容
     */
    String replaceSensitiveWords(String content);
    
    /**
     * 综合过滤（先检测违禁词，再替换敏感词）
     * @param content 待过滤内容
     * @return 过滤结果
     */
    FilterResult filter(String content);
}
