package com.docanalyzer.ai;

import com.docanalyzer.metrics.MetricsValidator;
import com.docanalyzer.model.MetricsResult;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses responses from the Anthropic API.k
 */
@Slf4j
public class ResponseParser {
    
    private final MetricsValidator metricsValidator;
    
    // Patterns for extracting information from the API response
    private static final Pattern METHOD_PATTERN = Pattern.compile("METHOD (\\d+) [^\\n]+ EVALUATION:", Pattern.CASE_INSENSITIVE);
    private static final Pattern METRIC_PATTERN = Pattern.compile("(?:^|\\n)\\s*([A-Za-z][A-Za-z ]+?):\\s*(\\d+)(?:\\s|$)", Pattern.MULTILINE);
    private static final Pattern JUSTIFICATION_PATTERN = Pattern.compile("Justification:\\s*(.+?)(?=\\n\\n|\\n[A-Z]|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern RECOMMENDATIONS_PATTERN = Pattern.compile("Recommendations:\\s*(.+?)(?=\\n\\n|\\n[A-Z]|$|---)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern RECOMMENDATION_ITEM_PATTERN = Pattern.compile("\\d+\\.\\s*(.+?)(?=\\n\\d+\\.|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    
    /**
     * Creates a new ResponseParser with the given MetricsValidator.
     * 
     * @param metricsValidator The validator to use for validating parsed metrics
     */
    public ResponseParser(MetricsValidator metricsValidator) {
        this.metricsValidator = metricsValidator;
    }
    
    /**
     * Creates a new ResponseParser without validation (for backward compatibility).
     */
    public ResponseParser() {
        this.metricsValidator = null;
    }
    
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
            // Find all method headers first
            Matcher methodMatcher = METHOD_PATTERN.matcher(response);
            List<Integer> methodIndices = new ArrayList<>();
            List<Integer> methodStarts = new ArrayList<>();
            
            while (methodMatcher.find()) {
                int methodIndex = Integer.parseInt(methodMatcher.group(1));
                methodIndices.add(methodIndex);
                methodStarts.add(methodMatcher.end()); // Start after the header
            }
            
            // Process each method
            for (int i = 0; i < methodIndices.size(); i++) {
                int methodIndex = methodIndices.get(i);
                int start = methodStarts.get(i);
                int end = (i + 1 < methodStarts.size()) ? 
                    response.lastIndexOf("---", methodStarts.get(i + 1)) : response.length();
                
                // If we couldn't find the separator, use the start of next method
                if (end == -1 || end <= start) {
                    end = (i + 1 < methodStarts.size()) ? methodStarts.get(i + 1) : response.length();
                }
                
                String methodEvaluation = response.substring(start, end).trim();
                // Remove the trailing separator if present
                if (methodEvaluation.endsWith("---")) {
                    methodEvaluation = methodEvaluation.substring(0, methodEvaluation.length() - 3).trim();
                }
                
                results.put(methodIndex, parseMethodEvaluation(methodEvaluation));
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
            //log.debug("Parsing method evaluation: {}", methodEvaluation);
            
            // Extract metrics
            Matcher metricMatcher = METRIC_PATTERN.matcher(methodEvaluation);
            int metricCount = 0;
            while (metricMatcher.find()) {
                String metricName = metricMatcher.group(1).trim();
                int score = Integer.parseInt(metricMatcher.group(2));
                metricCount++;
                
                log.debug("Found metric #{}: '{}' with score {}", metricCount, metricName, score);
                
                // Find justification for this metric
                String justification = "";
                String metricSection = methodEvaluation.substring(metricMatcher.start());
                Matcher justificationMatcher = JUSTIFICATION_PATTERN.matcher(metricSection);
                if (justificationMatcher.find()) {
                    justification = justificationMatcher.group(1).trim();
                    log.debug("Found justification for '{}': {}", metricName, justification);
                }

                // Use validation if validator is available
                if (metricsValidator != null) {
                    try {
                        result.addMetricResult(metricName, score, justification, metricsValidator);
                        log.debug("Successfully validated and added metric '{}' with score {}", metricName, score);
                    } catch (IllegalArgumentException e) {
                        log.warn("Metric validation failed for '{}' with score {}: {}", metricName, score, e.getMessage());
                        // Still add the metric without validation for robustness
                        result.addMetricResult(metricName, score, justification);
                    }
                } else {
                    log.warn("Metric validation is not available!!");
                    result.addMetricResult(metricName, score, justification);
                }
            }
            
            log.debug("Total metrics found: {}", metricCount);
            
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
