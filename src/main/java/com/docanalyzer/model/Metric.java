package com.docanalyzer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a documentation metric with its definition and guidelines.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Metric {
    
    /**
     * The name of the metric.
     */
    private String name;
    
    /**
     * Description of what this metric measures.
     */
    private String description;
    
    /**
     * Guidelines for each score level (1-5).
     */
    @Builder.Default
    private Map<Integer, String> guidelines = new HashMap<>();
    
    /**
     * Weight of this metric in the overall score calculation.
     */
    private double weight;
    
    /**
     * Sets the guideline for a specific score level.
     * 
     * @param score The score level (1-5)
     * @param guideline The guideline text for this score
     */
    public void setGuideline(int score, String guideline) {
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("Score must be between 1 and 5");
        }
        guidelines.put(score, guideline);
    }
    
    /**
     * Gets the guideline for a specific score level.
     * 
     * @param score The score level (1-5)
     * @return The guideline text for this score
     */
    public String getGuideline(int score) {
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("Score must be between 1 and 5");
        }
        return guidelines.getOrDefault(score, "No guideline available for score " + score);
    }
    
    /**
     * Creates a builder with default values.
     * 
     * @return A builder with default values
     */
    public static MetricBuilder defaultBuilder() {
        return builder()
                .weight(1.0)
                .guidelines(new HashMap<>());
    }
}
