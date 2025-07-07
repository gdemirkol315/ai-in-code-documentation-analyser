package com.docanalyzer.parser;

import com.docanalyzer.model.Method;
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
 * Test class to verify that the new bracket counting approach works correctly with nested braces.
 */
public class BracketCountingTest {
    
    private JavaParser javaParser;
    private MethodExtractor methodExtractor;
    
    @BeforeEach
    public void setUp() {
        javaParser = new JavaParser();
        methodExtractor = new MethodExtractor();
    }
    
    @Test
    public void testNestedBraces(@TempDir Path tempDir) throws IOException {
        // Given
        File javaFile = createNestedBracesFile(tempDir);
        Optional<JavaParser.JavaFile> parsedFileOpt = javaParser.parseFile(javaFile.getAbsolutePath());
        assertTrue(parsedFileOpt.isPresent());
        JavaParser.JavaFile parsedFile = parsedFileOpt.get();
        
        // When
        List<Method> methods = methodExtractor.extractMethods(parsedFile);
        
        // Then
        assertEquals(2, methods.size());
        
        // Verify complex method with nested braces
        Method complexMethod = methods.stream()
                .filter(m -> m.getName().equals("complexMethod"))
                .findFirst()
                .orElseThrow();
        
        // The method body should contain all nested braces properly
        String body = complexMethod.getBody();
        assertTrue(body.contains("if (condition) {"));
        assertTrue(body.contains("for (int i = 0; i < 10; i++) {"));
        assertTrue(body.contains("while (running) {"));
        assertTrue(body.contains("System.out.println(\"Nested: \" + i);"));
        assertTrue(body.contains("// String with braces: \"text with { and }\""));
        
        // Verify interface method
        Method interfaceMethod = methods.stream()
                .filter(m -> m.getName().equals("interfaceMethod"))
                .findFirst()
                .orElseThrow();
        
        assertEquals("", interfaceMethod.getBody());
        assertTrue(interfaceMethod.getFullCode().endsWith(";"));
    }
    
    private File createNestedBracesFile(Path tempDir) throws IOException {
        File file = tempDir.resolve("NestedBracesClass.java").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("package com.example;\n\n" +
                    "/**\n" +
                    " * A class to test nested braces handling.\n" +
                    " */\n" +
                    "public class NestedBracesClass {\n" +
                    "    \n" +
                    "    /**\n" +
                    "     * A complex method with deeply nested braces.\n" +
                    "     * \n" +
                    "     * @param condition the condition to check\n" +
                    "     * @param running the running flag\n" +
                    "     */\n" +
                    "    public void complexMethod(boolean condition, boolean running) {\n" +
                    "        if (condition) {\n" +
                    "            for (int i = 0; i < 10; i++) {\n" +
                    "                while (running) {\n" +
                    "                    System.out.println(\"Nested: \" + i);\n" +
                    "                    // String with braces: \"text with { and }\"\n" +
                    "                    if (i > 5) {\n" +
                    "                        break;\n" +
                    "                    }\n" +
                    "                }\n" +
                    "            }\n" +
                    "        }\n" +
                    "    }\n" +
                    "    \n" +
                    "    /**\n" +
                    "     * An interface method declaration.\n" +
                    "     * \n" +
                    "     * @param value the value to process\n" +
                    "     * @return processed result\n" +
                    "     */\n" +
                    "    public abstract String interfaceMethod(String value);\n" +
                    "}\n");
        }
        return file;
    }
}
