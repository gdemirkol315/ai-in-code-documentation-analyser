#!/usr/bin/env python3
"""
AI vs Developer Ratings Comparison Analysis
Analyzes and compares AI ratings with human developer ratings across comprehensibility, completeness, and alignment metrics.
"""

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path
import warnings
from scipy.stats import pearsonr, spearmanr
warnings.filterwarnings('ignore')

def load_survey_data():
    """Load and process survey data from CSV file."""
    try:
        # Load the survey data
        df = pd.read_csv('survey-result.csv', sep='\t')
        print(f"Loaded survey data with {len(df)} responses and {len(df.columns)} questions")
        return df
    except FileNotFoundError:
        print("Error: survey-result.csv not found")
        return None
    except Exception as e:
        print(f"Error loading survey data: {e}")
        return None

def load_simplified_output():
    """Load AI ratings and developer averages from output-simplified.txt."""
    try:
        df = pd.read_csv('output-simplified.txt', sep='\t')
        print(f"Loaded simplified output with {len(df)} question-metric combinations")
        return df
    except FileNotFoundError:
        print("Error: output-simplified.txt not found")
        return None
    except Exception as e:
        print(f"Error loading simplified output: {e}")
        return None

def organize_comparison_data(simplified_df):
    """Organize data for AI vs Developer comparison by metric type."""
    comparison_data = {
        'comprehensibility': {'questions': [], 'ai_ratings': [], 'dev_ratings': []},
        'completeness': {'questions': [], 'ai_ratings': [], 'dev_ratings': []},
        'alignment': {'questions': [], 'ai_ratings': [], 'dev_ratings': []}
    }
    
    metric_mapping = {'1': 'comprehensibility', '2': 'completeness', '3': 'alignment'}
    
    for _, row in simplified_df.iterrows():
        question = row['Question']
        # Extract question number and metric type (e.g., Q1_1 -> Q1, 1)
        q_parts = question.split('_')
        q_num = q_parts[0]  # Q1, Q2, etc.
        metric_num = q_parts[1]  # 1, 2, 3
        
        metric_type = metric_mapping[metric_num]
        
        comparison_data[metric_type]['questions'].append(q_num)
        comparison_data[metric_type]['ai_ratings'].append(row['AI_Result'])
        comparison_data[metric_type]['dev_ratings'].append(row['Average_Developer_Result'])
    
    return comparison_data

def create_ai_vs_developer_comparison(comparison_data):
    """Create side-by-side bar charts comparing AI vs Developer ratings by metric."""
    fig, axes = plt.subplots(1, 3, figsize=(18, 6))
    fig.suptitle('AI vs Developer Ratings Comparison by Metric Type', fontsize=16, fontweight='bold')
    
    metrics = ['comprehensibility', 'completeness', 'alignment']
    colors = [['#FF6B6B', '#4ECDC4'], ['#45B7D1', '#96CEB4'], ['#FECA57', '#FF9FF3']]
    
    for i, metric in enumerate(metrics):
        data = comparison_data[metric]
        questions = data['questions']
        ai_ratings = data['ai_ratings']
        dev_ratings = data['dev_ratings']
        
        x = np.arange(len(questions))
        width = 0.35
        
        bars1 = axes[i].bar(x - width/2, ai_ratings, width, label='AI Ratings', 
                           color=colors[i][0], alpha=0.8, edgecolor='black', linewidth=0.5)
        bars2 = axes[i].bar(x + width/2, dev_ratings, width, label='Developer Avg', 
                           color=colors[i][1], alpha=0.8, edgecolor='black', linewidth=0.5)
        
        axes[i].set_title(f'{metric.capitalize()}', fontsize=14, fontweight='bold')
        axes[i].set_xlabel('Questions')
        axes[i].set_ylabel('Rating (1-5)')
        axes[i].set_xticks(x)
        axes[i].set_xticklabels(questions, rotation=45)
        axes[i].set_ylim(0, 5.5)
        axes[i].legend()
        axes[i].grid(True, alpha=0.3, axis='y')
        
        # Add value labels on bars
        for bar in bars1:
            height = bar.get_height()
            axes[i].text(bar.get_x() + bar.get_width()/2., height + 0.05,
                        f'{height:.1f}', ha='center', va='bottom', fontsize=8)
        
        for bar in bars2:
            height = bar.get_height()
            axes[i].text(bar.get_x() + bar.get_width()/2., height + 0.05,
                        f'{height:.1f}', ha='center', va='bottom', fontsize=8)
    
    plt.tight_layout()
    plt.savefig('ai_vs_developer_comparison.png', dpi=300, bbox_inches='tight')
    plt.show()
    print("AI vs Developer comparison plots saved as 'ai_vs_developer_comparison.png'")

