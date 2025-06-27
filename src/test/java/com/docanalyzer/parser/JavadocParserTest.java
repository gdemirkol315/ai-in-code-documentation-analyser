package com.docanalyzer.parser;

import com.docanalyzer.model.Javadoc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for JavadocParser.
 */
public class JavadocParserTest {
    
    private JavadocParser javadocParser;
    
    @BeforeEach
    public void setUp() {
        javadocParser = new JavadocParser();
    }
    
    @Test
    public void testParseJavadocComment_CompleteJavadoc() {
        // Given
        String javadocComment = "/**\n" +
                " * Calculates the sum of all values in the array.\n" +
                " * \n" +
                " * @param values The array of integers to sum\n" +
                " * @return The sum of all values\n" +
                " * @throws NullPointerException if the array is null\n" +
                " * @throws ArithmeticException if an arithmetic error occurs\n" +
                " * @see Math\n" +
                " */";
        List<String> parameterNames = Arrays.asList("values");
        
        // When
        Javadoc javadoc = javadocParser.parseJavadocComment(javadocComment, parameterNames);
        
        // Then
        assertNotNull(javadoc);
        assertEquals("Calculates the sum of all values in the array.", javadoc.getDescription());
        assertEquals(1, javadoc.getParamTags().size());
        assertEquals("values", javadoc.getParamTags().get(0).getParameterName());
        assertEquals("The array of integers to sum", javadoc.getParamTags().get(0).getDescription());
        assertEquals("The sum of all values", javadoc.getReturnTag());
        assertEquals(2, javadoc.getThrowsTags().size());
        assertEquals("NullPointerException", javadoc.getThrowsTags().get(0).getExceptionType());
        assertEquals("if the array is null", javadoc.getThrowsTags().get(0).getDescription());
        assertEquals(1, javadoc.getOtherTags().size());
        assertEquals("see", javadoc.getOtherTags().get(0).getTagName());
        assertEquals("Math", javadoc.getOtherTags().get(0).getContent());
    }
    
    @Test
    public void testParseJavadocComment_MinimalJavadoc() {
        // Given
        String javadocComment = "/**\n" +
                " * Simple description.\n" +
                " */";
        List<String> parameterNames = Arrays.asList("param1");
        
        // When
        Javadoc javadoc = javadocParser.parseJavadocComment(javadocComment, parameterNames);
        
        // Then
        assertNotNull(javadoc);
        assertEquals("Simple description.", javadoc.getDescription());
        assertTrue(javadoc.getParamTags().isEmpty());
        assertNull(javadoc.getReturnTag());
        assertTrue(javadoc.getThrowsTags().isEmpty());
        assertTrue(javadoc.getOtherTags().isEmpty());
    }
    
    @Test
    public void testParseJavadocComment_MissingParameterDocumentation() {
        // Given
        String javadocComment = "/**\n" +
                " * Method with undocumented parameter.\n" +
                " * \n" +
                " * @return Some value\n" +
                " */";
        List<String> parameterNames = Arrays.asList("undocumentedParam");
        
        // When
        Javadoc javadoc = javadocParser.parseJavadocComment(javadocComment, parameterNames);
        
        // Then
        assertNotNull(javadoc);
        assertEquals("Method with undocumented parameter.", javadoc.getDescription());
        assertTrue(javadoc.getParamTags().isEmpty());
        assertEquals("Some value", javadoc.getReturnTag());
    }
    
    @Test
    public void testParseJavadocComment_NullOrEmptyComment() {
        // Given
        List<String> parameterNames = Arrays.asList("param1");
        
        // When & Then
        assertNull(javadocParser.parseJavadocComment(null, parameterNames));
        assertNull(javadocParser.parseJavadocComment("", parameterNames));
        assertNull(javadocParser.parseJavadocComment("  ", parameterNames));
    }
}
