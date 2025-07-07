#!/bin/bash

# Script to run the Documentation Analyzer

# Check if the JAR file exists
JAR_FILE="target/ai-in-code-documentation-analyser-1.0-SNAPSHOT-jar-with-dependencies.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "JAR file not found: $JAR_FILE"
    echo "Building the project..."
    mvn clean package
    
    if [ ! -f "$JAR_FILE" ]; then
        echo "Failed to build the project. Please check the build logs."
        exit 1
    fi
fi

# Check if ANTHROPIC_API_KEY is set
if [ -z "$API_KEY" ]; then
    echo "Warning: ANTHROPIC_API_KEY environment variable is not set."
    echo "You can set it with: export ANTHROPIC_API_KEY=your_api_key_here"
    echo "Alternatively, you can add it to src/main/resources/config.properties"
    echo ""
fi

# Run the tool with the provided arguments
java -jar "$JAR_FILE" "$@"

# Check the exit code
if [ $? -ne 0 ]; then
    echo "Error running the Documentation Analyzer. Please check the logs."
    exit 1
fi
