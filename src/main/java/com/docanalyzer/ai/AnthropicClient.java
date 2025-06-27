package com.docanalyzer.ai;

import com.docanalyzer.config.Configuration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;

/**
 * Client for making requests to the Anthropic API.
 */
@Slf4j
public class AnthropicClient {
    
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    
    private final String apiKey;
    private final String modelName;
    private final int maxTokens;
    private final double temperature;
    private final ObjectMapper objectMapper;
    
    /**
     * Creates a new AnthropicClient with the specified configuration.
     * 
     * @param config The configuration
     */
    public AnthropicClient(Configuration config) {
        this.apiKey = config.getAnthropicApiKey();
        this.modelName = config.getModelName();
        this.maxTokens = config.getMaxTokens();
        this.temperature = config.getTemperature();
        this.objectMapper = new ObjectMapper();
        
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Anthropic API key is not set. API requests will fail.");
        }
    }
    
    /**
     * Sends a request to the Anthropic API.
     * 
     * @param prompt The prompt to send
     * @return The API response
     * @throws IOException If an error occurs during the request
     */
    public String sendRequest(String prompt) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(API_URL);
            
            // Set headers
            httpPost.setHeader("x-api-key", apiKey);
            httpPost.setHeader("anthropic-version", ANTHROPIC_VERSION);
            httpPost.setHeader("Content-Type", "application/json");
            
            // Create request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", modelName);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);
            
            // Create messages array with system and user messages
            requestBody.putArray("messages")
                    .add(createMessage("user", prompt));
            
            // Set request body
            httpPost.setEntity(new StringEntity(requestBody.toString(), ContentType.APPLICATION_JSON));
            
            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                
                // Check for errors
                int statusCode = response.getCode();
                if (statusCode != 200) {
                    log.error("API request failed with status code {}: {}", statusCode, responseBody);
                    throw new IOException("API request failed with status code " + statusCode);
                }
                
                // Parse response
                JsonNode responseJson = objectMapper.readTree(responseBody);
                JsonNode contentNode = responseJson.path("content");
                
                if (contentNode.isArray() && contentNode.size() > 0) {
                    StringBuilder result = new StringBuilder();
                    for (JsonNode content : contentNode) {
                        if (content.has("text")) {
                            result.append(content.get("text").asText());
                        }
                    }
                    return result.toString();
                }
                
                log.error("Unexpected API response format: {}", responseBody);
                throw new IOException("Unexpected API response format");
            }
        } catch (Exception e) {
            log.error("Error sending request to Anthropic API: {}", e.getMessage());
            throw new IOException("Error sending request to Anthropic API", e);
        }
    }
    
    /**
     * Creates a message object for the API request.
     * 
     * @param role The role (user or assistant)
     * @param content The message content
     * @return The message object
     */
    private ObjectNode createMessage(String role, String content) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", role);
        message.put("content", content);
        return message;
    }
}
