package com.docanalyzer.ai;

import com.docanalyzer.model.Method;
import com.docanalyzer.util.TokenCounter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Generates prompts for the Anthropic API.
 */
@Slf4j
public class PromptGenerator {
    
    private final TokenCounter tokenCounter;
    
    /**
     * Creates a new PromptGenerator.
     */
    public PromptGenerator() {
        this.tokenCounter = new TokenCounter();
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
        
        // Add response format instructions
        promptBuilder.append("FORMAT YOUR RESPONSE AS:\n\n");
        
        for (int i = 0; i < methods.size(); i++) {
            promptBuilder.append("METHOD ").append(i + 1).append(" [method name] EVALUATION:\n");
            promptBuilder.append("[Metric 1 Name]: [rating 1-5]\n");
            promptBuilder.append("Justification: [explanation]\n");
            
            promptBuilder.append("[Metric 2 Name]: [rating 1-5]\n");
            promptBuilder.append("Justification: [explanation]\n");

            promptBuilder.append("... (for all metrics)\n\n");
            
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
    
    /**
     * Generates a prompt for a single method.
     * 
     * @param method The method
     * @param guidelines The evaluation guidelines
     * @return The generated prompt
     */
    public String generateSingleMethodPrompt(Method method, String guidelines) {
        return generateBatchPrompt(List.of(method), guidelines);
    }
}
