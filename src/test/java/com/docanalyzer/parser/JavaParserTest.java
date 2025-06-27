package com.docanalyzer.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for JavaParser.
 */
public class JavaParserTest {
    
    private JavaParser javaParser;
    
    @BeforeEach
    public void setUp() {
        javaParser = new JavaParser();
    }
    
    @Test
    public void testParseFile_ValidJavaFile(@TempDir Path tempDir) throws IOException {
        // Given
        File javaFile = createSampleJavaFile(tempDir);
        
        // When
        Optional<JavaParser.JavaFile> result = javaParser.parseFile(javaFile.getAbsolutePath());
        
        // Then
        assertTrue(result.isPresent());
        JavaParser.JavaFile parsedFile = result.get();
        assertEquals("com.example", parsedFile.getPackageName());
        assertEquals("Calculator", parsedFile.getClassName());
        assertTrue(parsedFile.getContent().contains("public class Calculator"));
    }
    
    @Test
    public void testParseFile_NonExistentFile() {
        // Given
        String nonExistentFilePath = "/path/to/nonexistent/file.java";
        
        // When
        Optional<JavaParser.JavaFile> result = javaParser.parseFile(nonExistentFilePath);
        
        // Then
        assertFalse(result.isPresent());
    }
    
    @Test
    public void testParseDirectory(@TempDir Path tempDir) throws IOException {
        // Given
        createSampleJavaFile(tempDir);
        createAnotherSampleJavaFile(tempDir);
        
        // When
        List<JavaParser.JavaFile> result = javaParser.parseDirectory(tempDir.toString());
        
        // Then
        assertEquals(2, result.size());
        
        // Debug: Print parsed files to understand what's being parsed
        result.forEach(file -> {
            System.out.println("Parsed file: " + file.getClassName() + " in package: " + file.getPackageName());
        });
        
        // Verify first file
        JavaParser.JavaFile file1 = result.stream()
                .filter(f -> f.getClassName().equals("Calculator"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Calculator class not found in parsed results. Found classes: " + 
                    result.stream().map(JavaParser.JavaFile::getClassName).toList()));
        assertEquals("com.example", file1.getPackageName());
        
        // Verify second file
        JavaParser.JavaFile file2 = result.stream()
                .filter(f -> f.getClassName().equals("StringUtils"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("StringUtils class not found in parsed results. Found classes: " + 
                    result.stream().map(JavaParser.JavaFile::getClassName).toList()));
        assertEquals("com.example.util", file2.getPackageName());
    }
    
    @Test
    public void testParseDirectory_NonExistentDirectory() {
        // Given
        String nonExistentDirPath = "/path/to/nonexistent/directory";
        
        // When
        List<JavaParser.JavaFile> result = javaParser.parseDirectory(nonExistentDirPath);
        
        // Then
        assertTrue(result.isEmpty());
    }
    
    @Test
    public void testGetRelativePath(@TempDir Path tempDir) throws IOException {
        // Given
        File javaFile = createSampleJavaFile(tempDir);
        String baseDir = tempDir.toString();
        
        // When
        Optional<JavaParser.JavaFile> result = javaParser.parseFile(javaFile.getAbsolutePath());
        
        // Then
        assertTrue(result.isPresent());
        JavaParser.JavaFile parsedFile = result.get();
        String relativePath = parsedFile.getRelativePath(baseDir);
        assertEquals("Calculator.java", relativePath);
    }
    
    private File createSampleJavaFile(Path tempDir) throws IOException {
        File file = tempDir.resolve("Calculator.java").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("package com.example;\n\n" +
                    "/**\n" +
                    " * A simple calculator class.\n" +
                    " */\n" +
                    "public class Calculator {\n" +
                    "    /**\n" +
                    "     * Adds two numbers.\n" +
                    "     * \n" +
                    "     * @param a the first number\n" +
                    "     * @param b the second number\n" +
                    "     * @return the sum of a and b\n" +
                    "     */\n" +
                    "    public int add(int a, int b) {\n" +
                    "        return a + b;\n" +
                    "    }\n" +
                    "    \n" +
                    "    /**\n" +
                    "     * Subtracts two numbers.\n" +
                    "     * \n" +
                    "     * @param a the first number\n" +
                    "     * @param b the second number\n" +
                    "     * @return the difference of a and b\n" +
                    "     */\n" +
                    "    public int subtract(int a, int b) {\n" +
                    "        return a - b;\n" +
                    "    }\n" +
                    "}\n");
        }
        return file;
    }
    
    private File createAnotherSampleJavaFile(Path tempDir) throws IOException {
        File file = tempDir.resolve("StringUtils.java").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("package com.example.util;\n\n" +
                    "/**\n" +
                    " * Utility class for string operations.\n" +
                    " */\n" +
                    "public class StringUtils {\n" +
                    "    /**\n" +
                    "     * Reverses a string.\n" +
                    "     * \n" +
                    "     * @param input the string to reverse\n" +
                    "     * @return the reversed string\n" +
                    "     */\n" +
                    "    public static String reverse(String input) {\n" +
                    "        return new StringBuilder(input).reverse().toString();\n" +
                    "    }\n" +
                    "}\n");
        }
        return file;
    }
}
