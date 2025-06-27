package com.docanalyzer.ai;

import com.docanalyzer.model.MetricsResult;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses responses from the Anthropic API.
 */
@Slf4j
public class ResponseParser {
    
    // Patterns for extracting information from the API response
    private static final Pattern METHOD_PATTERN = Pattern.compile("METHOD (\\d+) EVALUATION:", Pattern.CASE_INSENSITIVE);
    private static final Pattern METRIC_PATTERN = Pattern.compile("([\\w\\s]+):\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern JUSTIFICATION_PATTERN = Pattern.compile("Justification:\\s*(.+?)(?=\\n\\n|\\n[A-Z]|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern GUIDELINE_PATTERN = Pattern.compile("Guideline:\\s*(.+?)(?=\\n\\n|\\n[A-Z]|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern RECOMMENDATIONS_PATTERN = Pattern.compile("Recommendations:\\s*(.+?)(?=\\n\\n|\\n[A-Z]|$|---)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern RECOMMENDATION_ITEM_PATTERN = Pattern.compile("\\d+\\.\\s*(.+?)(?=\\n\\d+\\.|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    
    /**
     * Parses a batch response from the API.
     * 
     * @param response The API response
     * @param expectedMethodCount The expected number of methods in the response
     * @return A map of method indices to metrics results
     */
    public Map<Integer, MetricsResult> parseBatchResponse(String response, int expectedMethodCount) {
        Map<Integer, MetricsResult> results = new HashMap<>();
        
        try {
            // Split the response by method
            Matcher methodMatcher = METHOD_PATTERN.matcher(response);
            int lastEnd = 0;
            
            while (methodMatcher.find()) {
                int methodIndex = Integer.parseInt(methodMatcher.group(1));
                int start = methodMatcher.start();
                
                // If this is not the first method, process the previous method
                if (lastEnd > 0) {
                    String methodEvaluation = response.substring(lastEnd, start).trim();
                    results.put(methodIndex - 1, parseMethodEvaluation(methodEvaluation));
                }
                
                lastEnd = start;
            }
            
            // Process the last method
            if (lastEnd > 0) {
                String methodEvaluation = response.substring(lastEnd).trim();
                Matcher lastMethodMatcher = METHOD_PATTERN.matcher(methodEvaluation);
                if (lastMethodMatcher.find()) {
                    int methodIndex = Integer.parseInt(lastMethodMatcher.group(1));
                    results.put(methodIndex, parseMethodEvaluation(methodEvaluation.substring(lastMethodMatcher.end()).trim()));
                }
            }
            
            // Check if we got results for all expected methods
            if (results.size() != expectedMethodCount) {
                log.warn("Expected {} methods in response, but found {}", expectedMethodCount, results.size());
            }
            
            return results;
        } catch (Exception e) {
            log.error("Error parsing batch response: {}", e.getMessage());
            log.debug("Response: {}", response);
            return results;
        }
    }
    
    /**
     * Parses a method evaluation from the API response.
     * 
     * @param methodEvaluation The method evaluation text
     * @return The metrics result
     */
    private MetricsResult parseMethodEvaluation(String methodEvaluation) {
        MetricsResult result = new MetricsResult();
        
        try {
            // Extract metrics
            Matcher metricMatcher = METRIC_PATTERN.matcher(methodEvaluation);
            while (metricMatcher.find()) {
                String metricName = metricMatcher.group(1).trim();
                int score = Integer.parseInt(metricMatcher.group(2));
                
                // Find justification for this metric
                String justification = "";
                String metricSection = methodEvaluation.substring(metricMatcher.start());
                Matcher justificationMatcher = JUSTIFICATION_PATTERN.matcher(metricSection);
                if (justificationMatcher.find()) {
                    justification = justificationMatcher.group(1).trim();
                }
                
                // Find guideline for this metric
                String guideline = "";
                Matcher guidelineMatcher = GUIDELINE_PATTERN.matcher(metricSection);
                if (guidelineMatcher.find()) {
                    guideline = guidelineMatcher.group(1).trim();
                }
                
                result.addMetricResult(metricName, score, guideline, justification);
            }
            
            // Extract recommendations
            List<String> recommendations = new ArrayList<>();
            Matcher recommendationsMatcher = RECOMMENDATIONS_PATTERN.matcher(methodEvaluation);
            if (recommendationsMatcher.find()) {
                String recommendationsText = recommendationsMatcher.group(1).trim();
                Matcher itemMatcher = RECOMMENDATION_ITEM_PATTERN.matcher(recommendationsText);
                while (itemMatcher.find()) {
                    recommendations.add(itemMatcher.group(1).trim());
                }
            }
            
            for (String recommendation : recommendations) {
                result.addRecommendation(recommendation);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error parsing method evaluation: {}", e.getMessage());
            log.debug("Method evaluation: {}", methodEvaluation);
            return result;
        }
    }
    
    /**
     * Parses a single method response from the API.
     * 
     * @param response The API response
     * @return The metrics result
     */
    public MetricsResult parseSingleMethodResponse(String response) {
        Map<Integer, MetricsResult> results = parseBatchResponse(response, 1);
        return results.getOrDefault(1, new MetricsResult());
    }
}