def create_scatter_comparison(comparison_data):
    """Create scatter plots showing AI vs Developer rating correlations."""
    fig, axes = plt.subplots(2, 2, figsize=(12, 10))
    fig.suptitle('AI vs Developer Rating Agreement Analysis', fontsize=16, fontweight='bold')
    
    # Individual metric scatter plots
    metrics = ['comprehensibility', 'completeness', 'alignment']
    colors = ['#FF6B6B', '#45B7D1', '#FECA57']
    positions = [(0, 0), (0, 1), (1, 0)]
    
    all_ai_ratings = []
    all_dev_ratings = []
    all_metric_labels = []
    
    for i, metric in enumerate(metrics):
        data = comparison_data[metric]
        ai_ratings = data['ai_ratings']
        dev_ratings = data['dev_ratings']
        
        # Store for combined plot
        all_ai_ratings.extend(ai_ratings)
        all_dev_ratings.extend(dev_ratings)
        all_metric_labels.extend([metric.capitalize()] * len(ai_ratings))
        
        row, col = positions[i]
        axes[row, col].scatter(ai_ratings, dev_ratings, color=colors[i], alpha=0.7, s=60, edgecolors='black')
        axes[row, col].plot([1, 5], [1, 5], 'k--', alpha=0.5, label='Perfect Agreement')
        axes[row, col].set_title(f'{metric.capitalize()}')
        axes[row, col].set_xlabel('AI Ratings')
        axes[row, col].set_ylabel('Developer Average Ratings')
        axes[row, col].set_xlim(0.5, 5.5)
        axes[row, col].set_ylim(0.5, 5.5)
        axes[row, col].grid(True, alpha=0.3)
        axes[row, col].legend()
        
        # Calculate and display correlation
        corr, p_value = pearsonr(ai_ratings, dev_ratings)
        axes[row, col].text(0.05, 0.95, f'r = {corr:.3f}\np = {p_value:.3f}', 
                           transform=axes[row, col].transAxes, verticalalignment='top',
                           bbox=dict(boxstyle='round', facecolor='white', alpha=0.8))
    
    # Combined scatter plot
    ax_combined = axes[1, 1]
    for i, metric in enumerate(metrics):
        data = comparison_data[metric]
        ai_ratings = data['ai_ratings']
        dev_ratings = data['dev_ratings']
        ax_combined.scatter(ai_ratings, dev_ratings, color=colors[i], alpha=0.7, 
                           s=60, edgecolors='black', label=metric.capitalize())
    
    ax_combined.plot([1, 5], [1, 5], 'k--', alpha=0.5, label='Perfect Agreement')
    ax_combined.set_title('All Metrics Combined')
    ax_combined.set_xlabel('AI Ratings')
    ax_combined.set_ylabel('Developer Average Ratings')
    ax_combined.set_xlim(0.5, 5.5)
    ax_combined.set_ylim(0.5, 5.5)
    ax_combined.grid(True, alpha=0.3)
    ax_combined.legend()
    
    # Overall correlation
    overall_corr, overall_p = pearsonr(all_ai_ratings, all_dev_ratings)
    ax_combined.text(0.05, 0.95, f'Overall r = {overall_corr:.3f}\np = {overall_p:.3f}', 
                    transform=ax_combined.transAxes, verticalalignment='top',
                    bbox=dict(boxstyle='round', facecolor='white', alpha=0.8))
    
    plt.tight_layout()
    plt.savefig('rating_agreement_analysis.png', dpi=300, bbox_inches='tight')
    plt.show()
    print("Rating agreement analysis saved as 'rating_agreement_analysis.png'")

