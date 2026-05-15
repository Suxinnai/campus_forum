package org.example.finishedbackend.service.filter;

import jakarta.annotation.Resource;
import org.example.finishedbackend.entity.DTO.SensitiveWordDTO;
import org.example.finishedbackend.mapper.SensitiveWordMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class ContentFilterImpl implements ContentFilter {
    
    @Resource
    SensitiveWordMapper sensitiveWordMapper;
    
    @Override
    public String checkForbiddenWords(String content) {
        if (content == null || content.isBlank()) return null;
        
        String lowerContent = content.toLowerCase(Locale.ROOT);
        
        for (SensitiveWordDTO sw : sensitiveWordMapper.selectList(null)) {
            if (isForbiddenWord(sw) && lowerContent.contains(sw.getWord().toLowerCase(Locale.ROOT))) {
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
            if (sw.getWord() != null && !sw.getWord().isBlank() && !isForbiddenWord(sw)) {
                String replacement = "*".repeat(sw.getWord().length());
                result = result.replaceAll("(?i)" + Pattern.quote(sw.getWord()), replacement);
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

    private boolean isForbiddenWord(SensitiveWordDTO sw) {
        if (sw == null) return false;
        String word = sw.getWord();
        if (word == null || word.isBlank()) return false;
        if ("forbidden".equalsIgnoreCase(sw.getType())) return true;
        String normalized = word.toLowerCase(Locale.ROOT);
        return normalized.contains("违禁") || normalized.contains("forbidden");
    }
}
