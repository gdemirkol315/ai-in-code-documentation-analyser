package com.docanalyzer.ai;

import com.docanalyzer.model.MetricsResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ResponseParser.
 */
public class ResponseParserTest {

    private ResponseParser responseParser;
    private String apiResponse;

    @BeforeEach
    public void setUp() throws IOException {
        responseParser = new ResponseParser();
        // Read the API response from the test resources
        apiResponse = Files.readString(
            Paths.get("src/test/resources/api-response.txt"), 
            StandardCharsets.UTF_8
        );
    }

    @Test
    public void testParseBatchResponse_WithValidResponse() {
        // Test parsing the batch response with expected 5 methods
        Map<Integer, MetricsResult> results = responseParser.parseBatchResponse(apiResponse, 5);
        
        // Assert that we got results for all 5 methods
        assertEquals(5, results.size(), "Should parse all 5 methods from the response");
        
        // Verify that all method indices are present
        for (int i = 1; i <= 5; i++) {
            assertTrue(results.containsKey(i), "Should contain method " + i);
        }
    }

    @Test
    public void testParseBatchResponse_Method1_Constructor() {
        Map<Integer, MetricsResult> results = responseParser.parseBatchResponse(apiResponse, 5);
        MetricsResult method1Result = results.get(1);
        
        assertNotNull(method1Result, "Method 1 result should not be null");
        
        // Check metrics
        assertEquals(3, method1Result.getMetricResults().size(), "Method 1 should have 3 metrics");
        
        // Check Completeness metric
        assertTrue(method1Result.getMetricResults().containsKey("Completeness"));
        assertEquals(4, method1Result.getMetricResults().get("Completeness").getScore());
        assertTrue(method1Result.getMetricResults().get("Completeness").getFeedback()
            .contains("covers the essential information"));
        
        // Check Code Alignment metric
        assertTrue(method1Result.getMetricResults().containsKey("Code Alignment"));
        assertEquals(5, method1Result.getMetricResults().get("Code Alignment").getScore());
        assertTrue(method1Result.getMetricResults().get("Code Alignment").getFeedback()
            .contains("accurately reflects the code"));
        
        // Check Clarity metric
        assertTrue(method1Result.getMetricResults().containsKey("Clarity"));
        assertEquals(5, method1Result.getMetricResults().get("Clarity").getScore());
        assertTrue(method1Result.getMetricResults().get("Clarity").getFeedback()
            .contains("clear, concise, and easy to understand"));
        
        // Check recommendations
        assertEquals(1, method1Result.getRecommendations().size());
        assertTrue(method1Result.getRecommendations().get(0)
            .contains("Consider adding a brief mention of the initialization"));
        
        // Check overall score calculation
        double expectedOverallScore = (4 + 5 + 5) / 3.0;
        assertEquals(expectedOverallScore, method1Result.getOverallScore(), 0.01);
    }

    @Test
    public void testParseBatchResponse_Method2_AddItem() {
        Map<Integer, MetricsResult> results = responseParser.parseBatchResponse(apiResponse, 5);
        MetricsResult method2Result = results.get(2);
        
        assertNotNull(method2Result, "Method 2 result should not be null");
        
        // Check metrics scores
        assertEquals(3, method2Result.getMetricResults().get("Completeness").getScore());
        assertEquals(3, method2Result.getMetricResults().get("Code Alignment").getScore());
        assertEquals(4, method2Result.getMetricResults().get("Clarity").getScore());
        
        // Check recommendations
        assertEquals(2, method2Result.getRecommendations().size());
        assertTrue(method2Result.getRecommendations().get(0)
            .contains("Include information about updating the count"));
        assertTrue(method2Result.getRecommendations().get(1)
            .contains("Consider providing more details on how the count is updated"));
        
        // Check overall score
        double expectedOverallScore = (3 + 3 + 4) / 3.0;
        assertEquals(expectedOverallScore, method2Result.getOverallScore(), 0.01);
    }

    @Test
    public void testParseBatchResponse_Method3_GetCount() {
        Map<Integer, MetricsResult> results = responseParser.parseBatchResponse(apiResponse, 5);
        MetricsResult method3Result = results.get(3);
        
        assertNotNull(method3Result, "Method 3 result should not be null");
        
        // This method has perfect scores across all metrics
        assertEquals(5, method3Result.getMetricResults().get("Completeness").getScore());
        assertEquals(5, method3Result.getMetricResults().get("Code Alignment").getScore());
        assertEquals(5, method3Result.getMetricResults().get("Clarity").getScore());
        
        // Should have no recommendations (perfect documentation)
        assertEquals(0, method3Result.getRecommendations().size());
        
        // Perfect overall score
        assertEquals(5.0, method3Result.getOverallScore(), 0.01);
    }