def create_difference_analysis(comparison_data):
    """Create visualization showing differences between AI and Developer ratings."""
    fig, axes = plt.subplots(2, 2, figsize=(15, 10))
    fig.suptitle('AI vs Developer Rating Differences Analysis', fontsize=16, fontweight='bold')
    
    metrics = ['comprehensibility', 'completeness', 'alignment']
    colors = ['#FF6B6B', '#45B7D1', '#FECA57']
    
    # Calculate differences for each metric
    all_differences = []
    all_questions = []
    all_metrics = []
    
    # Individual metric difference plots
    for i, metric in enumerate(metrics):
        data = comparison_data[metric]
        questions = data['questions']
        differences = np.array(data['ai_ratings']) - np.array(data['dev_ratings'])
        
        all_differences.extend(differences)
        all_questions.extend(questions)
        all_metrics.extend([metric.capitalize()] * len(differences))
        
        if i < 2:  # First row
            ax = axes[0, i]
        else:  # Second row, first column
            ax = axes[1, 0]
        
        bars = ax.barh(questions, differences, color=colors[i], alpha=0.7, edgecolor='black')
        ax.set_title(f'{metric.capitalize()} - Differences (AI - Developer)')
        ax.set_xlabel('Rating Difference')
        ax.axvline(x=0, color='black', linestyle='-', alpha=0.5)
        ax.grid(True, alpha=0.3, axis='x')
        
        # Color bars based on positive/negative differences
        for bar, diff in zip(bars, differences):
            if diff > 0:
                bar.set_color('#FF6B6B')  # Red for AI higher
            elif diff < 0:
                bar.set_color('#4ECDC4')  # Teal for Developer higher
            else:
                bar.set_color('#95A5A6')  # Gray for equal
        
        # Add value labels
        for bar, diff in zip(bars, differences):
            width = bar.get_width()
            ax.text(width + (0.05 if width >= 0 else -0.05), bar.get_y() + bar.get_height()/2,
                   f'{diff:.1f}', ha='left' if width >= 0 else 'right', va='center', fontsize=8)
    
    # Heatmap of differences
    ax_heatmap = axes[1, 1]
    
    # Create matrix for heatmap
    questions_list = [f'Q{i}' for i in range(1, 12)]
    diff_matrix = np.zeros((len(metrics), len(questions_list)))
    
    for i, metric in enumerate(metrics):
        data = comparison_data[metric]
        differences = np.array(data['ai_ratings']) - np.array(data['dev_ratings'])
        diff_matrix[i, :] = differences
    
    im = ax_heatmap.imshow(diff_matrix, cmap='RdBu_r', aspect='auto', vmin=-2, vmax=2)
    ax_heatmap.set_xticks(range(len(questions_list)))
    ax_heatmap.set_xticklabels(questions_list)
    ax_heatmap.set_yticks(range(len(metrics)))
    ax_heatmap.set_yticklabels([m.capitalize() for m in metrics])
    ax_heatmap.set_title('Difference Heatmap\n(Red: AI Higher, Blue: Developer Higher)')
    
    # Add text annotations
    for i in range(len(metrics)):
        for j in range(len(questions_list)):
            text = ax_heatmap.text(j, i, f'{diff_matrix[i, j]:.1f}',
                                 ha="center", va="center", color="black", fontsize=8)
    
    # Add colorbar
    cbar = plt.colorbar(im, ax=ax_heatmap, shrink=0.8)
    cbar.set_label('Rating Difference (AI - Developer)')
    
    plt.tight_layout()
    plt.savefig('difference_analysis.png', dpi=300, bbox_inches='tight')
    plt.show()
    print("Difference analysis saved as 'difference_analysis.png'")

def calculate_comprehensive_statistics(comparison_data):
    """Calculate comprehensive statistics for AI vs Developer comparison."""
    stats = {}
    
    for metric in ['comprehensibility', 'completeness', 'alignment']:
        data = comparison_data[metric]
        ai_ratings = np.array(data['ai_ratings'])
        dev_ratings = np.array(data['dev_ratings'])
        differences = ai_ratings - dev_ratings
        
        # Correlation statistics
        pearson_corr, pearson_p = pearsonr(ai_ratings, dev_ratings)
        spearman_corr, spearman_p = spearmanr(ai_ratings, dev_ratings)
        
        # Difference statistics
        mean_diff = np.mean(differences)
        std_diff = np.std(differences, ddof=1)
        mean_abs_diff = np.mean(np.abs(differences))
        
        # Agreement statistics
        exact_agreement = np.sum(np.abs(differences) < 0.1)  # Within 0.1 points
        close_agreement = np.sum(np.abs(differences) <= 0.5)  # Within 0.5 points
        
        stats[metric] = {
            'ai_mean': np.mean(ai_ratings),
            'ai_std': np.std(ai_ratings, ddof=1),
            'dev_mean': np.mean(dev_ratings),
            'dev_std': np.std(dev_ratings, ddof=1),
            'pearson_corr': pearson_corr,
            'pearson_p': pearson_p,
            'spearman_corr': spearman_corr,
            'spearman_p': spearman_p,
            'mean_difference': mean_diff,
            'std_difference': std_diff,
            'mean_abs_difference': mean_abs_diff,
            'exact_agreement_count': exact_agreement,
            'close_agreement_count': close_agreement,
            'total_comparisons': len(ai_ratings)
        }
    
    return stats

