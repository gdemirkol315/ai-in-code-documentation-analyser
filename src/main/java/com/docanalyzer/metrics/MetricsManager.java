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
            
            List<Metric> metricsList = objectMapper.readValue(
                    metricsNode.toString(),
                    new TypeReference<List<Metric>>() {}
            );
            
            for (Metric metric : metricsList) {
                metrics.put(metric.getName(), metric);
                log.info("Loaded metric: {}", metric.getName());
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
     * Creates a default set of metrics.
     * 
     * @return A list of default metrics
     */
    public static List<Metric> createDefaultMetrics() {
        List<Metric> defaultMetrics = new ArrayList<>();
        
        // Completeness metric
        Metric completeness = Metric.builder()
                .name("Completeness")
                .description("Measures how thoroughly the documentation covers all aspects of the method")
                .weight(1.0)
                .build();
        
        completeness.setGuideline(1, "Missing most essential information (parameters, return values, purpose)");
        completeness.setGuideline(2, "Covers basic purpose but missing details on parameters or returns");
        completeness.setGuideline(3, "Documents all parameters and returns but lacks exception handling or edge cases");
        completeness.setGuideline(4, "Comprehensive coverage with minor omissions");
        completeness.setGuideline(5, "Perfect documentation covering purpose, parameters, returns, exceptions, and edge cases");
        
        defaultMetrics.add(completeness);
        
        // Clarity metric
        Metric clarity = Metric.builder()
                .name("Clarity")
                .description("Evaluates how clear and understandable the documentation is")
                .weight(1.0)
                .build();
        
        clarity.setGuideline(1, "Confusing or misleading documentation");
        clarity.setGuideline(2, "Unclear wording with ambiguous descriptions");
        clarity.setGuideline(3, "Mostly clear but with some confusing elements");
        clarity.setGuideline(4, "Clear and concise with minor improvements possible");
        clarity.setGuideline(5, "Exceptionally clear, concise, and easy to understand");
        
        defaultMetrics.add(clarity);
        
        // Code-Documentation Alignment metric
        Metric alignment = Metric.builder()
                .name("Code Alignment")
                .description("Measures how well the documentation aligns with the actual code")
                .weight(1.0)
                .build();
        
        alignment.setGuideline(1, "Documentation contradicts or misrepresents the code");
        alignment.setGuideline(2, "Documentation partially aligns with code but has significant discrepancies");
        alignment.setGuideline(3, "Documentation mostly aligns with code but has minor discrepancies");
        alignment.setGuideline(4, "Documentation accurately reflects code with very minor omissions");
        alignment.setGuideline(5, "Documentation perfectly aligns with code, including edge cases and special conditions");
        
        defaultMetrics.add(alignment);
        
        return defaultMetrics;
    }
    
    /**
     * Creates a default metrics definitions file.
     * 
     * @param filePath The path to save the default metrics definitions
     * @return True if the default metrics were saved successfully
     */
    public boolean createDefaultMetricsFile(String filePath) {
        try {
            List<Metric> defaultMetrics = createDefaultMetrics();
            
            for (Metric metric : defaultMetrics) {
                addMetric(metric);
            }
            
            return saveMetricsToFile(filePath);
            
        } catch (Exception e) {
            log.error("Error creating default metrics file: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Gets the guidelines for all metrics as a formatted string.
     * 
     * @return The guidelines for all metrics
     */
    public String getFormattedGuidelines() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Please rate the documentation on the following metrics using a scale of 1-5:\n\n");
        
        for (Metric metric : getAllMetrics()) {
            sb.append(metric.getName()).append(" (1-5):\n");
            sb.append("Description: ").append(metric.getDescription()).append("\n");
            
            for (int i = 1; i <= 5; i++) {
                sb.append("  ").append(i).append(": ").append(metric.getGuideline(i)).append("\n");
            }
            
            sb.append("\n");
        }
        
        sb.append("For each metric, provide:\n");
        sb.append("1. A numerical rating (1-5)\n");
        sb.append("2. A brief explanation justifying your rating\n");
        sb.append("3. The guideline text that corresponds to your rating\n");
        sb.append("4. Specific recommendations for improvement if the rating is less than 5\n");
        
        return sb.toString();
    }
}
