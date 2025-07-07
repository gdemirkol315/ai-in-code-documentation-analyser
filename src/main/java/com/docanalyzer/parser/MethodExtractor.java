package com.docanalyzer.parser;

import com.docanalyzer.model.Javadoc;
import com.docanalyzer.model.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts methods and their Javadoc comments from parsed Java files.
 */
public class MethodExtractor {
    
    private static final Logger log = LoggerFactory.getLogger(MethodExtractor.class);
    
    private final JavadocParser javadocParser;
    
    // Pattern to match method declarations with Javadoc (including annotations) - both implemented and interface methods
    // Updated to handle complex generic type parameters like <T extends Comparable<T>> before return type
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "(?s)(/\\*\\*.*?\\*/)\\s*(?:(?!class|interface|enum)[^\\S\\r\\n]*\\n)*[^\\S\\r\\n]*(?:@\\w+(?:\\([^)]*\\))?\\s*)*\\s*(public|private|protected|static|final|native|synchronized|abstract|transient)?\\s*(public|private|protected|static|final|native|synchronized|abstract|transient)?\\s*(?:<[^<>]*(?:<[^<>]*>[^<>]*)*>\\s+)?([\\w<>\\[\\].,]+)\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*(throws\\s+[\\w\\s,.]+)?\\s*([{;])"
    );
    
    // Pattern to match parameters
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("([\\w<>\\[\\]]+)\\s+(\\w+)");
    
    /**
     * Creates a new MethodExtractor with a JavadocParser.
     */
    public MethodExtractor() {
        this.javadocParser = new JavadocParser();
    }
    
    /**
     * Extracts all methods from a parsed Java file.
     * 
     * @param parsedFile The parsed Java file
     * @return A list of extracted methods
     */
    public List<Method> extractMethods(JavaParser.JavaFile parsedFile) {
        List<Method> methods = new ArrayList<>();
        String content = parsedFile.getContent();
        String packageName = parsedFile.getPackageName();
        String className = parsedFile.getClassName();
        String filePath = parsedFile.getFilePath();
        
        Matcher methodMatcher = METHOD_PATTERN.matcher(content);
        
        while (methodMatcher.find()) {
            try {
                String javadocComment = methodMatcher.group(1);
                if (javadocComment != null){
                    javadocComment = stripUntilMethodJavadoc(javadocComment);
                }
                String returnType = methodMatcher.group(4);
                String methodName = methodMatcher.group(5);
                String parameters = methodMatcher.group(6);
                
                // Extract method body using bracket counting or handle interface methods
                String methodTerminator = methodMatcher.group(8); // Either '{' or ';'
                String body;
                
                if ("{".equals(methodTerminator)) {
                    // Implemented method - extract body using bracket counting
                    int openBracePos = methodMatcher.end() - 1; // Position of the opening brace
                    MethodBodyResult bodyResult = extractMethodBody(content, openBracePos);
                    body = bodyResult.getBody();
                } else {
                    // Interface/abstract method - no body
                    body = "";
                }
                
                // Extract parameter information
                List<String> parameterNames = new ArrayList<>();
                List<String> parameterTypes = new ArrayList<>();
                
                if (parameters != null && !parameters.trim().isEmpty()) {
                    Matcher paramMatcher = PARAMETER_PATTERN.matcher(parameters);
                    while (paramMatcher.find()) {
                        parameterTypes.add(paramMatcher.group(1));
                        parameterNames.add(paramMatcher.group(2));
                    }
                }
                
                // Build method signature
                StringBuilder signatureBuilder = new StringBuilder();
                if (methodMatcher.group(2) != null) {
                    signatureBuilder.append(methodMatcher.group(2)).append(" ");
                }
                if (methodMatcher.group(3) != null) {
                    signatureBuilder.append(methodMatcher.group(3)).append(" ");
                }
                signatureBuilder.append(returnType).append(" ");
                signatureBuilder.append(methodName).append("(");
                
                for (int i = 0; i < parameterNames.size(); i++) {
                    if (i > 0) {
                        signatureBuilder.append(", ");
                    }
                    signatureBuilder.append(parameterTypes.get(i)).append(" ").append(parameterNames.get(i));
                }
                
                signatureBuilder.append(")");
                
                if (methodMatcher.group(7) != null) {
                    signatureBuilder.append(" ").append(methodMatcher.group(7));
                }
                
                String signature = signatureBuilder.toString();
                
                // Build full code based on method type
                String fullCode;
                String methodBody;
                if ("{".equals(methodTerminator)) {
                    methodBody = "{" + body + "}";
                    fullCode = signature + " " + methodBody;
                } else {
                    methodBody = "";
                    fullCode = signature + ";";
                }
                
                // Parse Javadoc
                Javadoc javadoc = null;
                if (javadocComment != null && !javadocComment.trim().isEmpty()) {
                    javadoc = javadocParser.parseJavadocComment(javadocComment, parameterNames);
                }
                
                // Create Method object
                Method method = Method.builder()
                        .name(methodName)
                        .signature(signature)
                        .body(methodBody)
                        .fullCode(fullCode)
                        .javadoc(javadoc)
                        .className(className)
                        .packageName(packageName)
                        .filePath(filePath)
                        .startLine(0) // Line numbers not available with regex approach
                        .endLine(0)
                        .parameterNames(parameterNames)
                        .parameterTypes(parameterTypes)
                        .returnType(returnType)
                        .build();
                
                methods.add(method);
            } catch (Exception e) {
                log.error("Error extracting method: {}", e.getMessage());
            }
        }
        
        return methods;
    }

    /**
     * Extracts method body using bracket counting to handle nested braces properly.
     * 
     * @param content the full content string
     * @param startPos the position of the opening brace
     * @return MethodBodyResult containing the body content and end position
     */
    private MethodBodyResult extractMethodBody(String content, int startPos) {
        if (startPos >= content.length() || content.charAt(startPos) != '{') {
            return new MethodBodyResult("", startPos);
        }
        
        int braceCount = 0;
        int pos = startPos;
        boolean inString = false;
        boolean inChar = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        char prevChar = '\0';
        
        while (pos < content.length()) {
            char currentChar = content.charAt(pos);
            
            // Handle line comments
            if (!inString && !inChar && !inBlockComment && prevChar == '/' && currentChar == '/') {
                inLineComment = true;
                pos++;
                prevChar = currentChar;
                continue;
            }
            
            // Handle block comments
            if (!inString && !inChar && !inLineComment && prevChar == '/' && currentChar == '*') {
                inBlockComment = true;
                pos++;
                prevChar = currentChar;
                continue;
            }
            
            // End block comment
            if (inBlockComment && prevChar == '*' && currentChar == '/') {
                inBlockComment = false;
                pos++;
                prevChar = currentChar;
                continue;
            }
            
            // End line comment
            if (inLineComment && (currentChar == '\n' || currentChar == '\r')) {
                inLineComment = false;
            }
            
            // Skip if in comments
            if (inLineComment || inBlockComment) {
                pos++;
                prevChar = currentChar;
                continue;
            }
            
            // Handle string literals
            if (!inChar && currentChar == '"' && prevChar != '\\') {
                inString = !inString;
            }
            
            // Handle character literals
            if (!inString && currentChar == '\'' && prevChar != '\\') {
                inChar = !inChar;
            }
            
            // Count braces only if not in string or char literal
            if (!inString && !inChar) {
                if (currentChar == '{') {
                    braceCount++;
                } else if (currentChar == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        // Found the closing brace for the method
                        String methodBody = content.substring(startPos + 1, pos);
                        return new MethodBodyResult(methodBody, pos + 1);
                    }
                }
            }
            
            pos++;
            prevChar = currentChar;
        }
        
        // If we reach here, braces weren't properly closed
        String methodBody = content.substring(startPos + 1);
        return new MethodBodyResult(methodBody, content.length());
    }
    
    /**
     * Helper class to hold method body extraction results.
     */
    private static class MethodBodyResult {
        private final String body;
        private final int endPosition;
        
        public MethodBodyResult(String body, int endPosition) {
            this.body = body;
            this.endPosition = endPosition;
        }
        
        public String getBody() {
            return body;
        }
        
        public int getEndPosition() {
            return endPosition;
        }
    }

    /**
     * This method was introduced to handle the unnecessarly taken string for the case when class, interface or enum
     * have general javadoc. Only method javadoc is relevant for the tool. It could not be handled with regex
     *
     * @param input string that will be handled for javadoc search
     * @return method javadoc string without including the general javadoc for class, interface or enum
     */

    private String stripUntilMethodJavadoc(String input) {
        // Match all Javadoc comments
        String javadocPattern = "/\\*\\*.*?\\*/";
        Pattern pattern = Pattern.compile(javadocPattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(input);

        int count = 0;
        int secondJavadocStart = -1;

        while (matcher.find()) {
            count++;
            if (count == 2) {
                secondJavadocStart = matcher.start();
                break;
            }
        }

        // If there are at least two Javadocs, remove everything before the second
        if (count >= 2 && secondJavadocStart != -1) {
            return input.substring(secondJavadocStart);
        }

        // Otherwise, return the original input
        return input;
        }
    }