def create_human_response_distribution_stacked_bars(survey_df):
    """Create stacked bar charts showing distribution of human responses for each question."""
    # Organize data by metric type
    metrics = ['comprehensibility', 'completeness', 'alignment']
    metric_suffixes = ['_1', '_2', '_3']
    
    fig, axes = plt.subplots(1, 3, figsize=(20, 8))
    fig.suptitle('Human Response Distribution by Question and Metric', fontsize=16, fontweight='bold')
    
    # Color scheme for ratings 1-5
    colors = ['#d32f2f', '#ff9800', '#ffc107', '#8bc34a', '#4caf50']  # Red to Green
    rating_labels = ['Rating 1', 'Rating 2', 'Rating 3', 'Rating 4', 'Rating 5']
    
    for metric_idx, (metric, suffix) in enumerate(zip(metrics, metric_suffixes)):
        ax = axes[metric_idx]
        
        # Get questions for this metric (Q1_1, Q2_1, etc.)
        question_cols = [col for col in survey_df.columns if col.endswith(suffix)]
        question_cols.sort(key=lambda x: int(x.split('_')[0][1:]))  # Sort by question number
        
        questions = [col.split('_')[0] for col in question_cols]  # Extract Q1, Q2, etc.
        
        # Count frequency of each rating (1-5) for each question
        rating_counts = {rating: [] for rating in range(1, 6)}
        
        for col in question_cols:
            responses = survey_df[col].values
            for rating in range(1, 6):
                count = np.sum(responses == rating)
                rating_counts[rating].append(count)
        
        # Create stacked bar chart
        bottom = np.zeros(len(questions))
        bars = []
        
        for rating in range(1, 6):
            counts = rating_counts[rating]
            bar = ax.bar(questions, counts, bottom=bottom, color=colors[rating-1], 
                        label=rating_labels[rating-1], alpha=0.8, edgecolor='black', linewidth=0.5)
            bars.append(bar)
            
            # Add percentage labels on each segment if count > 0
            for i, (count, bot) in enumerate(zip(counts, bottom)):
                if count > 0:
                    total_responses = len(survey_df)
                    percentage = (count / total_responses) * 100
                    ax.text(i, bot + count/2, f'{count}\n({percentage:.0f}%)', 
                           ha='center', va='center', fontsize=8, fontweight='bold')
            
            bottom += counts
        
        ax.set_title(f'{metric.capitalize()}', fontsize=14, fontweight='bold')
        ax.set_xlabel('Questions')
        ax.set_ylabel('Number of Responses')
        ax.set_ylim(0, len(survey_df) + 1)
        ax.legend(bbox_to_anchor=(1.05, 1), loc='upper left')
        ax.grid(True, alpha=0.3, axis='y')
        
        # Rotate x-axis labels for better readability
        ax.tick_params(axis='x', rotation=45)
    
    plt.tight_layout()
    plt.savefig('human_response_distribution_stacked.png', dpi=300, bbox_inches='tight')
    plt.show()
    print("Human response distribution (stacked bars) saved as 'human_response_distribution_stacked.png'")

