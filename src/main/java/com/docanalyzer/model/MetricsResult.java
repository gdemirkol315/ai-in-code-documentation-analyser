package com.docanalyzer.model;

import com.docanalyzer.metrics.MetricsValidator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the results of metrics evaluation for a method's documentation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsResult {
    
    /**
     * Map of metric names to their individual results.
     */
    @Builder.Default
    private Map<String, MetricResult> metricResults = new HashMap<>();
    
    /**
     * The overall score (average of all metrics).
     */
    private double overallScore;
    
    /**
     * List of recommendations for improving the documentation.
     */
    @Builder.Default
    private List<String> recommendations = new ArrayList<>();
    
    /**
     * Represents the result for a single metric.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricResult {
        /**
         * The name of the metric.
         */
        private String name;
        
        /**
         * The score (1-5 scale).
         */
        private int score;
        
        /**
         * The guideline text that corresponds to this score.
         */
        private String guideline;
        
        /**
         * Feedback explaining the score.
         */
        private String feedback;
    }
    
    /**
     * Adds a metric result to this metrics result.
     * 
     * @param name The name of the metric
     * @param score The score (1-5)
     * @param feedback The feedback explaining the score
     */
    public void addMetricResult(String name, int score, String feedback) {
        addMetricResult(name, score, feedback, null);
    }
    
    /**
     * Adds a metric result to this metrics result with validation.
     * 
     * @param name The name of the metric
     * @param score The score (1-5)
     * @param feedback The feedback explaining the score
     * @param validator The metrics validator to use for validation (optional)
     * @throws IllegalArgumentException if validation fails
     */
    public void addMetricResult(String name, int score, String feedback, MetricsValidator validator) {
        // Perform validation if validator is provided
        if (validator != null) {
            MetricsValidator.ValidationResult validationResult = validator.validateMetricResult(name, score);
            validationResult.throwIfFailed();
            
            // Get the guideline text for this score if available
            var metric = validator.getMetricDefinition(name);
            if (metric != null) {
                String guideline = metric.getGuideline(score);
                
                MetricResult result = MetricResult.builder()
                        .name(name)
                        .score(score)
                        .feedback(feedback)
                        .guideline(guideline)
                        .build();
                
                metricResults.put(name, result);
                recalculateOverallScore();
                return;
            }
        }
        
        // Fallback to original behavior without validation
        MetricResult result = MetricResult.builder()
                .name(name)
                .score(score)
                .feedback(feedback)
                .build();
        
        metricResults.put(name, result);
        recalculateOverallScore();
    }
    
    /**
     * Adds a recommendation for improving the documentation.
     * 
     * @param recommendation The recommendation text
     */
    public void addRecommendation(String recommendation) {
        recommendations.add(recommendation);
    }
    
    /**
     * Recalculates the overall score based on all metric results.
     */
    private void recalculateOverallScore() {
        if (metricResults.isEmpty()) {
            overallScore = 0.0;
            return;
        }
        
        double sum = metricResults.values().stream()
                .mapToInt(MetricResult::getScore)
                .sum();
        
        overallScore = sum / metricResults.size();
    }
}
