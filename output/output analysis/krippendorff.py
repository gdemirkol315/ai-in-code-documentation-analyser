import krippendorff
import numpy as np
from datetime import datetime
from scipy import stats

def load_survey_data(filename):
    """
    Load survey data from a tab-separated file.
    Returns the data and headers.
    """
    with open(filename, 'r') as file:
        lines = file.readlines()
    
    # Get headers
    headers = lines[0].strip().split('\t')
    
    # Skip the header line and process the data
    data_lines = lines[1:]  # Skip header
    
    # Parse each line (tab-separated values)
    data = []
    for line in data_lines:
        values = line.strip().split('\t')
        # Convert to integers
        row = [int(val) for val in values if val.strip()]
        data.append(row)
    
    return data, headers

def calculate_item_agreement(responses):
    """
    Calculate agreement for a single item using multiple approaches.
    Returns a dictionary with different agreement measures.
    """
    if len(responses) <= 1:
        return {'variance_agreement': 1.0, 'range_agreement': 1.0, 'mode_agreement': 1.0}
    
    responses = np.array(responses)
    
    # Method 1: Variance-based agreement (lower variance = higher agreement)
    variance = np.var(responses)
    max_possible_variance = 2.0  # For 5-point scale, reasonable max variance
    variance_agreement = max(0, 1 - (variance / max_possible_variance))
    
    # Method 2: Range-based agreement (smaller range = higher agreement)
    response_range = np.max(responses) - np.min(responses)
    max_possible_range = 4  # For 5-point scale (5-1=4)
    range_agreement = max(0, 1 - (response_range / max_possible_range))
    
    # Method 3: Mode-based agreement (more responses at mode = higher agreement)
    from collections import Counter
    counts = Counter(responses)
    mode_count = counts.most_common(1)[0][1]
    mode_agreement = mode_count / len(responses)
    
    return {
        'variance_agreement': variance_agreement,
        'range_agreement': range_agreement,
        'mode_agreement': mode_agreement
    }

def calculate_composite_agreement(responses):
    """
    Calculate a composite agreement score combining multiple measures.
    """
    agreements = calculate_item_agreement(responses)
    
    # Weighted average of different agreement measures
    composite = (
        0.4 * agreements['variance_agreement'] +
        0.3 * agreements['range_agreement'] +
        0.3 * agreements['mode_agreement']
    )
    
    # Convert to alpha-like scale (approximate)
    # Scale so that perfect agreement (1.0) maps to ~0.9 alpha
    # and no agreement (0.0) maps to ~0.0 alpha
    alpha_like = composite * 0.9
    
    return alpha_like