def create_human_response_distribution_heatmap(survey_df):
    """Create heatmap showing frequency of each rating for each question-metric combination."""
    # Get all question-metric combinations
    question_cols = [col for col in survey_df.columns]
    question_cols.sort(key=lambda x: (int(x.split('_')[0][1:]), int(x.split('_')[1])))
    
    # Create matrix: rows = question-metric combinations, columns = ratings 1-5
    rating_matrix = np.zeros((len(question_cols), 5))
    
    for i, col in enumerate(question_cols):
        responses = survey_df[col].values
        for rating in range(1, 6):
            count = np.sum(responses == rating)
            rating_matrix[i, rating-1] = count
    
    # Create three separate heatmaps for each metric
    fig, axes = plt.subplots(1, 3, figsize=(18, 12))
    fig.suptitle('Human Response Distribution Heatmaps by Metric', fontsize=16, fontweight='bold')
    
    metrics = ['Comprehensibility', 'Completeness', 'Alignment']
    metric_suffixes = ['_1', '_2', '_3']
    
    for metric_idx, (metric, suffix) in enumerate(zip(metrics, metric_suffixes)):
        ax = axes[metric_idx]
        
        # Filter data for this metric
        metric_cols = [col for col in question_cols if col.endswith(suffix)]
        metric_indices = [question_cols.index(col) for col in metric_cols]
        metric_matrix = rating_matrix[metric_indices, :]
        
        # Create heatmap
        im = ax.imshow(metric_matrix, cmap='YlOrRd', aspect='auto')
        
        # Set labels
        questions = [col.split('_')[0] for col in metric_cols]
        ax.set_xticks(range(5))
        ax.set_xticklabels(['Rating 1', 'Rating 2', 'Rating 3', 'Rating 4', 'Rating 5'])
        ax.set_yticks(range(len(questions)))
        ax.set_yticklabels(questions)
        ax.set_title(f'{metric}')
        
        # Add text annotations with counts and percentages
        total_responses = len(survey_df)
        for i in range(len(questions)):
            for j in range(5):
                count = int(metric_matrix[i, j])
                percentage = (count / total_responses) * 100
                text_color = 'white' if count > total_responses * 0.3 else 'black'
                ax.text(j, i, f'{count}\n({percentage:.0f}%)', 
                       ha="center", va="center", color=text_color, fontsize=9, fontweight='bold')
        
        # Add colorbar
        cbar = plt.colorbar(im, ax=ax, shrink=0.8)
        cbar.set_label('Number of Responses')
    
    plt.tight_layout()
    plt.savefig('human_response_distribution_heatmap.png', dpi=300, bbox_inches='tight')
    plt.show()
    print("Human response distribution heatmap saved as 'human_response_distribution_heatmap.png'")

def create_human_response_box_plots(survey_df):
    """Create box plots showing distribution of human responses for each question."""
    fig, axes = plt.subplots(1, 3, figsize=(20, 8))
    fig.suptitle('Human Response Distribution - Box Plots with Individual Points', fontsize=16, fontweight='bold')
    
    metrics = ['Comprehensibility', 'Completeness', 'Alignment']
    metric_suffixes = ['_1', '_2', '_3']
    colors = ['#FF6B6B', '#45B7D1', '#FECA57']
    
    for metric_idx, (metric, suffix) in enumerate(zip(metrics, metric_suffixes)):
        ax = axes[metric_idx]
        
        # Get questions for this metric
        question_cols = [col for col in survey_df.columns if col.endswith(suffix)]
        question_cols.sort(key=lambda x: int(x.split('_')[0][1:]))
        
        questions = [col.split('_')[0] for col in question_cols]
        
        # Prepare data for box plot
        data_for_boxplot = []
        for col in question_cols:
            data_for_boxplot.append(survey_df[col].values)
        
        # Create box plot
        bp = ax.boxplot(data_for_boxplot, labels=questions, patch_artist=True, 
                       showfliers=False, medianprops={'color': 'black', 'linewidth': 2})
        
        # Color the boxes
        for patch in bp['boxes']:
            patch.set_facecolor(colors[metric_idx])
            patch.set_alpha(0.7)
        
        # Add individual points (jittered)
        for i, col in enumerate(question_cols):
            responses = survey_df[col].values
            # Add small random jitter to x-coordinates for better visibility
            x_jitter = np.random.normal(i+1, 0.05, len(responses))
            ax.scatter(x_jitter, responses, alpha=0.6, s=30, color='darkred', edgecolors='black', linewidth=0.5)
        
        ax.set_title(f'{metric}', fontsize=14, fontweight='bold')
        ax.set_xlabel('Questions')
        ax.set_ylabel('Rating (1-5)')
        ax.set_ylim(0.5, 5.5)
        ax.grid(True, alpha=0.3, axis='y')
        ax.tick_params(axis='x', rotation=45)
    
    plt.tight_layout()
    plt.savefig('human_response_distribution_boxplots.png', dpi=300, bbox_inches='tight')
    plt.show()
    print("Human response distribution box plots saved as 'human_response_distribution_boxplots.png'")

