package com.docanalyzer.ai;

import com.docanalyzer.metrics.MetricsManager;
import com.docanalyzer.model.Method;
import com.docanalyzer.model.Metric;
import com.docanalyzer.util.TokenCounter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Generates prompts for the Anthropic API.
 */
@Slf4j
public class PromptGenerator {
    
    private final MetricsManager metricsManager;
    
    /**
     * Creates a new PromptGenerator with the specified MetricsManager.
     * 
     * @param metricsManager The metrics manager to use for dynamic metric generation
     */
    public PromptGenerator(MetricsManager metricsManager) {
        this.metricsManager = metricsManager;
    }

    /**
     * Generates a prompt for a batch of methods.
     * 
     * @param methods The methods to include in the prompt
     * @param guidelines The evaluation guidelines
     * @return The generated prompt
     */
    public String generateBatchPrompt(List<Method> methods, String guidelines) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // Add introduction
        promptBuilder.append("You are a documentation quality evaluator. Your task is to evaluate the quality of Javadoc documentation for Java methods.\n\n");
        
        // Add guidelines
        promptBuilder.append("EVALUATION GUIDELINES:\n");
        promptBuilder.append(guidelines);
        promptBuilder.append("\n\n");
        
        // Add methods
        promptBuilder.append("METHODS TO EVALUATE:\n\n");
        
        for (int i = 0; i < methods.size(); i++) {
            Method method = methods.get(i);
            promptBuilder.append("METHOD ").append(i + 1).append(":\n");
            appendMethodDetails(promptBuilder, method);
            promptBuilder.append("\n\n");
        }
        
        // Add formatting requirements
        promptBuilder.append("IMPORTANT FORMATTING REQUIREMENTS:\n");
        promptBuilder.append("- Use PLAIN TEXT only - NO markdown formatting (no **, *, etc.)\n");
        promptBuilder.append("- Format metrics exactly as: 'MetricName: Score' (e.g., 'Comprehensibility: 3')\n");
        promptBuilder.append("- Do NOT bold, italicize, or emphasize metric names\n");
        promptBuilder.append("- Use consistent formatting throughout your response\n");
        promptBuilder.append("- Each metric should be on its own line followed by justification\n\n");
        
        // Add response format instructions
        promptBuilder.append("FORMAT YOUR RESPONSE AS:\n\n");
        
        for (int i = 0; i < methods.size(); i++) {
            promptBuilder.append("METHOD ").append(i + 1).append(" [method name] EVALUATION:\n");
            
            // Dynamically generate metric format based on loaded metrics
            for (Metric metric : metricsManager.getAllMetrics()) {
                promptBuilder.append(metric.getName()).append(": [rating score]\n");
                promptBuilder.append("Justification: [explanation]\n\n");
            }
            
            promptBuilder.append("Overall Assessment: [brief summary]\n\n");
            promptBuilder.append("Recommendations:\n");
            promptBuilder.append("1. [recommendation 1]\n");
            promptBuilder.append("2. [recommendation 2]\n");
            promptBuilder.append("... (if any)\n\n");

            if (i < methods.size() - 1) {
                promptBuilder.append("---\n\n");
            }
        }
        
        String prompt = promptBuilder.toString();
        log.debug("Generated prompt with {} methods, {} characters", methods.size(), prompt.length());
        
        return prompt;
    }
    
    /**
     * Appends method details to the prompt.
     * 
     * @param promptBuilder The prompt builder
     * @param method The method
     */
    private void appendMethodDetails(StringBuilder promptBuilder, Method method) {
        // Add method signature
        promptBuilder.append("```java\n");
        promptBuilder.append(method.getSignature());
        promptBuilder.append("\n```\n\n");
        
        // Add method body
        promptBuilder.append("METHOD BODY:\n");
        promptBuilder.append("```java\n");
        promptBuilder.append(method.getBody());
        promptBuilder.append("\n```\n\n");
        
        // Add Javadoc if available
        if (method.getJavadoc() != null) {
            promptBuilder.append("JAVADOC:\n");
            promptBuilder.append("```java\n");
            promptBuilder.append(method.getJavadoc().getRawText());
            promptBuilder.append("\n```\n");
        } else {
            promptBuilder.append("JAVADOC: None\n");
        }
        
        // Add context information
        promptBuilder.append("\nCONTEXT:\n");
        promptBuilder.append("Class: ").append(method.getClassName()).append("\n");
        promptBuilder.append("Package: ").append(method.getPackageName()).append("\n");
        
        // Add parameter information
        if (!method.getParameterNames().isEmpty()) {
            promptBuilder.append("Parameters:\n");
            for (int i = 0; i < method.getParameterNames().size(); i++) {
                promptBuilder.append("- ").append(method.getParameterTypes().get(i))
                        .append(" ").append(method.getParameterNames().get(i)).append("\n");
            }
        }
        
        // Add return type
        if (!"void".equals(method.getReturnType())) {
            promptBuilder.append("Return Type: ").append(method.getReturnType()).append("\n");
        }
    }

}
