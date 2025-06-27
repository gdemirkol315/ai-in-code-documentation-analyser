package com.docanalyzer.util;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for estimating token counts for API requests.
 */
@Slf4j
public class TokenCounter {
    
    /**
     * Average number of characters per token (rough estimate).
     */
    private static final double CHARS_PER_TOKEN = 4.0;
    
    /**
     * Estimates the token count for a text string.
     * 
     * @param text The text to estimate tokens for
     * @return The estimated token count
     */
    public int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // This is a very rough estimate based on the average number of characters per token
        // For more accurate token counting, you would need to use a tokenizer like tiktoken
        int charCount = text.length();
        int estimatedTokens = (int) Math.ceil(charCount / CHARS_PER_TOKEN);
        
        return estimatedTokens;
    }
    
    /**
     * Estimates if a text will fit within a token limit.
     * 
     * @param text The text to check
     * @param tokenLimit The token limit
     * @return True if the text is estimated to fit within the token limit
     */
    public boolean willFitInTokenLimit(String text, int tokenLimit) {
        int estimatedTokens = estimateTokenCount(text);
        return estimatedTokens <= tokenLimit;
    }
    
    /**
     * Truncates text to fit within a token limit.
     * 
     * @param text The text to truncate
     * @param tokenLimit The token limit
     * @return The truncated text
     */
    public String truncateToTokenLimit(String text, int tokenLimit) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        if (willFitInTokenLimit(text, tokenLimit)) {
            return text;
        }
        
        // Estimate the character limit based on the token limit
        int charLimit = (int) (tokenLimit * CHARS_PER_TOKEN);
        
        // Truncate the text to the character limit
        if (charLimit < text.length()) {
            String truncated = text.substring(0, charLimit);
            log.warn("Text truncated from {} to {} characters to fit token limit", text.length(), truncated.length());
            return truncated;
        }
        
        return text;
    }
}
