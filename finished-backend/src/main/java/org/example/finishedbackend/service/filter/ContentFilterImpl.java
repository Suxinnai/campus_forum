package org.example.finishedbackend.service.filter;

import jakarta.annotation.Resource;
import org.example.finishedbackend.entity.DTO.SensitiveWordDTO;
import org.example.finishedbackend.mapper.SensitiveWordMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContentFilterImpl implements ContentFilter {
    
    @Resource
    SensitiveWordMapper sensitiveWordMapper;
    
    @Override
    public String checkForbiddenWords(String content) {
        if (content == null || content.isBlank()) return null;
        
        List<SensitiveWordDTO> words = sensitiveWordMapper.selectList(null);
        String lowerContent = content.toLowerCase();
        
        for (SensitiveWordDTO sw : words) {
            if (sw.getWord() != null && lowerContent.contains(sw.getWord().toLowerCase())) {
                return sw.getWord();
            }
        }
        return null;
    }
    
    @Override
    public String replaceSensitiveWords(String content) {
        if (content == null || content.isBlank()) return content;
        
        List<SensitiveWordDTO> words = sensitiveWordMapper.selectList(null);
        String result = content;
        
        for (SensitiveWordDTO sw : words) {
            if (sw.getWord() != null && !sw.getWord().isBlank()) {
                String replacement = "*".repeat(sw.getWord().length());
                result = result.replaceAll("(?i)" + sw.getWord(), replacement);
            }
        }
        return result;
    }
    
    @Override
    public FilterResult filter(String content) {
        // 先检测违禁词
        String forbiddenWord = checkForbiddenWords(content);
        if (forbiddenWord != null) {
            return new FilterResult(false, forbiddenWord, null);
        }
        
        // 再替换敏感词
        String filteredContent = replaceSensitiveWords(content);
        return new FilterResult(true, null, filteredContent);
    }
}
