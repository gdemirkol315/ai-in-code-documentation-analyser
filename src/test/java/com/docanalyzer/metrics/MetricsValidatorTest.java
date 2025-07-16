package com.docanalyzer.metrics;

import com.docanalyzer.config.Configuration;
import com.docanalyzer.model.Metric;
import com.docanalyzer.model.MetricsResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the MetricsValidator class.
 */
class MetricsValidatorTest {
    
    @TempDir
    Path tempDir;
    
    private MetricsValidator validator;
    private Path metricsFile;
    
    @BeforeEach
    void setUp() throws IOException {
        // Create a test metrics definitions file
        metricsFile = tempDir.resolve("test-metrics.json");
        String metricsJson = """
            {
              "metrics": [
                {
                  "name": "Completeness",
                  "description": "Measures how thoroughly the documentation covers all aspects of the method",
                  "guidelines": {
                    "1": "Missing most essential information",
                    "2": "Covers basic description but missing details",
                    "3": "Documents all parameters and returns but lacks exception handling",
                    "4": "Comprehensive coverage with minor possible improvement",
                    "5": "Perfect documentation covering all aspects"
                  }
                },
                {
                  "name": "Clarity",
                  "description": "Evaluates how clear and understandable the documentation is",
                  "guidelines": {
                    "1": "Confusing or misleading documentation",
                    "2": "Unclear wording with ambiguous descriptions",
                    "3": "Mostly clear but with some confusing elements",
                    "4": "Clear and concise with minor improvements possible",
                    "5": "Exceptionally clear, concise, and easy to understand"
                  }
                }
              ]
            }
            """;
        
        Files.writeString(metricsFile, metricsJson);
        
        // Create configuration and validator
        Configuration config = Configuration.builder()
                .metricsDefinitionsPath(metricsFile.toString())
                .build();
        
        validator = MetricsValidator.fromConfiguration(config);
    }
    
    @Test
    void testValidMetricResult() {
        MetricsValidator.ValidationResult result = validator.validateMetricResult("Completeness", 3);
        
        assertTrue(result.isSuccess());
        assertNull(result.getErrorMessage());
    }
    
    @Test
    void testInvalidMetricName() {
        MetricsValidator.ValidationResult result = validator.validateMetricResult("NonExistentMetric", 3);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("NonExistentMetric"));
        assertTrue(result.getErrorMessage().contains("Available metrics"));
    }
    
    @Test
    void testNullMetricName() {
        MetricsValidator.ValidationResult result = validator.validateMetricResult(null, 3);
        
        assertFalse(result.isSuccess());
        assertEquals("Metric name cannot be null or empty", result.getErrorMessage());
    }
    
    @Test
    void testEmptyMetricName() {
        MetricsValidator.ValidationResult result = validator.validateMetricResult("", 3);
        
        assertFalse(result.isSuccess());
        assertEquals("Metric name cannot be null or empty", result.getErrorMessage());
    }
    
    @Test
    void testScoreTooLow() {
        MetricsValidator.ValidationResult result = validator.validateMetricResult("Completeness", 0);
        
        assertFalse(result.isSuccess());
        assertEquals("Score must be between 1 and 5, but was: 0", result.getErrorMessage());
    }
    
    @Test
    void testScoreTooHigh() {
        MetricsValidator.ValidationResult result = validator.validateMetricResult("Completeness", 6);
        
        assertFalse(result.isSuccess());
        assertEquals("Score must be between 1 and 5, but was: 6", result.getErrorMessage());
    }
    
    @Test
    void testValidationResultThrowIfFailed() {
        MetricsValidator.ValidationResult failedResult = validator.validateMetricResult("InvalidMetric", 3);
        
        assertThrows(IllegalArgumentException.class, failedResult::throwIfFailed);
    }
    
    @Test
    void testValidationResultThrowIfFailedSuccess() {
        MetricsValidator.ValidationResult successResult = validator.validateMetricResult("Completeness", 3);
        
        assertDoesNotThrow(successResult::throwIfFailed);
    }
    
    @Test
    void testGetMetricDefinition() {
        Metric metric = validator.getMetricDefinition("Completeness");
        
        assertNotNull(metric);
        assertEquals("Completeness", metric.getName());
        assertEquals("Measures how thoroughly the documentation covers all aspects of the method", metric.getDescription());
    }
    
    @Test
    void testGetMetricDefinitionNotFound() {
        Metric metric = validator.getMetricDefinition("NonExistentMetric");
        
        assertNull(metric);
    }
    
    @Test
    void testMetricsResultWithValidation() {
        MetricsResult metricsResult = new MetricsResult();
        
        // This should work without throwing an exception
        assertDoesNotThrow(() -> {
            metricsResult.addMetricResult("Completeness", 4, "Good documentation", validator);
        });
        
        // Verify the result was added with guideline
        MetricsResult.MetricResult result = metricsResult.getMetricResults().get("Completeness");
        assertNotNull(result);
        assertEquals("Completeness", result.getName());
        assertEquals(4, result.getScore());
        assertEquals("Good documentation", result.getFeedback());
        assertEquals("Comprehensive coverage with minor possible improvement", result.getGuideline());
    }
    
    @Test
    void testMetricsResultWithValidationFailure() {
        MetricsResult metricsResult = new MetricsResult();
        
        // This should throw an exception due to invalid metric name
        assertThrows(IllegalArgumentException.class, () -> {
            metricsResult.addMetricResult("InvalidMetric", 4, "Good documentation", validator);
        });
        
        // Verify no result was added
        assertTrue(metricsResult.getMetricResults().isEmpty());
    }
    
    @Test
    void testMetricsResultWithoutValidation() {
        MetricsResult metricsResult = new MetricsResult();
        
        // This should work even with invalid metric name when no validator is provided
        assertDoesNotThrow(() -> {
            metricsResult.addMetricResult("InvalidMetric", 4, "Good documentation");
        });
        
        // Verify the result was added without guideline
        MetricsResult.MetricResult result = metricsResult.getMetricResults().get("InvalidMetric");
        assertNotNull(result);
        assertEquals("InvalidMetric", result.getName());
        assertEquals(4, result.getScore());
        assertEquals("Good documentation", result.getFeedback());
        assertNull(result.getGuideline());
    }
}
