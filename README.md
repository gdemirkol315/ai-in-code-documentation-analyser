# AI-in-Code Documentation Analyzer

A tool that analyzes Javadoc documentation quality using AI (Anthropic's Claude) to rate specific metrics and provide improvement recommendations.

## Features

- Parses Java source files to extract methods and their Javadoc comments
- Analyzes documentation quality using Anthropic's Claude AI
- Evaluates documentation based on configurable metrics (completeness, clarity, code alignment, etc.)
- Processes methods in batches for efficient API usage
- Generates detailed XML reports with analysis results
- Supports customizable metrics and evaluation guidelines

## Requirements

- Java 11 or higher
- Maven
- Anthropic API key

## Setup

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/ai-in-code-documentation-analyser.git
   cd ai-in-code-documentation-analyser
   ```

2. Build the project:
   ```
   mvn clean package
   ```

3. Set your Anthropic API key:
   - Either set it as an environment variable:
     ```
     export ANTHROPIC_API_KEY=your_api_key_here
     ```
   - Or add it to the `src/main/resources/config.properties` file:
     ```
     anthropic.api.key=your_api_key_here
     ```

## Usage

### Basic Usage

```
java -jar target/ai-in-code-documentation-analyser-1.0-SNAPSHOT-jar-with-dependencies.jar [input paths]
```

Where `[input paths]` can be Java files or directories containing Java files.

### Command Line Options

```
java -jar target/ai-in-code-documentation-analyser-1.0-SNAPSHOT-jar-with-dependencies.jar [options] [input paths]
```

Options:
- `-h, --help`: Show help message
- `-c, --config <path>`: Path to configuration file
- `-o, --output <path>`: Path to output directory
- `-m, --metrics <path>`: Path to metrics definitions file

### Examples

Analyze a single Java file:
```
java -jar target/ai-in-code-documentation-analyser-1.0-SNAPSHOT-jar-with-dependencies.jar src/main/java/com/example/MyClass.java
```

Analyze all Java files in a directory:
```
java -jar target/ai-in-code-documentation-analyser-1.0-SNAPSHOT-jar-with-dependencies.jar src/main/java/com/example/
```

Analyze multiple files and directories:
```
java -jar target/ai-in-code-documentation-analyser-1.0-SNAPSHOT-jar-with-dependencies.jar src/main/java/com/example/MyClass.java src/main/java/com/example/utils/
```

Use a custom configuration file:
```
java -jar target/ai-in-code-documentation-analyser-1.0-SNAPSHOT-jar-with-dependencies.jar -c my-config.properties src/main/java/
```

Specify output directory:
```
java -jar target/ai-in-code-documentation-analyser-1.0-SNAPSHOT-jar-with-dependencies.jar -o reports/ src/main/java/
```

## Configuration

The tool can be configured using a properties file. The default configuration is in `src/main/resources/config.properties`.

### Configuration Options

```properties
# Anthropic API settings
anthropic.api.key=your_api_key_here
anthropic.model=claude-3-opus-20240229
anthropic.max.tokens=4096
anthropic.max.tokens.per.request=100000
anthropic.temperature=0.0

# Batch processing settings
batch.size=5

# File paths
metrics.definitions.path=src/main/resources/metrics-definitions.json
output.path=output
```

## Metrics Customization

Metrics are defined in a JSON file. The default metrics are in `src/main/resources/metrics-definitions.json`.

You can customize the metrics by creating your own metrics definitions file and specifying it with the `-m` option.

### Metrics Format

```json
{
  "metrics": [
    {
      "name": "Metric Name",
      "description": "Description of what this metric measures",
      "guidelines": {
        "1": "Guideline for score 1",
        "2": "Guideline for score 2",
        "3": "Guideline for score 3",
        "4": "Guideline for score 4",
        "5": "Guideline for score 5"
      },
      "weight": 1.0
    },
    // More metrics...
  ]
}
```

## Output

The tool generates an XML report with the analysis results. The report includes:

- Summary statistics (average scores for each metric)
- Detailed analysis for each method
- Recommendations for improving documentation

Example output:

```xml
<documentationAnalysis>
  <summary>
    <totalMethods>10</totalMethods>
    <averageOverallScore>3.5</averageOverallScore>
    <metricSummary name="Completeness">
      <averageScore>3.2</averageScore>
    </metricSummary>
    <metricSummary name="Clarity">
      <averageScore>3.8</averageScore>
    </metricSummary>
    <metricSummary name="Code Alignment">
      <averageScore>3.5</averageScore>
    </metricSummary>
  </summary>
  <file path="src/main/java/com/example/MyClass.java">
    <class name="MyClass">
      <method name="calculateTotal" signature="public int calculateTotal(int[] values)">
        <javadoc>
          /**
           * Calculates the sum of all values in the array.
           * @param values The array of integers to sum
           * @return The sum of all values
           */
        </javadoc>
        <metric name="Completeness" score="3">
          <guideline>Documents all parameters and returns but lacks exception handling or edge cases</guideline>
          <feedback>Documentation covers parameters and return values well, but lacks exception information.</feedback>
        </metric>
        <!-- More metrics... -->
        <overallScore>3.5</overallScore>
        <recommendations>
          <recommendation>Add @throws documentation for potential NullPointerException</recommendation>
          <recommendation>Mention behavior for empty arrays</recommendation>
        </recommendations>
      </method>
      <!-- More methods... -->
    </class>
  </file>
  <!-- More files... -->
</documentationAnalysis>
```

