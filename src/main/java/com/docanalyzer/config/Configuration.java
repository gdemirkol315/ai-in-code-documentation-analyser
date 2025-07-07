package com.docanalyzer.config;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuration settings for the documentation analyzer.
 */
@Data
@Builder
@Slf4j
public class Configuration {
    
    /**
     * The Anthropic API key.
     */
    private String anthropicApiKey;
    
    /**
     * The Anthropic model name to use.
     */
    private String modelName;
    
    /**
     * The maximum number of tokens to generate in the response.
     */
    private int maxTokens;
    
    /**
     * The maximum number of tokens per API request.
     */
    private int maxTokensPerRequest;
    
    /**
     * The default batch size for processing methods.
     */
    private int batchSize;
    
    /**
     * The path to the metrics definitions file.
     */
    private String metricsDefinitionsPath;
    
    /**
     * The path to save the output report.
     */
    private String outputPath;
    
    /**
     * The temperature for the API requests (0.0 to 1.0).
     */
    private double temperature;
    
    /**
     * Loads configuration from a properties file.
     * 
     * @param configPath The path to the properties file
     * @return The loaded configuration
     */
    public static Configuration loadFromFile(String configPath) {
        Properties properties = new Properties();
        
        try (InputStream input = new FileInputStream(configPath)) {
            properties.load(input);
            
            return Configuration.builder()
                    .anthropicApiKey(getProperty(properties, "anthropic.api.key", ""))
                    .modelName(getProperty(properties, "anthropic.model", "claude-3-opus-20240229"))
                    .maxTokens(getIntProperty(properties, "anthropic.max.tokens", 4096))
                    .maxTokensPerRequest(getIntProperty(properties, "anthropic.max.tokens.per.request", 100000))
                    .batchSize(getIntProperty(properties, "batch.size", 5))
                    .metricsDefinitionsPath(getProperty(properties, "metrics.definitions.path", "src/main/resources/metrics-definitions.json"))
                    .outputPath(getProperty(properties, "output.path", "output"))
                    .temperature(getDoubleProperty(properties, "anthropic.temperature", 0.0))
                    .build();
            
        } catch (IOException e) {
            log.error("Error loading configuration from {}: {}", configPath, e.getMessage());
            return getDefaultConfiguration();
        }
    }
    
    /**
     * Creates a default configuration.
     * 
     * @return The default configuration
     */
    public static Configuration getDefaultConfiguration() {
        return Configuration.builder()
                .anthropicApiKey(System.getenv("ANTHROPIC_API_KEY"))
                .modelName("claude-3-opus-20240229")
                .maxTokens(4096)
                .maxTokensPerRequest(100000)
                .batchSize(5)
                .metricsDefinitionsPath("src/main/resources/metrics-definitions.json")
                .outputPath("output")
                .temperature(0.0)
                .build();
    }
    
    /**
     * Ensures the output directory exists.
     */
    public void ensureOutputDirectoryExists() {
        Path outputDir = Paths.get(outputPath);
        
        if (!Files.exists(outputDir)) {
            try {
                Files.createDirectories(outputDir);
                log.info("Created output directory: {}", outputPath);
            } catch (IOException e) {
                log.error("Error creating output directory {}: {}", outputPath, e.getMessage());
            }
        }
    }
    
    /**
     * Gets a property from the properties object with a default value.
     * Supports environment variable substitution using ${VAR_NAME} syntax.
     * 
     * @param properties The properties object
     * @param key The property key
     * @param defaultValue The default value
     * @return The property value or default value
     */
    private static String getProperty(Properties properties, String key, String defaultValue) {
        String value = properties.getProperty(key, defaultValue);
        return substituteEnvironmentVariables(value);
    }
    
    /**
     * Substitutes environment variables in the format ${VAR_NAME} with their actual values.
     * 
     * @param value The string that may contain environment variable references
     * @return The string with environment variables substituted
     */
    private static String substituteEnvironmentVariables(String value) {
        if (value == null) {
            return null;
        }
        
        // Pattern to match ${VAR_NAME}
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}");
        java.util.regex.Matcher matcher = pattern.matcher(value);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String envVarName = matcher.group(1);
            String envVarValue = System.getenv(envVarName);
            
            if (envVarValue != null) {
                matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(envVarValue));
                log.debug("Substituted environment variable {} in configuration", envVarName);
            } else {
                log.warn("Environment variable {} not found, keeping original value", envVarName);
                matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Gets an integer property from the properties object with a default value.
     * 
     * @param properties The properties object
     * @param key The property key
     * @param defaultValue The default value
     * @return The property value as an integer or default value
     */
    private static int getIntProperty(Properties properties, String key, int defaultValue) {
        String value = properties.getProperty(key);
        
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid integer value for {}: {}. Using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Gets a double property from the properties object with a default value.
     * 
     * @param properties The properties object
     * @param key The property key
     * @param defaultValue The default value
     * @return The property value as a double or default value
     */
    private static double getDoubleProperty(Properties properties, String key, double defaultValue) {
        String value = properties.getProperty(key);
        
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid double value for {}: {}. Using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }
}
