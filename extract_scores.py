#!/usr/bin/env python3
"""
Script to extract scores from javadoc analysis XML output and format them
according to the specified format: Q[method number]_[metric number]-[score]

Metric mappings:
- Comprehensibility = 1
- Completeness = 2  
- Alignment = 3
"""

import xml.etree.ElementTree as ET
import sys
import os

def extract_scores(xml_file_path):
    """Extract scores from XML file and format them"""
    
    # Metric name to number mapping
    metric_mapping = {
        'Comprehensibility': 1,
        'Completeness': 2,
        'Alignment': 3
    }
    
    try:
        # Parse the XML file
        tree = ET.parse(xml_file_path)
        root = tree.getroot()
        
        # Find all method elements
        methods = root.findall('.//method')
        
        formatted_scores = []
        
        for method_index, method in enumerate(methods, 1):
            # Get the metrics results for this method
            metric_results = method.findall('.//metric-result')
            
            for metric_result in metric_results:
                metric_name = metric_result.get('name')
                score = metric_result.get('score')
                
                if metric_name in metric_mapping and score:
                    metric_number = metric_mapping[metric_name]
                    formatted_score = f"Q{method_index}_{metric_number}-{score}"
                    formatted_scores.append(formatted_score)
        
        return formatted_scores
        
    except ET.ParseError as e:
        print(f"Error parsing XML file: {e}")
        return []
    except FileNotFoundError:
        print(f"File not found: {xml_file_path}")
        return []
    except Exception as e:
        print(f"Unexpected error: {e}")
        return []

def main():
    # Default to the XML file in output directory
    xml_file = "output/javadoc_analysis_20250708_130833.xml"
    
    # Allow command line argument for different file
    if len(sys.argv) > 1:
        xml_file = sys.argv[1]
    
    if not os.path.exists(xml_file):
        print(f"Error: File '{xml_file}' not found")
        sys.exit(1)
    
    # Extract and format scores
    scores = extract_scores(xml_file)
    
    if scores:
        print("Extracted scores in required format:")
        print("=" * 40)
        for score in scores:
            print(score)
        print("=" * 40)
        print(f"Total scores extracted: {len(scores)}")
        
        # Also save to a file
        output_file = "formatted_scores.txt"
        with open(output_file, 'w') as f:
            for score in scores:
                f.write(score + '\n')
        print(f"Scores also saved to: {output_file}")
    else:
        print("No scores found or error occurred during extraction")

if __name__ == "__main__":
    main()
