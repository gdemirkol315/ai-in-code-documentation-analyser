package com.docanalyzer.parser;

import com.docanalyzer.model.Javadoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses Javadoc comments and extracts structured information.
 */
public class JavadocParser {
    
    private static final Logger log = LoggerFactory.getLogger(JavadocParser.class);
    
    /**
     * Parses a Javadoc comment string and extracts structured information.
     * 
     * @param javadocComment The Javadoc comment string
     * @param parameterNames The list of parameter names for the method
     * @return A structured Javadoc object
     */
    public Javadoc parseJavadocComment(String javadocComment, List<String> parameterNames) {
        if (javadocComment == null || javadocComment.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Clean up the comment
            String cleanedComment = cleanJavadocComment(javadocComment);
            
            // Split the comment into description and tags
            String[] parts = cleanedComment.split("(?=@)");
            
            // The first part is the description
            String description = parts[0].trim();
            
            // Create the builder and set the raw text and description
            Javadoc.JavadocBuilder builder = Javadoc.builder()
                    .rawText(javadocComment)
                    .description(description);
            
            // Process tags
            List<Javadoc.ParamTag> paramTags = new ArrayList<>();
            List<Javadoc.ThrowsTag> throwsTags = new ArrayList<>();
            List<Javadoc.OtherTag> otherTags = new ArrayList<>();
            
            for (int i = 1; i < parts.length; i++) {
                String tagPart = parts[i].trim();
                
                if (tagPart.startsWith("@param ")) {
                    // Parse @param tag
                    String[] paramParts = tagPart.substring(7).trim().split("\\s+", 2);
                    if (paramParts.length >= 2) {
                        String paramName = paramParts[0];
                        String paramDesc = paramParts[1];
                        paramTags.add(new Javadoc.ParamTag(paramName, paramDesc));
                    }
                } else if (tagPart.startsWith("@return ")) {
                    // Parse @return tag
                    String returnDesc = tagPart.substring(8).trim();
                    builder.returnTag(returnDesc);
                } else if (tagPart.startsWith("@throws ") || tagPart.startsWith("@exception ")) {
                    // Parse @throws or @exception tag
                    int prefixLength = tagPart.startsWith("@throws ") ? 8 : 11;
                    String[] throwsParts = tagPart.substring(prefixLength).trim().split("\\s+", 2);
                    if (throwsParts.length >= 2) {
                        String exceptionType = throwsParts[0];
                        String exceptionDesc = throwsParts[1];
                        throwsTags.add(new Javadoc.ThrowsTag(exceptionType, exceptionDesc));
                    }
                } else {
                    // Parse other tags
                    int spaceIndex = tagPart.indexOf(' ');
                    if (spaceIndex > 0) {
                        String tagName = tagPart.substring(1, spaceIndex);
                        String content = tagPart.substring(spaceIndex + 1).trim();
                        otherTags.add(new Javadoc.OtherTag(tagName, content));
                    }
                }
            }
            
            builder.paramTags(paramTags);
            builder.throwsTags(throwsTags);
            builder.otherTags(otherTags);
            
            // Check for missing parameter documentation
            for (String paramName : parameterNames) {
                boolean documented = paramTags.stream()
                        .anyMatch(tag -> tag.getParameterName().equals(paramName));
                
                if (!documented) {
                    log.debug("Parameter '{}' is not documented in Javadoc", paramName);
                }
            }
            
            return builder.build();
        } catch (Exception e) {
            log.error("Error parsing Javadoc: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Cleans up a Javadoc comment by removing * at the beginning of lines and normalizing whitespace.
     * 
     * @param comment The raw Javadoc comment
     * @return The cleaned comment
     */
    private String cleanJavadocComment(String comment) {
        // Remove leading and trailing /**/ and normalize line endings
        String cleaned = comment.replaceAll("^\\s*/\\*\\*", "")
                .replaceAll("\\*/\\s*$", "")
                .replaceAll("\\r\\n", "\n");
        
        // Remove * at the beginning of lines
        cleaned = cleaned.replaceAll("(?m)^\\s*\\*\\s?", "");
        
        // Normalize whitespace
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        return cleaned;
    }
}
