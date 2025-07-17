package com.docanalyzer.metrics;

import com.docanalyzer.model.Metric;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages metrics definitions for documentation evaluation.
 */
@Slf4j
public class MetricsManager {
    
    private final Map<String, Metric> metrics = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ExampleDefinition bestExample;
    private ExampleDefinition worstExample;
    
    /**
     * Inner class to represent example definitions.
     */
    public static class ExampleDefinition {
        private String description;
        private String code;
        private String documentation;
        
        // Getters and setters
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        
        public String getDocumentation() { return documentation; }
        public void setDocumentation(String documentation) { this.documentation = documentation; }
    }
    
    /**
     * Loads metrics definitions from a JSON file.
     * 
     * @param filePath The path to the metrics definitions file
     * @return True if the metrics were loaded successfully
     */
    public boolean loadMetricsFromFile(String filePath) {
        try {
            File file = new File(filePath);
            
            if (!file.exists()) {
                log.error("Metrics definitions file not found: {}", filePath);
                return false;
            }
            
            JsonNode rootNode = objectMapper.readTree(file);
            JsonNode metricsNode = rootNode.path("metrics");
            
            if (!metricsNode.isArray()) {
                log.error("Invalid metrics definitions file format: 'metrics' array not found");
                return false;
            }
            
            // Load metrics
            List<Metric> metricsList = objectMapper.readValue(
                    metricsNode.toString(),
                    new TypeReference<List<Metric>>() {}
            );
            
            for (Metric metric : metricsList) {
                metrics.put(metric.getName(), metric);
                log.info("Loaded metric: {}", metric.getName());
            }
            
            // Load best example
            JsonNode bestExampleNode = rootNode.path("best-example");
            if (!bestExampleNode.isMissingNode()) {
                bestExample = objectMapper.readValue(bestExampleNode.toString(), ExampleDefinition.class);
                log.info("Loaded best example");
            }
            
            // Load worst example
            JsonNode worstExampleNode = rootNode.path("worst-example");
            if (!worstExampleNode.isMissingNode()) {
                worstExample = objectMapper.readValue(worstExampleNode.toString(), ExampleDefinition.class);
                log.info("Loaded worst example");
            }
            
            log.info("Loaded {} metrics from {}", metrics.size(), filePath);
            return true;
            
        } catch (IOException e) {
            log.error("Error loading metrics definitions: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Gets a metric by name.
     * 
     * @param name The metric name
     * @return The metric, or null if not found
     */
    public Metric getMetric(String name) {
        return metrics.get(name);
    }
    
    /**
     * Gets all metrics.
     * 
     * @return A list of all metrics
     */
    public List<Metric> getAllMetrics() {
        return new ArrayList<>(metrics.values());
    }

    
    /**
     * Adds a metric.
     * 
     * @param metric The metric to add
     */
    public void addMetric(Metric metric) {
        metrics.put(metric.getName(), metric);
    }

    /**
     * Saves metrics definitions to a JSON file.
     * 
     * @param filePath The path to save the metrics definitions
     * @return True if the metrics were saved successfully
     */
    public boolean saveMetricsToFile(String filePath) {
        try {
            Map<String, List<Metric>> root = new HashMap<>();
            root.put("metrics", getAllMetrics());
            
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), root);
            
            log.info("Saved {} metrics to {}", metrics.size(), filePath);
            return true;
            
        } catch (IOException e) {
            log.error("Error saving metrics definitions: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Gets the best example.
     * 
     * @return The best example, or null if not loaded
     */
    public ExampleDefinition getBestExample() {
        return bestExample;
    }
    
    /**
     * Gets the worst example.
     * 
     * @return The worst example, or null if not loaded
     */
    public ExampleDefinition getWorstExample() {
        return worstExample;
    }
    
    /**
     * Gets the guidelines for all metrics as a formatted string.
     * 
     * @return The guidelines for all metrics
     */
    public String getFormattedGuidelines() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Please rate the documentation on the following metrics using provided scale below:\n\n");
        
        // Add reference examples if available
        if (bestExample != null && worstExample != null) {
            sb.append("REFERENCE EXAMPLES:\n\n");
            
            sb.append("BEST PRACTICE EXAMPLE (All metrics rated 5):\n");
            sb.append(bestExample.getDescription()).append("\n");
            sb.append("Method: ").append(bestExample.getCode()).append("\n");
            sb.append("Documentation:\n").append(bestExample.getDocumentation()).append("\n");
            sb.append("This example demonstrates:\n");
            sb.append("- Completeness: Covers all parameters, return value, exceptions, and provides comprehensive description\n");
            sb.append("- Comprehensibility: Clear, concise, and easy to understand language\n");
            sb.append("- Alignment: Documentation perfectly matches the method's actual behavior\n\n");
            
            sb.append("WORST PRACTICE EXAMPLE (All metrics rated 1):\n");
            sb.append(worstExample.getDescription()).append("\n");
            sb.append("Method: ").append(worstExample.getCode()).append("\n");
            sb.append("Documentation:\n").append(worstExample.getDocumentation()).append("\n");
            sb.append("This example demonstrates:\n");
            sb.append("- Completeness: Missing essential information about parameters, return value, and purpose\n");
            sb.append("- Comprehensibility: Vague and uninformative description\n");
            sb.append("- Alignment: Documentation doesn't properly describe what the method does\n\n");
            
            sb.append("Use these examples as reference points when evaluating documentation quality.\n\n");
        }
        
        for (Metric metric : getAllMetrics()) {
            sb.append(metric.getName()).append(" scores:\n");
            sb.append("Description: ").append(metric.getDescription()).append("\n");
            
            for (int i = 1; i <= 5; i++) {
                sb.append("  ").append(i).append(": ").append(metric.getGuideline(i)).append("\n");
            }
            
            sb.append("\n");
        }
        
        sb.append("For each metric, provide:\n");
        sb.append("1. A numerical rating\n");
        sb.append("2. A brief explanation justifying your rating\n");
        sb.append("3. The guideline text that corresponds to your rating\n");
        sb.append("4. Specific recommendations for improvement if the rating is less than the highest score\n");
        
        return sb.toString();
    }
}
