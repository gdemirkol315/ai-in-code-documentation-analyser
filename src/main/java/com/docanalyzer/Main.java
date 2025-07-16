package com.docanalyzer;

import com.docanalyzer.batch.BatchProcessor;
import com.docanalyzer.config.Configuration;
import com.docanalyzer.metrics.MetricsManager;
import com.docanalyzer.model.Method;
import com.docanalyzer.output.XMLReportGenerator;
import com.docanalyzer.parser.JavaParser;
import com.docanalyzer.parser.MethodExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main entry point for the documentation analyzer.
 */
public class Main {
    
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    
    /**
     * Main method.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            
            log.info("Starting Documentation Analyzer");
            
            // Parse command line arguments
            CommandLineArgs cmdArgs = parseCommandLineArgs(args);

            log.info("Parsed arguments - Config path: {}, Input paths: {}", 
                     cmdArgs.getConfigPath(), cmdArgs.getInputPaths());
            
            if (cmdArgs.isHelp()) {
                printHelp();
                return;
            }
            
            // Load configuration
            Configuration config;
            if (cmdArgs.getConfigPath() != null && !cmdArgs.getConfigPath().isEmpty()) {
                config = Configuration.loadFromFile(cmdArgs.getConfigPath());
                log.info("Loaded configuration from {}", cmdArgs.getConfigPath());
            } else {
                config = Configuration.getDefaultConfiguration();
                log.info("Using default configuration");
            }
            
            // Override configuration with command line arguments
            if (cmdArgs.getOutputPath() != null && !cmdArgs.getOutputPath().isEmpty()) {
                config.setOutputPath(cmdArgs.getOutputPath());
            }
            
            if (cmdArgs.getMetricsPath() != null && !cmdArgs.getMetricsPath().isEmpty()) {
                config.setMetricsDefinitionsPath(cmdArgs.getMetricsPath());
            }
            
            // Ensure output directory exists
            config.ensureOutputDirectoryExists();
            
            // Load metrics
            MetricsManager metricsManager = new MetricsManager();
            boolean metricsLoaded = false;
            
            if (Files.exists(Paths.get(config.getMetricsDefinitionsPath()))) {
                metricsLoaded = metricsManager.loadMetricsFromFile(config.getMetricsDefinitionsPath());
            }
            
            if (!metricsLoaded) {
                log.info("Creating default metrics file");
                metricsManager.createDefaultMetricsFile(config.getMetricsDefinitionsPath());
            }
            
            // Get formatted guidelines
            String guidelines = metricsManager.getFormattedGuidelines();
            
            // Parse Java files
            JavaParser javaParser = new JavaParser();
            MethodExtractor methodExtractor = new MethodExtractor();
            List<Method> allMethods = new ArrayList<>();
            
            for (String inputPath : cmdArgs.getInputPaths()) {
                File inputFile = new File(inputPath);
                
                if (!inputFile.exists()) {
                    log.error("Input path does not exist: {}", inputPath);
                    continue;
                }
                
                if (inputFile.isDirectory()) {
                    log.info("Parsing directory: {}", inputPath);
                    List<JavaParser.JavaFile> parsedFiles = javaParser.parseDirectory(inputPath);
                    
                    for (JavaParser.JavaFile parsedFile : parsedFiles) {
                        List<Method> methods = methodExtractor.extractMethods(parsedFile);
                        allMethods.addAll(methods);
                    }
                } else if (inputFile.getName().endsWith(".java")) {
                    log.info("Parsing file: {}", inputPath);
                    javaParser.parseFile(inputPath).ifPresent(parsedFile -> {
                        List<Method> methods = methodExtractor.extractMethods(parsedFile);
                        allMethods.addAll(methods);
                    });
                } else {
                    log.warn("Skipping non-Java file: {}", inputPath);
                }
            }
            
            log.info("Extracted {} methods from {} input paths", allMethods.size(), cmdArgs.getInputPaths().size());
            
            // Filter methods to only include those with Javadoc
            List<Method> methodsWithJavadoc = allMethods.stream()
                    .filter(method -> method.getJavadoc() != null)
                    .collect(Collectors.toList());
            
            log.info("Filtered {} methods with Javadoc from {} total methods (skipped {} methods without Javadoc)", 
                     methodsWithJavadoc.size(), allMethods.size(), allMethods.size() - methodsWithJavadoc.size());
            
            if (methodsWithJavadoc.isEmpty()) {
                log.error("No methods with Javadoc found in the specified input paths");
                return;
            }
            
            // Process methods in batches
            BatchProcessor batchProcessor = new BatchProcessor(config);
            batchProcessor.processBatches(methodsWithJavadoc, guidelines);
            
            // Generate report
            XMLReportGenerator reportGenerator = new XMLReportGenerator(config);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String reportName = "javadoc_analysis_" + timestamp + ".xml";
            String reportPath = reportGenerator.generateReport(methodsWithJavadoc, config.getOutputPath(), reportName);
            
            if (reportPath != null) {
                log.info("Analysis complete. Report generated at: {}", reportPath);
            } else {
                log.error("Failed to generate report");
            }
            
        } catch (Exception e) {
            log.error("Error running documentation analyzer: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
    
    /**
     * Parses command line arguments.
     * 
     * @param args Command line arguments
     * @return Parsed command line arguments
     */
    private static CommandLineArgs parseCommandLineArgs(String[] args) {
        CommandLineArgs cmdArgs = new CommandLineArgs();
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            switch (arg) {
                case "-h":
                case "--help":
                    cmdArgs.setHelp(true);
                    break;
                    
                case "-c":
                case "--config":
                    if (i + 1 < args.length) {
                        cmdArgs.setConfigPath(args[++i]);
                    }
                    break;
                    
                case "-o":
                case "--output":
                    if (i + 1 < args.length) {
                        cmdArgs.setOutputPath(args[++i]);
                    }
                    break;
                    
                case "-m":
                case "--metrics":
                    if (i + 1 < args.length) {
                        cmdArgs.setMetricsPath(args[++i]);
                    }
                    break;
                    
                default:
                    // Assume it's an input path
                    cmdArgs.getInputPaths().add(arg);
                    break;
            }
        }
        
        return cmdArgs;
    }
    
    /**
     * Prints help information.
     */
    private static void printHelp() {
        System.out.println("Documentation Analyzer");
        System.out.println("Usage: java -jar docanalyzer.jar [options] [input paths]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --help                 Show this help message");
        System.out.println("  -c, --config <path>        Path to configuration file");
        System.out.println("  -o, --output <path>        Path to output directory");
        System.out.println("  -m, --metrics <path>       Path to metrics definitions file");
        System.out.println();
        System.out.println("Input paths can be Java files or directories containing Java files.");
    }
    
    /**
     * Command line arguments.
     */
    private static class CommandLineArgs {
        private boolean help = false;
        private String configPath;
        private String outputPath;
        private String metricsPath;
        private final List<String> inputPaths = new ArrayList<>();
        
        public boolean isHelp() {
            return help;
        }
        
        public void setHelp(boolean help) {
            this.help = help;
        }
        
        public String getConfigPath() {
            return configPath;
        }
        
        public void setConfigPath(String configPath) {
            this.configPath = configPath;
        }
        
        public String getOutputPath() {
            return outputPath;
        }
        
        public void setOutputPath(String outputPath) {
            this.outputPath = outputPath;
        }
        
        public String getMetricsPath() {
            return metricsPath;
        }
        
        public void setMetricsPath(String metricsPath) {
            this.metricsPath = metricsPath;
        }
        
        public List<String> getInputPaths() {
            return inputPaths;
        }
    }
}
