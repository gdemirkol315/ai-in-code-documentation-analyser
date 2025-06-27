package com.docanalyzer.batch;

import com.docanalyzer.ai.AnthropicClient;
import com.docanalyzer.ai.PromptGenerator;
import com.docanalyzer.ai.ResponseParser;
import com.docanalyzer.config.Configuration;
import com.docanalyzer.model.Method;
import com.docanalyzer.model.MetricsResult;
import com.docanalyzer.util.TokenCounter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Processes methods in batches for efficient API usage.
 */
@Slf4j
public class BatchProcessor {
    
    private final int defaultBatchSize;
    private final int maxTokensPerRequest;
    private final TokenCounter tokenCounter;
    private final AnthropicClient anthropicClient;
    private final PromptGenerator promptGenerator;
    private final ResponseParser responseParser;
    
    /**
     * Creates a new BatchProcessor with the specified configuration.
     * 
     * @param config The configuration
     */
    public BatchProcessor(Configuration config) {
        this.defaultBatchSize = config.getBatchSize();
        this.maxTokensPerRequest = config.getMaxTokensPerRequest();
        this.tokenCounter = new TokenCounter();
        this.anthropicClient = new AnthropicClient(config);
        this.promptGenerator = new PromptGenerator();
        this.responseParser = new ResponseParser();
    }
    
    /**
     * Processes a list of methods in batches.
     * 
     * @param methods The methods to process
     * @param guidelines The evaluation guidelines
     */
    public void processBatches(List<Method> methods, String guidelines) {
        List<List<Method>> batches = createBatches(methods, guidelines);
        AtomicInteger processedCount = new AtomicInteger(0);
        int totalMethods = methods.size();
        
        log.info("Processing {} methods in {} batches", totalMethods, batches.size());
        
        for (List<Method> batch : batches) {
            try {
                String prompt = promptGenerator.generateBatchPrompt(batch, guidelines);
                String response = anthropicClient.sendRequest(prompt);
                Map<Integer, MetricsResult> results = responseParser.parseBatchResponse(response, batch.size());
                
                // Assign results to methods
                for (int i = 0; i < batch.size(); i++) {
                    Method method = batch.get(i);
                    MetricsResult result = results.get(i + 1);
                    
                    if (result != null) {
                        method.setMetricsResult(result);
                    } else {
                        log.error("No result found for method {} in batch", method.getName());
                    }
                }
                
                int processed = processedCount.addAndGet(batch.size());
                log.info("Processed {}/{} methods ({:.1f}%)", processed, totalMethods, 
                        (double) processed / totalMethods * 100);
                
            } catch (Exception e) {
                log.error("Error processing batch: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * Creates batches of methods based on token limits and batch size.
     * 
     * @param methods The methods to batch
     * @param guidelines The evaluation guidelines
     * @return A list of method batches
     */
    private List<List<Method>> createBatches(List<Method> methods, String guidelines) {
        List<List<Method>> batches = new ArrayList<>();
        List<Method> currentBatch = new ArrayList<>();
        int guidelinesTokens = tokenCounter.estimateTokenCount(guidelines);
        int currentBatchTokens = guidelinesTokens;
        int maxMethodTokens = maxTokensPerRequest - guidelinesTokens - 500; // 500 tokens buffer
        
        for (Method method : methods) {
            int methodTokens = estimateMethodTokens(method);
            
            // If adding this method would exceed the token limit or batch size, start a new batch
            if ((currentBatchTokens + methodTokens > maxTokensPerRequest) || 
                (currentBatch.size() >= defaultBatchSize)) {
                
                if (!currentBatch.isEmpty()) {
                    batches.add(new ArrayList<>(currentBatch));
                    currentBatch.clear();
                    currentBatchTokens = guidelinesTokens;
                }
                
                // If a single method is too large, process it individually with truncation
                if (methodTokens > maxMethodTokens) {
                    log.warn("Method {} exceeds token limit ({} tokens). It will be processed individually with possible truncation.", 
                            method.getName(), methodTokens);
                    List<Method> singleMethodBatch = new ArrayList<>();
                    singleMethodBatch.add(method);
                    batches.add(singleMethodBatch);
                    continue;
                }
            }
            
            currentBatch.add(method);
            currentBatchTokens += methodTokens;
        }
        
        // Add the last batch if not empty
        if (!currentBatch.isEmpty()) {
            batches.add(currentBatch);
        }
        
        return batches;
    }
    
    /**
     * Estimates the token count for a method.
     * 
     * @param method The method
     * @return The estimated token count
     */
    private int estimateMethodTokens(Method method) {
        StringBuilder methodText = new StringBuilder();
        
        methodText.append("METHOD: ").append(method.getName()).append("\n");
        methodText.append("SIGNATURE: ").append(method.getSignature()).append("\n");
        methodText.append("CODE: ").append(method.getBody()).append("\n");
        
        if (method.getJavadoc() != null) {
            methodText.append("JAVADOC: ").append(method.getJavadoc().getRawText()).append("\n");
        }
        
        return tokenCounter.estimateTokenCount(methodText.toString());
    }
}