    @Test
    public void testParseBatchResponse_Method4_CalculateAverageLength() {
        Map<Integer, MetricsResult> results = responseParser.parseBatchResponse(apiResponse, 5);
        MetricsResult method4Result = results.get(4);
        
        assertNotNull(method4Result, "Method 4 result should not be null");
        
        // Check metrics scores
        assertEquals(4, method4Result.getMetricResults().get("Completeness").getScore());
        assertEquals(4, method4Result.getMetricResults().get("Code Alignment").getScore());
        assertEquals(5, method4Result.getMetricResults().get("Clarity").getScore());
        
        // Check recommendations
        assertEquals(1, method4Result.getRecommendations().size());
        assertTrue(method4Result.getRecommendations().get(0)
            .contains("Remove the @throws tag for ArithmeticException"));
        
        // Check overall score
        double expectedOverallScore = (4 + 4 + 5) / 3.0;
        assertEquals(expectedOverallScore, method4Result.getOverallScore(), 0.01);
    }

    @Test
    public void testParseBatchResponse_Method5_ProcessItem() {
        Map<Integer, MetricsResult> results = responseParser.parseBatchResponse(apiResponse, 5);
        MetricsResult method5Result = results.get(5);
        
        assertNotNull(method5Result, "Method 5 result should not be null");
        
        // Check metrics scores
        assertEquals(5, method5Result.getMetricResults().get("Completeness").getScore());
        assertEquals(5, method5Result.getMetricResults().get("Code Alignment").getScore());
        assertEquals(4, method5Result.getMetricResults().get("Clarity").getScore());
        
        // Check recommendations
        assertEquals(1, method5Result.getRecommendations().size());
        assertTrue(method5Result.getRecommendations().get(0)
            .contains("Consider providing more details on how the item is processed"));
        
        // Check overall score
        double expectedOverallScore = (5 + 5 + 4) / 3.0;
        assertEquals(expectedOverallScore, method5Result.getOverallScore(), 0.01);
    }

    @Test
    public void testParseSingleMethodResponse() {
        // Extract just the first method from the response for testing single method parsing
        String singleMethodResponse = "METHOD 1 ExampleClass() EVALUATION:\n" +
            "Completeness: 4\n" +
            "Justification: The documentation covers the essential information about the constructor, but it could mention that it initializes empty collections for items and counts.\n" +
            "Code Alignment: 5\n" +
            "Justification: The documentation accurately reflects the code, which initializes empty ArrayList and HashMap for items and counts respectively.\n" +
            "Clarity: 5\n" +
            "Justification: The documentation is clear, concise, and easy to understand.\n" +
            "Recommendations:\n" +
            "1. Consider adding a brief mention of the initialization of empty collections for items and counts.";
        
        MetricsResult result = responseParser.parseSingleMethodResponse(singleMethodResponse);
        
        assertNotNull(result, "Single method result should not be null");
        assertEquals(3, result.getMetricResults().size(), "Should have 3 metrics");
        assertEquals(1, result.getRecommendations().size(), "Should have 1 recommendation");
        
        // Verify the scores
        assertEquals(4, result.getMetricResults().get("Completeness").getScore());
        assertEquals(5, result.getMetricResults().get("Code Alignment").getScore());
        assertEquals(5, result.getMetricResults().get("Clarity").getScore());
    }

    @Test
    public void testParseBatchResponse_WithWrongExpectedCount() {
        // Test with wrong expected count to verify warning behavior
        Map<Integer, MetricsResult> results = responseParser.parseBatchResponse(apiResponse, 3);
        
        // Should still parse all available methods
        assertEquals(5, results.size(), "Should parse all available methods regardless of expected count");
    }

    @Test
    public void testParseBatchResponse_WithEmptyResponse() {
        Map<Integer, MetricsResult> results = responseParser.parseBatchResponse("", 0);
        
        assertTrue(results.isEmpty(), "Empty response should return empty results");
    }

    @Test
    public void testParseBatchResponse_WithMalformedResponse() {
        String malformedResponse = "This is not a valid API response format";
        Map<Integer, MetricsResult> results = responseParser.parseBatchResponse(malformedResponse, 1);
        
        assertTrue(results.isEmpty(), "Malformed response should return empty results");
    }

    @Test
    public void testMetricResultsContainExpectedFields() {
        Map<Integer, MetricsResult> results = responseParser.parseBatchResponse(apiResponse, 5);
        MetricsResult method1Result = results.get(1);
        
        // Verify that metric results contain all expected fields
        MetricsResult.MetricResult completenessResult = method1Result.getMetricResults().get("Completeness");
        assertNotNull(completenessResult.getName());
        assertTrue(completenessResult.getScore() >= 1 && completenessResult.getScore() <= 5);
        assertNotNull(completenessResult.getFeedback());
        assertFalse(completenessResult.getFeedback().trim().isEmpty());
    }
}
