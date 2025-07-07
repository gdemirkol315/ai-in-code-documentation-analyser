package com.docanalyzer.parser;

import com.docanalyzer.model.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        File javaFile = Paths.get("/home/gork/IdeaProjects/ai-in-code-documentation-analyser/src/main/resources/Assess.java").toFile();
        Optional<JavaParser.JavaFile> parsedFileOpt = javaParser.parseFile(javaFile.getAbsolutePath());
        assertTrue(parsedFileOpt.isPresent());
        JavaParser.JavaFile parsedFile = parsedFileOpt.get();
        
        // When
        List<Method> methods = methodExtractor.extractMethods(parsedFile);
        
        // Then
        assertEquals(9, methods.size());
        
        // Verify ceil method (first method in Assess.java)
        Method ceilMethod = methods.stream()
                .filter(m -> m.getName().equals("ceil"))
                .findFirst()
                .orElseThrow();
        assertEquals("double", ceilMethod.getReturnType());
        assertEquals(1, ceilMethod.getParameterNames().size());
        assertEquals("number", ceilMethod.getParameterNames().get(0));
        assertEquals("double", ceilMethod.getParameterTypes().get(0));
        assertNotNull(ceilMethod.getJavadoc());
        assertTrue(ceilMethod.getJavadoc().getDescription().contains("smallest"));
        
        // Verify commitNameChange method (has @Override annotation)
        Method commitMethod = methods.stream()
                .filter(m -> m.getName().equals("commitNameChange"))
                .findFirst()
                .orElseThrow();
        assertEquals("void", commitMethod.getReturnType());
        assertEquals(1, commitMethod.getParameterNames().size());
        assertEquals("evt", commitMethod.getParameterNames().get(0));
        assertNotNull(commitMethod.getJavadoc());
        assertTrue(commitMethod.getJavadoc().getDescription().contains("change in name"));
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
    
    @Test
    public void testExtractMethods_WithAnnotations(@TempDir Path tempDir) throws IOException {
        // Given
        File javaFile = createAnnotatedJavaFile(tempDir);
        Optional<JavaParser.JavaFile> parsedFileOpt = javaParser.parseFile(javaFile.getAbsolutePath());
        assertTrue(parsedFileOpt.isPresent());
        JavaParser.JavaFile parsedFile = parsedFileOpt.get();
        
        // When
        List<Method> methods = methodExtractor.extractMethods(parsedFile);
        
        // Then
        assertEquals(2, methods.size());
        
        // Verify method with @Override annotation
        Method overrideMethod = methods.stream()
                .filter(m -> m.getName().equals("processValue"))
                .findFirst()
                .orElseThrow();
        assertEquals("String", overrideMethod.getReturnType());
        assertEquals(1, overrideMethod.getParameterNames().size());
        assertEquals("value", overrideMethod.getParameterNames().get(0));
        assertEquals("String", overrideMethod.getParameterTypes().get(0));
        assertNotNull(overrideMethod.getJavadoc());
        assertEquals("This method has an annotation.", overrideMethod.getJavadoc().getDescription());
        
        // Verify method with multiple annotations
        Method multiAnnotationMethod = methods.stream()
                .filter(m -> m.getName().equals("processData"))
                .findFirst()
                .orElseThrow();
        assertEquals("List<String>", multiAnnotationMethod.getReturnType());
        assertEquals(1, multiAnnotationMethod.getParameterNames().size());
        assertEquals("data", multiAnnotationMethod.getParameterNames().get(0));
        assertEquals("Object", multiAnnotationMethod.getParameterTypes().get(0));
        assertNotNull(multiAnnotationMethod.getJavadoc());
        assertEquals("This method has multiple annotations.", multiAnnotationMethod.getJavadoc().getDescription());
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
    
    private File createAnnotatedJavaFile(Path tempDir) throws IOException {
        File file = tempDir.resolve("AnnotatedClass.java").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("package com.example.annotated;\n\n" +
                    "import java.util.List;\n" +
                    "import java.util.Arrays;\n\n" +
                    "/**\n" +
                    " * A class with annotated methods.\n" +
                    " */\n" +
                    "public class AnnotatedClass {\n" +
                    "    \n" +
                    "    /**\n" +
                    "     * This method has an annotation.\n" +
                    "     * @param value the input value\n" +
                    "     * @return processed value\n" +
                    "     */\n" +
                    "    @Override\n" +
                    "    public String processValue(String value) {\n" +
                    "        return value.toUpperCase();\n" +
                    "    }\n" +
                    "    \n" +
                    "    /**\n" +
                    "     * This method has multiple annotations.\n" +
                    "     * @param data the data to process\n" +
                    "     * @return result\n" +
                    "     */\n" +
                    "    @SuppressWarnings(\"unchecked\")\n" +
                    "    @NotNull\n" +
                    "    public List<String> processData(Object data) {\n" +
                    "        return Arrays.asList(data.toString());\n" +
                    "    }\n" +
                    "}\n");
        }
        return file;
    }
}
