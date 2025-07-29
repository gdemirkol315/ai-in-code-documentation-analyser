import pandas as pd
import numpy as np
from scipy.stats import spearmanr
import os

def load_human_data(filepath):
    """Load and process human survey data"""
    # Read the survey results
    df = pd.read_csv(filepath, sep='\t')
    
    # Get column names
    columns = df.columns.tolist()
    
    # Process each row (participant) and each method
    processed_data = []
    
    for participant_idx, row in df.iterrows():
        # Extract data for each question (method)
        for q_num in range(1, 12):  # Q1 to Q11
            comprehensibility = row[f'Q{q_num}_1']
            completeness = row[f'Q{q_num}_2'] 
            alignment = row[f'Q{q_num}_3']
            
            processed_data.append({
                'method_id': f'Q{q_num}',
                'participant_id': participant_idx,
                'group': 'human',
                'comprehensibility': comprehensibility,
                'completeness': completeness,
                'alignment': alignment
            })
    
    return pd.DataFrame(processed_data)

def load_ai_data(filepath):
    """Load and process AI tool data"""
    df = pd.read_csv(filepath, sep='\t')
    
    processed_data = []
    
    # Group by question number
    for q_num in range(1, 12):  # Q1 to Q11
        # Find rows for this question
        q_rows = df[df['Question'].str.startswith(f'Q{q_num}_')]
        
        if len(q_rows) == 3:  # Should have _1, _2, _3
            comprehensibility = q_rows[q_rows['Question'] == f'Q{q_num}_1']['AI_Result'].iloc[0]
            completeness = q_rows[q_rows['Question'] == f'Q{q_num}_2']['AI_Result'].iloc[0]
            alignment = q_rows[q_rows['Question'] == f'Q{q_num}_3']['AI_Result'].iloc[0]
            
            processed_data.append({
                'method_id': f'Q{q_num}',
                'group': 'ai',
                'comprehensibility': comprehensibility,
                'completeness': completeness,
                'alignment': alignment
            })
    
    return pd.DataFrame(processed_data)

def calculate_spearman_correlations(data, group_name):
    """Calculate Spearman correlations for a specific group"""
    print(f"\n=== {group_name.upper()} GROUP ANALYSIS ===")
    
    # Filter data for this group
    group_data = data[data['group'] == group_name].copy()
    
    if group_name == 'human':
        # For human data, we need to aggregate across participants
        # Calculate mean scores for each method across all participants
        group_data = group_data.groupby('method_id')[['comprehensibility', 'completeness', 'alignment']].mean().reset_index()
    
    print(f"Number of methods analyzed: {len(group_data)}")
    print(f"Data shape: {group_data.shape}")
    
    # Extract the three metrics
    comprehensibility = group_data['comprehensibility'].values
    completeness = group_data['completeness'].values
    alignment = group_data['alignment'].values
    
    # Calculate correlations
    correlations = []
    
    # Comprehensibility vs Completeness
    rho1, p1 = spearmanr(comprehensibility, completeness)
    correlations.append(('Comprehensibility vs Completeness', rho1, p1))
    
    # Comprehensibility vs Alignment
    rho2, p2 = spearmanr(comprehensibility, alignment)
    correlations.append(('Comprehensibility vs Alignment', rho2, p2))
    
    # Completeness vs Alignment
    rho3, p3 = spearmanr(completeness, alignment)
    correlations.append(('Completeness vs Alignment', rho3, p3))
    
    # Print results
    print(f"\nSpearman Correlation Results for {group_name.upper()}:")
    print("-" * 60)
    print(f"{'Metric Pair':<35} {'ρ (rho)':<12} {'p-value':<12} {'Significance'}")
    print("-" * 60)
    
    for metric_pair, rho, p_val in correlations:
        significance = "***" if p_val < 0.001 else "**" if p_val < 0.01 else "*" if p_val < 0.05 else "ns"
        print(f"{metric_pair:<35} {rho:>8.4f}    {p_val:>8.4f}    {significance}")
    
    print("\nSignificance levels: *** p<0.001, ** p<0.01, * p<0.05, ns = not significant")
    
    return correlations

def main():
    # File paths
    human_data_path = 'survey-result.txt'
    ai_data_path = 'output-simplified.txt'
    
    print("METRIC INDEPENDENCE ANALYSIS")
    print("=" * 50)
    print("Analyzing correlations between:")
    print("- Comprehensibility (metric 1)")
    print("- Completeness (metric 2)")  
    print("- Alignment (metric 3)")
    print("=" * 50)
    
    try:
        # Load data
        print("Loading human survey data...")
        human_data = load_human_data(human_data_path)
        
        print("Loading AI tool data...")
        ai_data = load_ai_data(ai_data_path)
        
        # Combine datasets
        all_data = pd.concat([human_data, ai_data], ignore_index=True)
        
        print(f"\nTotal data points loaded: {len(all_data)}")
        print(f"Human responses: {len(human_data)} (across {human_data['participant_id'].nunique()} participants)")
        print(f"AI responses: {len(ai_data)}")
        
        # Calculate correlations for each group
        human_correlations = calculate_spearman_correlations(all_data, 'human')
        ai_correlations = calculate_spearman_correlations(all_data, 'ai')
        
        # Summary comparison
        print("\n" + "=" * 60)
        print("SUMMARY COMPARISON")
        print("=" * 60)
        print(f"{'Metric Pair':<35} {'Human ρ':<12} {'AI ρ':<12} {'Difference'}")
        print("-" * 60)
        
        for i, (metric_pair, _, _) in enumerate(human_correlations):
            human_rho = human_correlations[i][1]
            ai_rho = ai_correlations[i][1]
            diff = abs(human_rho - ai_rho)
            print(f"{metric_pair:<35} {human_rho:>8.4f}    {ai_rho:>8.4f}    {diff:>8.4f}")
        
        print("\n" + "=" * 60)
        print("INTERPRETATION:")
        print("- High |ρ| (>0.7): Strong correlation - metrics may be dependent")
        print("- Medium |ρ| (0.3-0.7): Moderate correlation - some dependency")
        print("- Low |ρ| (<0.3): Weak correlation - metrics relatively independent")
        print("- p-value <0.05: Statistically significant correlation")
        print("=" * 60)
        
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
