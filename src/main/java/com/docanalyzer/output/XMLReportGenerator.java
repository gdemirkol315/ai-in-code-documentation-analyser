package com.docanalyzer.output;

import com.docanalyzer.model.Javadoc;
import com.docanalyzer.model.Method;
import com.docanalyzer.model.MetricsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Generates XML reports for Javadoc analysis results.
 */
public class XMLReportGenerator {
    
    private static final Logger log = LoggerFactory.getLogger(XMLReportGenerator.class);
    
    /**
     * Generates an XML report for the analyzed methods.
     * 
     * @param methods List of analyzed methods
     * @param outputPath Output directory path
     * @param reportName Name of the report file
     * @return Path to the generated report file, or null if generation failed
     */
    public String generateReport(List<Method> methods, String outputPath, String reportName) {
        if (methods == null || methods.isEmpty()) {
            log.warn("No methods provided for report generation");
            return null;
        }
        
        Path reportPath = Paths.get(outputPath, reportName);
        
        try (FileOutputStream fos = new FileOutputStream(reportPath.toFile())) {
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            XMLStreamWriter writer = factory.createXMLStreamWriter(fos, "UTF-8");
            
            // Start document
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeCharacters("\n");
            
            // Root element
            writer.writeStartElement("javadoc-analysis-report");
            writer.writeAttribute("generated-at", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            writer.writeAttribute("total-methods", String.valueOf(methods.size()));
            writer.writeCharacters("\n");
            
            // Summary section
            writeSummary(writer, methods);
            
            // Methods section
            writeMethods(writer, methods);
            
            // End root element
            writer.writeEndElement();
            writer.writeCharacters("\n");
            
            writer.writeEndDocument();
            writer.flush();
            writer.close();
            
            log.info("XML report generated successfully: {}", reportPath);
            return reportPath.toString();
            
        } catch (IOException | XMLStreamException e) {
            log.error("Failed to generate XML report: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Writes the summary section of the report.
     * 
     * @param writer XML stream writer
     * @param methods List of methods
     * @throws XMLStreamException if XML writing fails
     */
    private void writeSummary(XMLStreamWriter writer, List<Method> methods) throws XMLStreamException {
        writer.writeCharacters("  ");
        writer.writeStartElement("summary");
        writer.writeCharacters("\n");
        
        // Calculate summary statistics
        int totalMethods = methods.size();
        int methodsWithMetrics = (int) methods.stream()
                .filter(m -> m.getMetricsResult() != null)
                .count();
        
        double averageScore = methods.stream()
                .filter(m -> m.getMetricsResult() != null)
                .mapToDouble(m -> m.getMetricsResult().getOverallScore())
                .average()
                .orElse(0.0);
        
        // Write statistics
        writeSimpleElement(writer, "total-methods", String.valueOf(totalMethods), 4);
        writeSimpleElement(writer, "methods-with-metrics", String.valueOf(methodsWithMetrics), 4);
        writeSimpleElement(writer, "average-score", String.format("%.2f", averageScore), 4);
        
        // Score distribution
        writer.writeCharacters("    ");
        writer.writeStartElement("score-distribution");
        writer.writeCharacters("\n");
        
        int[] scoreRanges = new int[5]; // 1-2, 2-3, 3-4, 4-5, 5
        for (Method method : methods) {
            if (method.getMetricsResult() != null) {
                double score = method.getMetricsResult().getOverallScore();
                if (score >= 1.0 && score < 2.0) scoreRanges[0]++;
                else if (score >= 2.0 && score < 3.0) scoreRanges[1]++;
                else if (score >= 3.0 && score < 4.0) scoreRanges[2]++;
                else if (score >= 4.0 && score < 5.0) scoreRanges[3]++;
                else if (score >= 5.0) scoreRanges[4]++;
            }
        }
        
        writeSimpleElement(writer, "score-1-2", String.valueOf(scoreRanges[0]), 6);
        writeSimpleElement(writer, "score-2-3", String.valueOf(scoreRanges[1]), 6);
        writeSimpleElement(writer, "score-3-4", String.valueOf(scoreRanges[2]), 6);
        writeSimpleElement(writer, "score-4-5", String.valueOf(scoreRanges[3]), 6);
        writeSimpleElement(writer, "score-5", String.valueOf(scoreRanges[4]), 6);
        
        writer.writeCharacters("    ");
        writer.writeEndElement(); // score-distribution
        writer.writeCharacters("\n");
        
        writer.writeCharacters("  ");
        writer.writeEndElement(); // summary
        writer.writeCharacters("\n");
    }
    
    /**
     * Writes the methods section of the report.
     * 
     * @param writer XML stream writer
     * @param methods List of methods
     * @throws XMLStreamException if XML writing fails
     */
    private void writeMethods(XMLStreamWriter writer, List<Method> methods) throws XMLStreamException {
        writer.writeCharacters("  ");
        writer.writeStartElement("methods");
        writer.writeCharacters("\n");
        
        for (Method method : methods) {
            writeMethod(writer, method);
        }
        
        writer.writeCharacters("  ");
        writer.writeEndElement(); // methods
        writer.writeCharacters("\n");
    }
    
    /**
     * Writes a single method to the report.
     * 
     * @param writer XML stream writer
     * @param method Method to write
     * @throws XMLStreamException if XML writing fails
     */
    private void writeMethod(XMLStreamWriter writer, Method method) throws XMLStreamException {
        writer.writeCharacters("    ");
        writer.writeStartElement("method");
        writer.writeCharacters("\n");
        
        // Basic method information
        writeSimpleElement(writer, "name", method.getName(), 6);
        writeSimpleElement(writer, "class-name", method.getClassName(), 6);
        writeSimpleElement(writer, "package-name", method.getPackageName(), 6);
        writeSimpleElement(writer, "file-path", method.getFilePath(), 6);
        writeSimpleElement(writer, "signature", method.getSignature(), 6);
        writeSimpleElement(writer, "return-type", method.getReturnType(), 6);
        writeSimpleElement(writer, "start-line", String.valueOf(method.getStartLine()), 6);
        writeSimpleElement(writer, "end-line", String.valueOf(method.getEndLine()), 6);
        
        // Parameters
        if (!method.getParameterNames().isEmpty()) {
            writer.writeCharacters("      ");
            writer.writeStartElement("parameters");
            writer.writeCharacters("\n");
            
            for (int i = 0; i < method.getParameterNames().size(); i++) {
                writer.writeCharacters("        ");
                writer.writeStartElement("parameter");
                writer.writeAttribute("name", method.getParameterNames().get(i));
                if (i < method.getParameterTypes().size()) {
                    writer.writeAttribute("type", method.getParameterTypes().get(i));
                }
                writer.writeEndElement();
                writer.writeCharacters("\n");
            }
            
            writer.writeCharacters("      ");
            writer.writeEndElement(); // parameters
            writer.writeCharacters("\n");
        }
        
        // Javadoc
        if (method.getJavadoc() != null) {
            writeJavadoc(writer, method.getJavadoc());
        }
        
        // Metrics results
        if (method.getMetricsResult() != null) {
            writeMetricsResult(writer, method.getMetricsResult());
        }
        
        writer.writeCharacters("    ");
        writer.writeEndElement(); // method
        writer.writeCharacters("\n");
    }
    
    /**
     * Writes Javadoc information to the report.
     * 
     * @param writer XML stream writer
     * @param javadoc Javadoc to write
     * @throws XMLStreamException if XML writing fails
     */
    private void writeJavadoc(XMLStreamWriter writer, Javadoc javadoc) throws XMLStreamException {
        writer.writeCharacters("      ");
        writer.writeStartElement("javadoc");
        writer.writeCharacters("\n");
        
        if (javadoc.getDescription() != null && !javadoc.getDescription().trim().isEmpty()) {
            writeSimpleElement(writer, "description", javadoc.getDescription(), 8);
        }
        
        if (javadoc.getReturnTag() != null && !javadoc.getReturnTag().trim().isEmpty()) {
            writeSimpleElement(writer, "return-tag", javadoc.getReturnTag(), 8);
        }
        
        // Parameter tags
        if (!javadoc.getParamTags().isEmpty()) {
            writer.writeCharacters("        ");
            writer.writeStartElement("param-tags");
            writer.writeCharacters("\n");
            
            for (Javadoc.ParamTag paramTag : javadoc.getParamTags()) {
                writer.writeCharacters("          ");
                writer.writeStartElement("param-tag");
                writer.writeAttribute("name", paramTag.getParameterName());
                writer.writeCharacters("\n");
                
                if (paramTag.getDescription() != null && !paramTag.getDescription().trim().isEmpty()) {
                    writeSimpleElement(writer, "description", paramTag.getDescription(), 12);
                }
                
                writer.writeCharacters("          ");
                writer.writeEndElement(); // param-tag
                writer.writeCharacters("\n");
            }
            
            writer.writeCharacters("        ");
            writer.writeEndElement(); // param-tags
            writer.writeCharacters("\n");
        }
        
        // Throws tags
        if (!javadoc.getThrowsTags().isEmpty()) {
            writer.writeCharacters("        ");
            writer.writeStartElement("throws-tags");
            writer.writeCharacters("\n");
            
            for (Javadoc.ThrowsTag throwsTag : javadoc.getThrowsTags()) {
                writer.writeCharacters("          ");
                writer.writeStartElement("throws-tag");
                writer.writeAttribute("exception-type", throwsTag.getExceptionType());
                writer.writeCharacters("\n");
                
                if (throwsTag.getDescription() != null && !throwsTag.getDescription().trim().isEmpty()) {
                    writeSimpleElement(writer, "description", throwsTag.getDescription(), 12);
                }
                
                writer.writeCharacters("          ");
                writer.writeEndElement(); // throws-tag
                writer.writeCharacters("\n");
            }
            
            writer.writeCharacters("        ");
            writer.writeEndElement(); // throws-tags
            writer.writeCharacters("\n");
        }
        
        // Other tags
        if (!javadoc.getOtherTags().isEmpty()) {
            writer.writeCharacters("        ");
            writer.writeStartElement("other-tags");
            writer.writeCharacters("\n");
            
            for (Javadoc.OtherTag otherTag : javadoc.getOtherTags()) {
                writer.writeCharacters("          ");
                writer.writeStartElement("other-tag");
                writer.writeAttribute("name", otherTag.getTagName());
                writer.writeCharacters("\n");
                
                if (otherTag.getContent() != null && !otherTag.getContent().trim().isEmpty()) {
                    writeSimpleElement(writer, "content", otherTag.getContent(), 12);
                }
                
                writer.writeCharacters("          ");
                writer.writeEndElement(); // other-tag
                writer.writeCharacters("\n");
            }
            
            writer.writeCharacters("        ");
            writer.writeEndElement(); // other-tags
            writer.writeCharacters("\n");
        }
        
        if (javadoc.getRawText() != null && !javadoc.getRawText().trim().isEmpty()) {
            writeSimpleElement(writer, "raw-text", javadoc.getRawText(), 8);
        }
        
        writer.writeCharacters("      ");
        writer.writeEndElement(); // javadoc
        writer.writeCharacters("\n");
    }
    
    /**
     * Writes metrics results to the report.
     * 
     * @param writer XML stream writer
     * @param metricsResult Metrics result to write
     * @throws XMLStreamException if XML writing fails
     */
    private void writeMetricsResult(XMLStreamWriter writer, MetricsResult metricsResult) throws XMLStreamException {
        writer.writeCharacters("      ");
        writer.writeStartElement("metrics-result");
        writer.writeCharacters("\n");
        
        writeSimpleElement(writer, "overall-score", String.format("%.2f", metricsResult.getOverallScore()), 8);
        
        // Individual metric results
        if (!metricsResult.getMetricResults().isEmpty()) {
            writer.writeCharacters("        ");
            writer.writeStartElement("metric-results");
            writer.writeCharacters("\n");
            
            for (Map.Entry<String, MetricsResult.MetricResult> entry : metricsResult.getMetricResults().entrySet()) {
                MetricsResult.MetricResult metricResult = entry.getValue();
                
                writer.writeCharacters("          ");
                writer.writeStartElement("metric-result");
                writer.writeAttribute("name", metricResult.getName());
                writer.writeAttribute("score", String.valueOf(metricResult.getScore()));
                writer.writeCharacters("\n");
                
                if (metricResult.getGuideline() != null && !metricResult.getGuideline().trim().isEmpty()) {
                    writeSimpleElement(writer, "guideline", metricResult.getGuideline(), 12);
                }
                
                if (metricResult.getFeedback() != null && !metricResult.getFeedback().trim().isEmpty()) {
                    writeSimpleElement(writer, "feedback", metricResult.getFeedback(), 12);
                }
                
                writer.writeCharacters("          ");
                writer.writeEndElement(); // metric-result
                writer.writeCharacters("\n");
            }
            
            writer.writeCharacters("        ");
            writer.writeEndElement(); // metric-results
            writer.writeCharacters("\n");
        }
        
        // Recommendations
        if (!metricsResult.getRecommendations().isEmpty()) {
            writer.writeCharacters("        ");
            writer.writeStartElement("recommendations");
            writer.writeCharacters("\n");
            
            for (String recommendation : metricsResult.getRecommendations()) {
                writeSimpleElement(writer, "recommendation", recommendation, 10);
            }
            
            writer.writeCharacters("        ");
            writer.writeEndElement(); // recommendations
            writer.writeCharacters("\n");
        }
        
        writer.writeCharacters("      ");
        writer.writeEndElement(); // metrics-result
        writer.writeCharacters("\n");
    }
    
    /**
     * Writes a simple XML element with text content.
     * 
     * @param writer XML stream writer
     * @param elementName Name of the element
     * @param content Text content
     * @param indentLevel Indentation level (number of spaces)
     * @throws XMLStreamException if XML writing fails
     */
    private void writeSimpleElement(XMLStreamWriter writer, String elementName, String content, int indentLevel) throws XMLStreamException {
        if (content == null) {
            content = "";
        }
        
        writer.writeCharacters(" ".repeat(indentLevel));
        writer.writeStartElement(elementName);
        writer.writeCharacters(escapeXmlContent(content));
        writer.writeEndElement();
        writer.writeCharacters("\n");
    }
    
    /**
     * Writes an XML element with CDATA content.
     * 
     * @param writer XML stream writer
     * @param elementName Name of the element
     * @param content Text content (will be wrapped in CDATA)
     * @param indentLevel Indentation level (number of spaces)
     * @throws XMLStreamException if XML writing fails
     */
    private void writeCDataElement(XMLStreamWriter writer, String elementName, String content, int indentLevel) throws XMLStreamException {
        if (content == null || content.trim().isEmpty()) {
            return;
        }
        
        writer.writeCharacters(" ".repeat(indentLevel));
        writer.writeStartElement(elementName);
        writer.writeCData(content);
        writer.writeEndElement();
        writer.writeCharacters("\n");
    }
    
    /**
     * Escapes XML content to prevent parsing errors.
     * 
     * @param content Content to escape
     * @return Escaped content
     */
    private String escapeXmlContent(String content) {
        if (content == null) {
            return "";
        }
        
        return content
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
