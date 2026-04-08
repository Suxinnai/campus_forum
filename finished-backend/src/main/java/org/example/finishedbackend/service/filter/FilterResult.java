package org.example.finishedbackend.service.filter;

import lombok.Data;

@Data
public class FilterResult {
    private boolean passed;
    private String forbiddenWord;
    private String filteredContent;
    
    public FilterResult(boolean passed, String forbiddenWord, String filteredContent) {
        this.passed = passed;
        this.forbiddenWord = forbiddenWord;
        this.filteredContent = filteredContent;
    }
}
