package com.docanalyzer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Java method with its code and documentation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Method {
    
    /**
     * The name of the method.
     */
    private String name;
    
    /**
     * The method signature (including return type, name, and parameters).
     */
    private String signature;
    
    /**
     * The method body code.
     */
    private String body;
    
    /**
     * The full method code (signature + body).
     */
    private String fullCode;
    
    /**
     * The Javadoc associated with this method.
     */
    private Javadoc javadoc;
    
    /**
     * The class name containing this method.
     */
    private String className;
    
    /**
     * The package name containing the class.
     */
    private String packageName;
    
    /**
     * The file path where this method is defined.
     */
    private String filePath;
    
    /**
     * The line number where this method starts in the source file.
     */
    private int startLine;
    
    /**
     * The line number where this method ends in the source file.
     */
    private int endLine;
    
    /**
     * The list of parameter names for this method.
     */
    @Builder.Default
    private List<String> parameterNames = new ArrayList<>();
    
    /**
     * The list of parameter types for this method.
     */
    @Builder.Default
    private List<String> parameterTypes = new ArrayList<>();
    
    /**
     * The return type of the method.
     */
    private String returnType;
    
    /**
     * The metrics results for this method's documentation.
     */
    private MetricsResult metricsResult;
}
