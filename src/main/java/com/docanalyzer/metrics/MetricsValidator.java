package com.docanalyzer.metrics;

import com.docanalyzer.config.Configuration;
import com.docanalyzer.model.Metric;
import lombok.extern.slf4j.Slf4j;

/**
 * Validates metrics results against the metrics definitions.
 */
@Slf4j
public class MetricsValidator {
    
    private final MetricsManager metricsManager;
    
    /**
     * Creates a new MetricsValidator with the given MetricsManager.
     * 
     * @param metricsManager The metrics manager containing the loaded metrics definitions
     */
    public MetricsValidator(MetricsManager metricsManager) {
        this.metricsManager = metricsManager;
    }
    
    /**
     * Creates a new MetricsValidator and loads metrics from the configuration.
     * 
     * @param configuration The configuration containing the metrics definitions path
     * @return A new MetricsValidator instance
     * @throws IllegalStateException if metrics cannot be loaded
     */
    public static MetricsValidator fromConfiguration(Configuration configuration) {
        MetricsManager manager = new MetricsManager();
        
        if (!manager.loadMetricsFromFile(configuration.getMetricsDefinitionsPath())) {
            throw new IllegalStateException("Failed to load metrics definitions from: " + 
                    configuration.getMetricsDefinitionsPath());
        }
        
        return new MetricsValidator(manager);
    }
    
    /**
     * Validates a metric result before it can be added.
     * 
     * @param metricName The name of the metric to validate
     * @param score The score to validate (should be 1-5)
     * @return A validation result containing success status and error message if applicable
     */
    public ValidationResult validateMetricResult(String metricName, int score) {
        // Check if metric name is null or empty
        if (metricName == null || metricName.trim().isEmpty()) {
            return ValidationResult.failure("Metric name cannot be null or empty");
        }
        
        // Check if the metric exists in the definitions
        Metric metric = metricsManager.getMetric(metricName);
        if (metric == null) {
            return ValidationResult.failure("Metric '" + metricName + "' is not defined in the metrics definitions file. " +
                    "Available metrics: " + String.join(", ", getAvailableMetricNames()));
        }
        
        // Check if the score is within valid range (1-5)
        if (score < 1 || score > 5) {
            return ValidationResult.failure("Score must be between 1 and 5, but was: " + score);
        }
        
        // Check if the metric has a guideline for this score
        String guideline = metric.getGuideline(score);
        if (guideline == null || guideline.startsWith("No guideline available")) {
            return ValidationResult.failure("No guideline available for metric '" + metricName + "' with score " + score);
        }
        
        log.debug("Validation successful for metric '{}' with score {}", metricName, score);
        return ValidationResult.success();
    }
    
    /**
     * Gets the names of all available metrics.
     * 
     * @return An array of available metric names
     */
    private String[] getAvailableMetricNames() {
        return metricsManager.getAllMetrics().stream()
                .map(Metric::getName)
                .toArray(String[]::new);
    }
    
    /**
     * Gets the metric definition for a given name.
     * 
     * @param metricName The name of the metric
     * @return The metric definition, or null if not found
     */
    public Metric getMetricDefinition(String metricName) {
        return metricsManager.getMetric(metricName);
    }
    
    /**
     * Represents the result of a validation operation.
     */
    public static class ValidationResult {
        private final boolean success;
        private final String errorMessage;
        
        private ValidationResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        /**
         * Creates a successful validation result.
         * 
         * @return A successful validation result
         */
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        /**
         * Creates a failed validation result with an error message.
         * 
         * @param errorMessage The error message
         * @return A failed validation result
         */
        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        /**
         * Checks if the validation was successful.
         * 
         * @return true if validation was successful, false otherwise
         */
        public boolean isSuccess() {
            return success;
        }
        
        /**
         * Gets the error message if validation failed.
         * 
         * @return The error message, or null if validation was successful
         */
        public String getErrorMessage() {
            return errorMessage;
        }
        
        /**
         * Throws an exception if validation failed.
         * 
         * @throws IllegalArgumentException if validation failed
         */
        public void throwIfFailed() {
            if (!success) {
                throw new IllegalArgumentException(errorMessage);
            }
        }
    }
}