def calculate_human_response_statistics(survey_df):
    """Calculate comprehensive statistics for human response distributions."""
    stats = {}
    
    metrics = ['comprehensibility', 'completeness', 'alignment']
    metric_suffixes = ['_1', '_2', '_3']
    
    for metric, suffix in zip(metrics, metric_suffixes):
        question_cols = [col for col in survey_df.columns if col.endswith(suffix)]
        question_cols.sort(key=lambda x: int(x.split('_')[0][1:]))
        
        metric_stats = {}
        
        for col in question_cols:
            question = col.split('_')[0]
            responses = survey_df[col].values
            
            # Basic statistics
            mean_rating = np.mean(responses)
            std_rating = np.std(responses, ddof=1)
            median_rating = np.median(responses)
            mode_rating = np.bincount(responses).argmax()
            
            # Distribution statistics
            rating_counts = {rating: np.sum(responses == rating) for rating in range(1, 6)}
            total_responses = len(responses)
            rating_percentages = {rating: (count/total_responses)*100 for rating, count in rating_counts.items()}
            
            # Consensus measures
            max_agreement = max(rating_counts.values())
            consensus_percentage = (max_agreement / total_responses) * 100
            
            # Variability measures
            range_rating = np.max(responses) - np.min(responses)
            iqr = np.percentile(responses, 75) - np.percentile(responses, 25)
            
            metric_stats[question] = {
                'mean': mean_rating,
                'std': std_rating,
                'median': median_rating,
                'mode': mode_rating,
                'range': range_rating,
                'iqr': iqr,
                'rating_counts': rating_counts,
                'rating_percentages': rating_percentages,
                'consensus_percentage': consensus_percentage,
                'total_responses': total_responses
            }
        
        stats[metric] = metric_stats
    
    return stats

def save_human_response_distribution_report(stats):
    """Save comprehensive human response distribution report."""
    with open('human_response_distribution_report.txt', 'w') as f:
        f.write("HUMAN RESPONSE DISTRIBUTION ANALYSIS REPORT\n")
        f.write("=" * 60 + "\n\n")
        
        f.write("OVERVIEW\n")
        f.write("-" * 20 + "\n")
        f.write("This report analyzes the distribution of human evaluator responses\n")
        f.write("across three documentation quality metrics:\n")
        f.write("• Comprehensibility (Q*_1): How easy is the documentation to understand?\n")
        f.write("• Completeness (Q*_2): How complete is the documentation?\n")
        f.write("• Alignment (Q*_3): How well does the documentation align with the code?\n")
        f.write(f"• Questions analyzed: Q1-Q11\n")
        f.write(f"• Rating scale: 1-5 (1=lowest, 5=highest)\n")
        f.write(f"• Number of human evaluators: {list(stats.values())[0]['Q1']['total_responses']}\n\n")
        
        f.write("DETAILED STATISTICS BY METRIC AND QUESTION\n")
        f.write("-" * 50 + "\n\n")
        
        for metric, metric_stats in stats.items():
            f.write(f"{metric.upper()}\n")
            f.write("=" * len(metric) + "\n\n")
            
            for question, q_stats in metric_stats.items():
                f.write(f"{question}:\n")
                f.write(f"  Central Tendency:\n")
                f.write(f"    Mean: {q_stats['mean']:.2f}\n")
                f.write(f"    Median: {q_stats['median']:.1f}\n")
                f.write(f"    Mode: {q_stats['mode']}\n")
                f.write(f"  \n")
                f.write(f"  Variability:\n")
                f.write(f"    Standard Deviation: {q_stats['std']:.2f}\n")
                f.write(f"    Range: {q_stats['range']:.1f}\n")
                f.write(f"    Interquartile Range: {q_stats['iqr']:.1f}\n")
                f.write(f"  \n")
                f.write(f"  Response Distribution:\n")
                for rating in range(1, 6):
                    count = q_stats['rating_counts'][rating]
                    percentage = q_stats['rating_percentages'][rating]
                    f.write(f"    Rating {rating}: {count} responses ({percentage:.1f}%)\n")
                f.write(f"  \n")
                f.write(f"  Consensus:\n")
                f.write(f"    Highest agreement: {q_stats['consensus_percentage']:.1f}% (Rating {q_stats['mode']})\n")
                f.write(f"\n")
            
            f.write("\n")
        
        # Summary statistics across all questions
        f.write("SUMMARY STATISTICS ACROSS ALL QUESTIONS\n")
        f.write("-" * 45 + "\n\n")
        
        for metric, metric_stats in stats.items():
            f.write(f"{metric.upper()}:\n")
            
            all_means = [q_stats['mean'] for q_stats in metric_stats.values()]
            all_stds = [q_stats['std'] for q_stats in metric_stats.values()]
            all_consensus = [q_stats['consensus_percentage'] for q_stats in metric_stats.values()]
            
            f.write(f"  Average mean rating across questions: {np.mean(all_means):.2f}\n")
            f.write(f"  Average standard deviation: {np.mean(all_stds):.2f}\n")
            f.write(f"  Average consensus percentage: {np.mean(all_consensus):.1f}%\n")
            f.write(f"  Questions with highest variability: {sorted(metric_stats.keys(), key=lambda q: metric_stats[q]['std'], reverse=True)[:3]}\n")
            f.write(f"  Questions with lowest variability: {sorted(metric_stats.keys(), key=lambda q: metric_stats[q]['std'])[:3]}\n")
            f.write(f"\n")
    
    print("Human response distribution report saved as 'human_response_distribution_report.txt'")

