package com.docanalyzer.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Java source files using a simple regex-based approach.
 */
public class JavaParser {
    
    private static final Logger log = LoggerFactory.getLogger(JavaParser.class);
    
    // Pattern to match package declaration
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([\\w.]+)\\s*;");
    
    // Pattern to match class declaration
    private static final Pattern CLASS_PATTERN = Pattern.compile("(public|private|protected)?\\s*(class|interface|enum)\\s+(\\w+)");
    
    /**
     * Parses a Java source file.
     * 
     * @param filePath The path to the Java file
     * @return An Optional containing the parsed JavaFile, or empty if parsing failed
     */
    public Optional<JavaFile> parseFile(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                log.error("File not found: {}", filePath);
                return Optional.empty();
            }
            
            String content = readFileContent(file);
            String packageName = extractPackageName(content);
            String className = extractClassName(content);
            
            JavaFile javaFile = new JavaFile(filePath, content, packageName, className);
            return Optional.of(javaFile);
        } catch (IOException e) {
            log.error("Error reading file {}: {}", filePath, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error parsing file {}: {}", filePath, e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Parses all Java files in a directory (recursively).
     * 
     * @param directoryPath The path to the directory
     * @return A list of parsed JavaFiles
     */
    public List<JavaFile> parseDirectory(String directoryPath) {
        List<JavaFile> parsedFiles = new ArrayList<>();
        File directory = new File(directoryPath);
        
        if (!directory.exists() || !directory.isDirectory()) {
            log.error("Directory does not exist or is not a directory: {}", directoryPath);
            return parsedFiles;
        }
        
        parseDirectoryRecursively(directory, parsedFiles);
        return parsedFiles;
    }
    
    /**
     * Recursively parses all Java files in a directory and its subdirectories.
     * 
     * @param directory The directory to parse
     * @param parsedFiles The list to add parsed files to
     */
    private void parseDirectoryRecursively(File directory, List<JavaFile> parsedFiles) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                parseDirectoryRecursively(file, parsedFiles);
            } else if (file.getName().endsWith(".java")) {
                parseFile(file.getAbsolutePath()).ifPresent(parsedFiles::add);
            }
        }
    }
    
    /**
     * Reads the content of a file.
     * 
     * @param file The file to read
     * @return The file content as a string
     * @throws IOException If an I/O error occurs
     */
    private String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
    
    /**
     * Extracts the package name from the file content.
     * 
     * @param content The file content
     * @return The package name, or an empty string if not found
     */
    private String extractPackageName(String content) {
        Matcher matcher = PACKAGE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
    
    /**
     * Extracts the class name from the file content.
     * 
     * @param content The file content
     * @return The class name, or an empty string if not found
     */
    private String extractClassName(String content) {
        Matcher matcher = CLASS_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(3);
        }
        return "";
    }
    
    /**
     * Represents a parsed Java file.
     */
    public static class JavaFile {
        private final String filePath;
        private final String content;
        private final String packageName;
        private final String className;
        
        public JavaFile(String filePath, String content, String packageName, String className) {
            this.filePath = filePath;
            this.content = content;
            this.packageName = packageName;
            this.className = className;
        }
        
        public String getFilePath() {
            return filePath;
        }
        
        public String getContent() {
            return content;
        }
        
        public String getPackageName() {
            return packageName;
        }
        
        public String getClassName() {
            return className;
        }
        
        public String getRelativePath(String baseDir) {
            Path basePath = Paths.get(baseDir).toAbsolutePath();
            Path filePath = Paths.get(this.filePath).toAbsolutePath();
            
            if (filePath.startsWith(basePath)) {
                return basePath.relativize(filePath).toString();
            }
            
            return this.filePath;
        }
    }
}
