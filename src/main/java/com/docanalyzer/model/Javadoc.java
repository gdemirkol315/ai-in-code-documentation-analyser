package com.docanalyzer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Javadoc comment for a method.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Javadoc {
    
    /**
     * The main description of the method.
     */
    private String description;
    
    /**
     * Parameter documentation (name and description).
     */
    @Builder.Default
    private List<ParamTag> paramTags = new ArrayList<>();
    
    /**
     * Return value documentation.
     */
    private String returnTag;
    
    /**
     * Exception documentation (exception type and description).
     */
    @Builder.Default
    private List<ThrowsTag> throwsTags = new ArrayList<>();
    
    /**
     * Other tags like @see, @since, etc.
     */
    @Builder.Default
    private List<OtherTag> otherTags = new ArrayList<>();
    
    /**
     * The raw Javadoc text.
     */
    private String rawText;
    
    /**
     * Represents a parameter tag in Javadoc.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParamTag {
        private String parameterName;
        private String description;
    }
    
    /**
     * Represents a throws/exception tag in Javadoc.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThrowsTag {
        private String exceptionType;
        private String description;
    }
    
    /**
     * Represents any other Javadoc tag.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtherTag {
        private String tagName;
        private String content;
    }
}