def save_comparison_report(stats, comparison_data):
    """Save comprehensive comparison report to file."""
    with open('ai_developer_comparison_report.txt', 'w') as f:
        f.write("AI vs DEVELOPER RATINGS COMPARISON REPORT\n")
        f.write("=" * 60 + "\n\n")
        
        f.write("OVERVIEW\n")
        f.write("-" * 20 + "\n")
        f.write("This report compares AI-generated ratings with averaged human developer ratings\n")
        f.write("across three key documentation quality metrics:\n")
        f.write("• Comprehensibility (Q*_1): How easy is the documentation to understand?\n")
        f.write("• Completeness (Q*_2): How complete is the documentation?\n")
        f.write("• Alignment (Q*_3): How well does the documentation align with the code?\n")
        f.write(f"• Total questions analyzed: 11 (Q1-Q11)\n")
        f.write(f"• Rating scale: 1-5 (1=lowest, 5=highest)\n\n")
        
        f.write("DETAILED STATISTICS BY METRIC\n")
        f.write("-" * 35 + "\n\n")
        
        for metric, stat in stats.items():
            f.write(f"{metric.upper()}\n")
            f.write(f"  AI Ratings:\n")
            f.write(f"    Mean: {stat['ai_mean']:.3f} ± {stat['ai_std']:.3f}\n")
            f.write(f"  Developer Ratings:\n")
            f.write(f"    Mean: {stat['dev_mean']:.3f} ± {stat['dev_std']:.3f}\n")
            f.write(f"  \n")
            f.write(f"  Correlation Analysis:\n")
            f.write(f"    Pearson correlation: r = {stat['pearson_corr']:.3f} (p = {stat['pearson_p']:.3f})\n")
            f.write(f"    Spearman correlation: ρ = {stat['spearman_corr']:.3f} (p = {stat['spearman_p']:.3f})\n")
            f.write(f"  \n")
            f.write(f"  Difference Analysis:\n")
            f.write(f"    Mean difference (AI - Dev): {stat['mean_difference']:.3f}\n")
            f.write(f"    Standard deviation of differences: {stat['std_difference']:.3f}\n")
            f.write(f"    Mean absolute difference: {stat['mean_abs_difference']:.3f}\n")
            f.write(f"  \n")
            f.write(f"  Agreement Analysis:\n")
            f.write(f"    Exact agreement (±0.1): {stat['exact_agreement_count']}/{stat['total_comparisons']} ({stat['exact_agreement_count']/stat['total_comparisons']*100:.1f}%)\n")
            f.write(f"    Close agreement (±0.5): {stat['close_agreement_count']}/{stat['total_comparisons']} ({stat['close_agreement_count']/stat['total_comparisons']*100:.1f}%)\n")
            f.write(f"\n")
        
        f.write("QUESTION-BY-QUESTION BREAKDOWN\n")
        f.write("-" * 35 + "\n\n")
        
        for metric in ['comprehensibility', 'completeness', 'alignment']:
            f.write(f"{metric.upper()}:\n")
            data = comparison_data[metric]
            for i, (q, ai, dev) in enumerate(zip(data['questions'], data['ai_ratings'], data['dev_ratings'])):
                diff = ai - dev
                f.write(f"  {q}: AI={ai:.1f}, Dev={dev:.1f}, Diff={diff:+.1f}\n")
            f.write(f"\n")
        
        # Overall summary
        all_ai = []
        all_dev = []
        for metric in ['comprehensibility', 'completeness', 'alignment']:
            data = comparison_data[metric]
            all_ai.extend(data['ai_ratings'])
            all_dev.extend(data['dev_ratings'])
        
        overall_corr, overall_p = pearsonr(all_ai, all_dev)
        overall_mean_diff = np.mean(np.array(all_ai) - np.array(all_dev))
        overall_mean_abs_diff = np.mean(np.abs(np.array(all_ai) - np.array(all_dev)))
        
        f.write("OVERALL SUMMARY\n")
        f.write("-" * 20 + "\n")
        f.write(f"Overall correlation (all metrics): r = {overall_corr:.3f} (p = {overall_p:.3f})\n")
        f.write(f"Overall mean difference: {overall_mean_diff:.3f}\n")
        f.write(f"Overall mean absolute difference: {overall_mean_abs_diff:.3f}\n")
        f.write(f"Total comparisons: {len(all_ai)}\n")
    
    print("Comprehensive comparison report saved as 'ai_developer_comparison_report.txt'")