def generate_comprehensive_report(data, headers, output_file='krippendorff_comprehensive_report.txt'):
    """
    Generate a comprehensive reliability analysis report.
    """
    report_lines = []
    
    # Header
    report_lines.append("="*90)
    report_lines.append("COMPREHENSIVE INTER-RATER RELIABILITY ANALYSIS")
    report_lines.append("KRIPPENDORFF'S ALPHA (ORDINAL) + ITEM-LEVEL AGREEMENT ANALYSIS")
    report_lines.append("="*90)
    report_lines.append(f"Generated on: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    report_lines.append("")
    
    # Data summary
    report_lines.append("DATA SUMMARY:")
    report_lines.append(f"Number of raters: {len(data)}")
    report_lines.append(f"Number of items: {len(headers)}")
    report_lines.append("")
    
    # Overall reliability using Krippendorff's alpha
    overall_alpha = krippendorff.alpha(reliability_data=data, level_of_measurement='ordinal')
    report_lines.append("OVERALL RELIABILITY (ALL ITEMS COMBINED):")
    report_lines.append(f"Krippendorff's alpha (ordinal): {overall_alpha:.4f}")
    
    if overall_alpha >= 0.800:
        interpretation = "ACCEPTABLE"
        symbol = "✓"
    elif overall_alpha >= 0.667:
        interpretation = "TENTATIVE"
        symbol = "⚠"
    else:
        interpretation = "INSUFFICIENT"
        symbol = "✗"
    
    report_lines.append(f"{symbol} Overall reliability: {interpretation}")
    report_lines.append("")
    
    # Item-by-item analysis
    report_lines.append("ITEM-BY-ITEM AGREEMENT ANALYSIS:")
    report_lines.append("="*90)
    report_lines.append("Note: For individual items, we use agreement measures since Krippendorff's alpha")
    report_lines.append("requires multiple units. Agreement scores are scaled to be comparable to alpha values.")
    report_lines.append("")
    
    item_agreements = []
    high_agreement_items = 0
    moderate_agreement_items = 0
    low_agreement_items = 0
    
    for i, header in enumerate(headers):
        # Get responses for this item from all raters
        item_responses = [rater[i] for rater in data]
        
        # Calculate basic statistics
        mean_rating = np.mean(item_responses)
        std_rating = np.std(item_responses)
        min_rating = min(item_responses)
        max_rating = max(item_responses)
        
        # Calculate agreement measures
        agreements = calculate_item_agreement(item_responses)
        composite_agreement = calculate_composite_agreement(item_responses)
        item_agreements.append(composite_agreement)
        
        # Categorize agreement level
        if composite_agreement >= 0.700:
            agreement_status = "HIGH"
            symbol = "✓"
            high_agreement_items += 1
        elif composite_agreement >= 0.500:
            agreement_status = "MODERATE"
            symbol = "⚠"
            moderate_agreement_items += 1
        else:
            agreement_status = "LOW"
            symbol = "✗"
            low_agreement_items += 1
        
        # Calculate response distribution
        unique_responses = sorted(set(item_responses))
        response_dist = {resp: item_responses.count(resp) for resp in unique_responses}
        
        report_lines.append(f"Item {i+1:2d}: {header}")
        report_lines.append(f"  Responses: {item_responses}")
        report_lines.append(f"  Statistics: Mean={mean_rating:.2f}, SD={std_rating:.2f}, Range=[{min_rating}-{max_rating}]")
        report_lines.append(f"  Distribution: {response_dist}")
        report_lines.append(f"  Agreement Measures:")
        report_lines.append(f"    - Variance-based:  {agreements['variance_agreement']:.3f}")
        report_lines.append(f"    - Range-based:     {agreements['range_agreement']:.3f}")
        report_lines.append(f"    - Mode-based:      {agreements['mode_agreement']:.3f}")
        report_lines.append(f"  Composite Agreement: {composite_agreement:.4f} {symbol} {agreement_status}")
        report_lines.append("")
    
    # Summary statistics
    report_lines.append("SUMMARY STATISTICS:")
    report_lines.append("="*50)
    report_lines.append(f"Items with high agreement (≥ 0.700):     {high_agreement_items:2d} ({high_agreement_items/len(headers)*100:.1f}%)")
    report_lines.append(f"Items with moderate agreement (≥ 0.500): {moderate_agreement_items:2d} ({moderate_agreement_items/len(headers)*100:.1f}%)")
    report_lines.append(f"Items with low agreement (< 0.500):      {low_agreement_items:2d} ({low_agreement_items/len(headers)*100:.1f}%)")
    report_lines.append("")
    report_lines.append(f"Average item agreement: {np.mean(item_agreements):.4f}")
    report_lines.append(f"Agreement range: {min(item_agreements):.4f} - {max(item_agreements):.4f}")
    report_lines.append(f"Standard deviation of agreements: {np.std(item_agreements):.4f}")
    
    # Items with highest and lowest agreement
    sorted_items = sorted(enumerate(item_agreements), key=lambda x: x[1], reverse=True)
    
    report_lines.append("")
    report_lines.append("TOP 5 ITEMS WITH HIGHEST AGREEMENT:")
    for rank, (item_idx, agreement) in enumerate(sorted_items[:5], 1):
        responses = [rater[item_idx] for rater in data]
        report_lines.append(f"{rank}. {headers[item_idx]} (Agreement: {agreement:.4f}) - Responses: {responses}")
    
    report_lines.append("")
    report_lines.append("TOP 5 ITEMS WITH LOWEST AGREEMENT:")
    for rank, (item_idx, agreement) in enumerate(sorted_items[-5:], 1):
        responses = [rater[item_idx] for rater in data]
        report_lines.append(f"{rank}. {headers[item_idx]} (Agreement: {agreement:.4f}) - Responses: {responses}")
    
    report_lines.append("")
    report_lines.append("INTERPRETATION GUIDELINES:")
    report_lines.append("="*50)
    report_lines.append("Overall Krippendorff's Alpha:")
    report_lines.append("  α ≥ 0.800: Acceptable reliability")
    report_lines.append("  α ≥ 0.667: Tentative conclusions possible")
    report_lines.append("  α < 0.667: Insufficient reliability")
    report_lines.append("")
    report_lines.append("Item-level Agreement Scores:")
    report_lines.append("  ≥ 0.700: High agreement")
    report_lines.append("  ≥ 0.500: Moderate agreement")
    report_lines.append("  < 0.500: Low agreement")
    
    # Write to file
    with open(output_file, 'w') as f:
        f.write('\n'.join(report_lines))
    
    return report_lines

def main():
    # Load the survey data
    print("Loading survey data...")
    data, headers = load_survey_data('output analysis/survey-result.txt')
    
    print(f"Loaded {len(data)} raters and {len(headers)} items")
    
    # Generate comprehensive report
    print("Generating comprehensive analysis report...")
    report_lines = generate_comprehensive_report(data, headers)
    
    # Print summary to console
    print("\n" + "="*70)
    print("RELIABILITY ANALYSIS SUMMARY")
    print("="*70)
    
    # Overall reliability
    overall_alpha = krippendorff.alpha(reliability_data=data, level_of_measurement='ordinal')
    print(f"Overall Krippendorff's alpha (ordinal): {overall_alpha:.4f}")
    
    if overall_alpha >= 0.800:
        print("✓ Overall reliability: ACCEPTABLE")
    elif overall_alpha >= 0.667:
        print("⚠ Overall reliability: TENTATIVE")
    else:
        print("✗ Overall reliability: INSUFFICIENT")
    
    # Item agreement summary
    item_agreements = []
    for i in range(len(headers)):
        item_responses = [rater[i] for rater in data]
        agreement = calculate_composite_agreement(item_responses)
        item_agreements.append(agreement)
    
    high_agreement = sum(1 for a in item_agreements if a >= 0.700)
    moderate_agreement = sum(1 for a in item_agreements if 0.500 <= a < 0.700)
    low_agreement = sum(1 for a in item_agreements if a < 0.500)
    
    print(f"\nItem-level Agreement Distribution:")
    print(f"  High agreement (≥0.700):     {high_agreement:2d} items ({high_agreement/len(headers)*100:.1f}%)")
    print(f"  Moderate agreement (≥0.500): {moderate_agreement:2d} items ({moderate_agreement/len(headers)*100:.1f}%)")
    print(f"  Low agreement (<0.500):      {low_agreement:2d} items ({low_agreement/len(headers)*100:.1f}%)")
    print(f"  Average item agreement:      {np.mean(item_agreements):.4f}")
    
    print(f"\nDetailed report saved to: krippendorff_comprehensive_report.txt")

if __name__ == "__main__":
    main()
