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
 * Test class for MethodExtractor.
 */
public class MethodExtractorTest {
    
    private JavaParser javaParser;
    private MethodExtractor methodExtractor;
    
    @BeforeEach
    public void setUp() {
        javaParser = new JavaParser();
        methodExtractor = new MethodExtractor();
    }
    
    @Test
    public void testExtractMethods_SimpleClass(@TempDir Path tempDir) throws IOException {
        // Given
        File javaFile = createSimpleJavaFile(tempDir);
        Optional<JavaParser.JavaFile> parsedFileOpt = javaParser.parseFile(javaFile.getAbsolutePath());
        assertTrue(parsedFileOpt.isPresent());
        JavaParser.JavaFile parsedFile = parsedFileOpt.get();
        
        // When
        List<Method> methods = methodExtractor.extractMethods(parsedFile);
        
        // Then
        assertEquals(2, methods.size());
        
        // Verify first method
        Method addMethod = methods.stream()
                .filter(m -> m.getName().equals("add"))
                .findFirst()
                .orElseThrow();
        assertEquals("int", addMethod.getReturnType());
        assertEquals(2, addMethod.getParameterNames().size());
        assertEquals("a", addMethod.getParameterNames().get(0));
        assertEquals("b", addMethod.getParameterNames().get(1));
        assertEquals("int", addMethod.getParameterTypes().get(0));
        assertEquals("int", addMethod.getParameterTypes().get(1));
        assertNotNull(addMethod.getJavadoc());
        assertEquals("Adds two numbers.", addMethod.getJavadoc().getDescription());
        
        // Verify second method
        Method subtractMethod = methods.stream()
                .filter(m -> m.getName().equals("subtract"))
                .findFirst()
                .orElseThrow();
        assertEquals("int", subtractMethod.getReturnType());
        assertEquals(2, subtractMethod.getParameterNames().size());
        assertNotNull(subtractMethod.getJavadoc());
        assertEquals("Subtracts two numbers.", subtractMethod.getJavadoc().getDescription());
    }
    
    @Test
    public void testExtractMethods_ComplexClass(@TempDir Path tempDir) throws IOException {
        // Given
        File javaFile = createComplexJavaFile(tempDir);
        Optional<JavaParser.JavaFile> parsedFileOpt = javaParser.parseFile(javaFile.getAbsolutePath());
        assertTrue(parsedFileOpt.isPresent());
        JavaParser.JavaFile parsedFile = parsedFileOpt.get();
        
        // When
        List<Method> methods = methodExtractor.extractMethods(parsedFile);
        
        // Then
        assertEquals(2, methods.size());

        // Verify method with complex parameters
        Method complexMethod = methods.stream()
                .filter(m -> m.getName().equals("processData"))
                .findFirst()
                .orElseThrow();
        assertEquals("void", complexMethod.getReturnType());
        assertEquals(3, complexMethod.getParameterNames().size());
        assertEquals("List<String>", complexMethod.getParameterTypes().get(0));
        assertNotNull(complexMethod.getJavadoc());
        assertEquals(2, complexMethod.getJavadoc().getParamTags().size());
        assertEquals(1, complexMethod.getJavadoc().getThrowsTags().size());
        
        // Verify method with generic return type
        Method genericMethod = methods.stream()
                .filter(m -> m.getName().equals("convertToList"))
                .findFirst()
                .orElseThrow();
        assertEquals("List<T>", genericMethod.getReturnType());
        assertEquals(1, genericMethod.getParameterNames().size());
        assertNotNull(genericMethod.getJavadoc());
    }
    
    private File createSimpleJavaFile(Path tempDir) throws IOException {
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
    
    private File createComplexJavaFile(Path tempDir) throws IOException {
        File file = tempDir.resolve("DataProcessor.java").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("package com.example.data;\n\n" +
                    "import java.util.List;\n" +
                    "import java.util.ArrayList;\n" +
                    "import java.util.Map;\n" +
                    "import java.util.HashMap;\n\n" +
                    "/**\n" +
                    " * A complex data processor class.\n" +
                    " */\n" +
                    "public class DataProcessor<T> {\n" +
                    "    \n" +
                    "    public void noJavadocMethod() {\n" +
                    "        // This method has no Javadoc\n" +
                    "        System.out.println(\"No Javadoc\");\n" +
                    "    }\n" +
                    "    \n" +
                    "    /**\n" +
                    "     * Processes data from multiple sources.\n" +
                    "     * \n" +
                    "     * @param data the list of data to process\n" +
                    "     * @param options processing options\n" +
                    "     * @throws IllegalArgumentException if data is invalid\n" +
                    "     */\n" +
                    "    public void processData(List<String> data, Map<String, Object> options, int timeout) {\n" +
                    "        if (data == null || data.isEmpty()) {\n" +
                    "            throw new IllegalArgumentException(\"Data cannot be null or empty\");\n" +
                    "        }\n" +
                    "        \n" +
                    "        // Process data\n" +
                    "        for (String item : data) {\n" +
                    "            System.out.println(\"Processing: \" + item);\n" +
                    "        }\n" +
                    "    }\n" +
                    "    \n" +
                    "    /**\n" +
                    "     * Converts an array to a list.\n" +
                    "     * \n" +
                    "     * @param array the array to convert\n" +
                    "     * @return a list containing the elements from the array\n" +
                    "     */\n" +
                    "    public List<T> convertToList(T[] array) {\n" +
                    "        List<T> list = new ArrayList<>();\n" +
                    "        for (T item : array) {\n" +
                    "            list.add(item);\n" +
                    "        }\n" +
                    "        return list;\n" +
                    "    }\n" +
                    "}\n");
        }
        return file;
    }
}