def main():
    """Main function to run the AI vs Developer comparison analysis."""
    print("Starting AI vs Developer Ratings Comparison Analysis...")
    print("=" * 60)
    
    # Load survey data
    survey_df = load_survey_data()
    if survey_df is None:
        return
    
    # Load simplified output data
    simplified_df = load_simplified_output()
    if simplified_df is None:
        return
    
    # Organize data for comparison
    comparison_data = organize_comparison_data(simplified_df)
    
    # Create visualizations
    print(f"\nCreating AI vs Developer comparison plots...")
    create_ai_vs_developer_comparison(comparison_data)
    
    print(f"Creating rating agreement analysis...")
    create_scatter_comparison(comparison_data)
    
    print(f"Creating difference analysis...")
    create_difference_analysis(comparison_data)
    
    # Calculate comprehensive statistics
    print(f"Calculating comprehensive statistics...")
    stats = calculate_comprehensive_statistics(comparison_data)
    
    # Save detailed report
    save_comparison_report(stats, comparison_data)
    
    # Create human response distribution visualizations
    print(f"\nCreating human response distribution visualizations...")
    create_human_response_distribution_stacked_bars(survey_df)
    
    print(f"Creating human response distribution heatmaps...")
    create_human_response_distribution_heatmap(survey_df)
    
    print(f"Creating human response distribution box plots...")
    create_human_response_box_plots(survey_df)
    
    # Calculate and save human response statistics
    print(f"Calculating human response distribution statistics...")
    human_stats = calculate_human_response_statistics(survey_df)
    save_human_response_distribution_report(human_stats)
    
    # Print summary to console
    print(f"\nSUMMARY STATISTICS")
    print(f"-" * 30)
    for metric, stat in stats.items():
        print(f"{metric.capitalize()}:")
        print(f"  Correlation: r = {stat['pearson_corr']:.3f}")
        print(f"  Mean difference (AI - Dev): {stat['mean_difference']:+.3f}")
        print(f"  Mean absolute difference: {stat['mean_abs_difference']:.3f}")
    
    print(f"\nHUMAN RESPONSE DISTRIBUTION SUMMARY")
    print(f"-" * 40)
    for metric, metric_stats in human_stats.items():
        all_means = [q_stats['mean'] for q_stats in metric_stats.values()]
        all_stds = [q_stats['std'] for q_stats in metric_stats.values()]
        all_consensus = [q_stats['consensus_percentage'] for q_stats in metric_stats.values()]
        
        print(f"{metric.capitalize()}:")
        print(f"  Average mean rating: {np.mean(all_means):.2f}")
        print(f"  Average std deviation: {np.mean(all_stds):.2f}")
        print(f"  Average consensus: {np.mean(all_consensus):.1f}%")
    
    print(f"\nAnalysis complete! Generated files:")
    print(f"  AI vs Developer Comparison:")
    print(f"    - ai_vs_developer_comparison.png")
    print(f"    - rating_agreement_analysis.png")
    print(f"    - difference_analysis.png")
    print(f"    - ai_developer_comparison_report.txt")
    print(f"  Human Response Distribution:")
    print(f"    - human_response_distribution_stacked.png")
    print(f"    - human_response_distribution_heatmap.png")
    print(f"    - human_response_distribution_boxplots.png")
    print(f"    - human_response_distribution_report.txt")

if __name__ == "__main__":
    main()
