@echo off
REM Script to run the Documentation Analyzer on Windows

REM Check if the JAR file exists
set JAR_FILE=target\ai-in-code-documentation-analyser-1.0-SNAPSHOT-jar-with-dependencies.jar
if not exist "%JAR_FILE%" (
    echo JAR file not found: %JAR_FILE%
    echo Building the project...
    call mvn clean package
    
    if not exist "%JAR_FILE%" (
        echo Failed to build the project. Please check the build logs.
        exit /b 1
    )
)

REM Check if ANTHROPIC_API_KEY is set
if "%API_KEY%"=="" (
    echo Warning: ANTHROPIC_API_KEY environment variable is not set.
    echo You can set it with: set ANTHROPIC_API_KEY=your_api_key_here
    echo Alternatively, you can add it to src\main\resources\config.properties
    echo.
)

REM Run the tool with the provided arguments
java -jar "%JAR_FILE%" %*

REM Check the exit code
if %ERRORLEVEL% neq 0 (
    echo Error running the Documentation Analyzer. Please check the logs.
    exit /b 1
)
