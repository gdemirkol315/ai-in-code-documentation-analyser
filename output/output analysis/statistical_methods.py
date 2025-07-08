import pandas as pd
import numpy as np
from scipy.stats import spearmanr, ttest_rel
import matplotlib.pyplot as plt
import seaborn as sns
from typing import Tuple, Dict, List
import warnings

class StatisticalAnalyzer:
    """
    A class for performing statistical analysis on documentation quality metrics.
    """
    
    def __init__(self, data_file: str = "output-simplified.txt"):
        """
        Initialize the analyzer with data from the specified file.
        
        Args:
            data_file (str): Path to the data file
        """
        self.data_file = data_file
        self.df = None
        self.load_data()
    
    def load_data(self) -> None:
        """
        Load data from the specified file into a pandas DataFrame.
        """
        try:
            self.df = pd.read_csv(self.data_file, sep='\t')
            print(f"Data loaded successfully from {self.data_file}")
            print(f"Shape: {self.df.shape}")
            print(f"Columns: {list(self.df.columns)}")
        except FileNotFoundError:
            print(f"Error: File {self.data_file} not found.")
            raise
        except Exception as e:
            print(f"Error loading data: {e}")
            raise
    
    def display_data_summary(self) -> None:
        """
        Display basic statistics and information about the dataset.
        """
        if self.df is None:
            print("No data loaded.")
            return
        
        print("\n" + "="*50)
        print("DATA SUMMARY")
        print("="*50)
        print(f"Number of observations: {len(self.df)}")
        print(f"Number of variables: {len(self.df.columns)}")
        
        print("\nFirst few rows:")
        print(self.df.head())
        
        print("\nDescriptive statistics:")
        print(self.df[['AI_Result', 'Average_Developer_Result']].describe())
        
        print("\nData types:")
        print(self.df.dtypes)
        
        # Check for missing values
        missing_values = self.df.isnull().sum()
        if missing_values.any():
            print("\nMissing values:")
            print(missing_values)
        else:
            print("\nNo missing values found.")
    
    def calculate_spearman_correlation(self) -> Tuple[float, float]:
        """
        Calculate Spearman's rank correlation coefficient and p-value.
        
        Returns:
            Tuple[float, float]: (correlation coefficient, p-value)
        """
        if self.df is None:
            raise ValueError("No data loaded.")
        
        ai_results = self.df['AI_Result'].values
        dev_results = self.df['Average_Developer_Result'].values
        
        # Calculate Spearman's rank correlation
        correlation, p_value = spearmanr(ai_results, dev_results)
        
        return correlation, p_value
    
    def interpret_correlation(self, correlation: float) -> str:
        """
        Provide interpretation of correlation strength.
        
        Args:
            correlation (float): Correlation coefficient
            
        Returns:
            str: Interpretation of correlation strength
        """
        abs_corr = abs(correlation)
        
        if abs_corr < 0.1:
            strength = "negligible"
        elif abs_corr < 0.3:
            strength = "weak"
        elif abs_corr < 0.5:
            strength = "moderate"
        elif abs_corr < 0.7:
            strength = "strong"
        else:
            strength = "very strong"
        
        direction = "positive" if correlation > 0 else "negative"
        
        return f"{strength} {direction}"
    
    def perform_correlation_analysis(self) -> Dict:
        """
        Perform complete Spearman correlation analysis and return results.
        
        Returns:
            Dict: Analysis results including correlation, p-value, and interpretation
        """
        correlation, p_value = self.calculate_spearman_correlation()
        interpretation = self.interpret_correlation(correlation)
        
        results = {
            'correlation': correlation,
            'p_value': p_value,
            'interpretation': interpretation,
            'n_observations': len(self.df),
            'significant': p_value < 0.05
        }
        
        return results
    
    def calculate_paired_t_test(self, ai_scores: List[float], human_scores: List[float]) -> Dict:
        """
        Calculate paired-sample t-test (H2) for comparing AI-generated documentation 
        quality scores against human ratings.
        
        Args:
            ai_scores (List[float]): List of AI-generated scores per sample
            human_scores (List[float]): List of average human scores per sample
            
        Returns:
            Dict: Dictionary containing:
                - t_statistic: the test statistic value
                - p_value: the p-value of the test
                - mean_difference: the average difference between AI and human scores
                
        Raises:
            ValueError: If input validation fails
        """
        # Input validation
        if not isinstance(ai_scores, list) or not isinstance(human_scores, list):
            raise ValueError("Both ai_scores and human_scores must be lists")
        
        if len(ai_scores) != len(human_scores):
            raise ValueError("ai_scores and human_scores must have the same length")
        
        if len(ai_scores) < 2:
            raise ValueError("Both lists must contain at least 2 values")
        
        # Convert to numpy arrays and check for valid numeric values
        try:
            ai_array = np.array(ai_scores, dtype=float)
            human_array = np.array(human_scores, dtype=float)
        except (ValueError, TypeError):
            raise ValueError("All values in both lists must be numeric")
        
        # Check for NaN or infinite values
        if np.any(np.isnan(ai_array)) or np.any(np.isnan(human_array)):
            raise ValueError("Lists cannot contain NaN values")
        
        if np.any(np.isinf(ai_array)) or np.any(np.isinf(human_array)):
            raise ValueError("Lists cannot contain infinite values")
        
        # Perform paired t-test
        t_statistic, p_value = ttest_rel(ai_array, human_array)
        
        # Calculate mean difference (AI - Human)
        differences = ai_array - human_array
        mean_difference = np.mean(differences)
        
        return {
            't_statistic': float(t_statistic),
            'p_value': float(p_value),
            'mean_difference': float(mean_difference)
        }
    
    def perform_paired_t_test_analysis(self) -> Dict:
        """
        Perform paired t-test analysis using the loaded data.
        
        Returns:
            Dict: Analysis results including t-statistic, p-value, and mean difference
        """
        if self.df is None:
            raise ValueError("No data loaded.")
        
        ai_scores = self.df['AI_Result'].tolist()
        human_scores = self.df['Average_Developer_Result'].tolist()
        
        return self.calculate_paired_t_test(ai_scores, human_scores)
    
    def print_paired_t_test_results(self) -> None:
        """
        Print formatted paired t-test analysis results.
        """
        results = self.perform_paired_t_test_analysis()
        
        print("\n" + "="*60)
        print("PAIRED T-TEST ANALYSIS (H2)")
        print("="*60)
        print(f"Number of paired observations: {len(self.df)}")
        print(f"T-statistic: {results['t_statistic']:.4f}")
        print(f"P-value: {results['p_value']:.6f}")
        print(f"Mean difference (AI - Human): {results['mean_difference']:.4f}")
        
        # Statistical significance
        alpha = 0.05
        if results['p_value'] < alpha:
            print(f"Result: STATISTICALLY SIGNIFICANT at α = {alpha}")
            if results['mean_difference'] > 0:
                print("AI scores are significantly HIGHER than human scores on average.")
            else:
                print("AI scores are significantly LOWER than human scores on average.")
        else:
            print(f"Result: NOT STATISTICALLY SIGNIFICANT at α = {alpha}")
            print("No significant difference between AI and human scores.")
        
        # Effect size interpretation (Cohen's d)
        if self.df is not None:
            differences = self.df['AI_Result'] - self.df['Average_Developer_Result']
            cohens_d = results['mean_difference'] / differences.std()
            
            abs_d = abs(cohens_d)
            if abs_d < 0.2:
                effect_size = "negligible"
            elif abs_d < 0.5:
                effect_size = "small"
            elif abs_d < 0.8:
                effect_size = "medium"
            else:
                effect_size = "large"
            
            print(f"Cohen's d: {cohens_d:.4f} ({effect_size} effect size)")
    
    def print_correlation_results(self) -> None:
        """
        Print formatted correlation analysis results.
        """
        results = self.perform_correlation_analysis()
        
        print("\n" + "="*60)
        print("SPEARMAN'S RANK CORRELATION ANALYSIS")
        print("="*60)
        print(f"Number of observations: {results['n_observations']}")
        print(f"Correlation coefficient (ρ): {results['correlation']:.4f}")
        print(f"P-value: {results['p_value']:.6f}")
        print(f"Interpretation: {results['interpretation']} correlation")
        
        # Statistical significance
        alpha = 0.05
        if results['significant']:
            print(f"Result: STATISTICALLY SIGNIFICANT at α = {alpha}")
            print("The correlation is unlikely to be due to chance.")
        else:
            print(f"Result: NOT STATISTICALLY SIGNIFICANT at α = {alpha}")
            print("The correlation could be due to chance.")
        
        # Effect size interpretation
        abs_corr = abs(results['correlation'])
        if abs_corr >= 0.5:
            effect_size = "large"
        elif abs_corr >= 0.3:
            effect_size = "medium"
        else:
            effect_size = "small"
        
        print(f"Effect size: {effect_size}")
        
        # Confidence interval (approximate)
        n = results['n_observations']
        if n > 3:
            # Fisher's z-transformation for confidence interval
            z = np.arctanh(results['correlation'])
            se = 1 / np.sqrt(n - 3)
            z_critical = 1.96  # for 95% CI
            
            z_lower = z - z_critical * se
            z_upper = z + z_critical * se
            
            ci_lower = np.tanh(z_lower)
            ci_upper = np.tanh(z_upper)
            
            print(f"95% Confidence Interval: [{ci_lower:.4f}, {ci_upper:.4f}]")
    
    def create_scatter_plot(self, save_plot: bool = True) -> None:
        """
        Create a scatter plot with regression line to visualize the correlation.
        
        Args:
            save_plot (bool): Whether to save the plot as a file
        """
        if self.df is None:
            print("No data loaded.")
            return
        
        plt.figure(figsize=(10, 8))
        
        # Create scatter plot
        plt.scatter(self.df['AI_Result'], self.df['Average_Developer_Result'], 
                   alpha=0.7, s=60, color='steelblue', edgecolors='black', linewidth=0.5)
        
        # Add regression line
        z = np.polyfit(self.df['AI_Result'], self.df['Average_Developer_Result'], 1)
        p = np.poly1d(z)
        plt.plot(self.df['AI_Result'], p(self.df['AI_Result']), "r--", alpha=0.8, linewidth=2)
        
        # Calculate and display correlation
        correlation, p_value = self.calculate_spearman_correlation()
        
        plt.xlabel('AI Result', fontsize=12, fontweight='bold')
        plt.ylabel('Average Developer Result', fontsize=12, fontweight='bold')
        plt.title(f'AI vs Developer Results\nSpearman ρ = {correlation:.4f}, p = {p_value:.6f}', 
                 fontsize=14, fontweight='bold')
        
        # Add grid
        plt.grid(True, alpha=0.3)
        
        # Set axis limits with some padding
        x_min, x_max = self.df['AI_Result'].min(), self.df['AI_Result'].max()
        y_min, y_max = self.df['Average_Developer_Result'].min(), self.df['Average_Developer_Result'].max()
        
        plt.xlim(x_min - 0.2, x_max + 0.2)
        plt.ylim(y_min - 0.2, y_max + 0.2)
        
        # Add correlation info as text box
        textstr = f'ρ = {correlation:.4f}\np = {p_value:.6f}\nn = {len(self.df)}'
        props = dict(boxstyle='round', facecolor='wheat', alpha=0.8)
        plt.text(0.05, 0.95, textstr, transform=plt.gca().transAxes, fontsize=10,
                verticalalignment='top', bbox=props)
        
        plt.tight_layout()
        
        if save_plot:
            plt.savefig('correlation_plot.png', dpi=300, bbox_inches='tight')
            print("Plot saved as 'correlation_plot.png'")
        
        plt.show()
    
    def create_rank_comparison_plot(self, save_plot: bool = True) -> None:
        """
        Create a plot comparing the ranks of AI and Developer results.
        
        Args:
            save_plot (bool): Whether to save the plot as a file
        """
        if self.df is None:
            print("No data loaded.")
            return
        
        # Calculate ranks
        ai_ranks = self.df['AI_Result'].rank()
        dev_ranks = self.df['Average_Developer_Result'].rank()
        
        plt.figure(figsize=(10, 8))
        
        # Create scatter plot of ranks
        plt.scatter(ai_ranks, dev_ranks, alpha=0.7, s=60, color='darkgreen', 
                   edgecolors='black', linewidth=0.5)
        
        # Add diagonal line (perfect correlation)
        min_rank, max_rank = 1, len(self.df)
        plt.plot([min_rank, max_rank], [min_rank, max_rank], 'r--', alpha=0.8, linewidth=2, 
                label='Perfect correlation')
        
        # Calculate and display correlation
        correlation, p_value = self.calculate_spearman_correlation()
        
        plt.xlabel('AI Result Ranks', fontsize=12, fontweight='bold')
        plt.ylabel('Developer Result Ranks', fontsize=12, fontweight='bold')
        plt.title(f'Rank Comparison: AI vs Developer Results\nSpearman ρ = {correlation:.4f}', 
                 fontsize=14, fontweight='bold')
        
        plt.grid(True, alpha=0.3)
        plt.legend()
        
        # Add correlation info as text box
        textstr = f'ρ = {correlation:.4f}\np = {p_value:.6f}\nn = {len(self.df)}'
        props = dict(boxstyle='round', facecolor='lightblue', alpha=0.8)
        plt.text(0.05, 0.95, textstr, transform=plt.gca().transAxes, fontsize=10,
                verticalalignment='top', bbox=props)
        
        plt.tight_layout()
        
        if save_plot:
            plt.savefig('rank_comparison_plot.png', dpi=300, bbox_inches='tight')
            print("Plot saved as 'rank_comparison_plot.png'")
        
        plt.show()
    
    def export_results_to_csv(self, filename: str = "correlation_results.csv") -> None:
        """
        Export correlation analysis results to a CSV file.
        
        Args:
            filename (str): Name of the output CSV file
        """
        results = self.perform_correlation_analysis()
        
        # Create a DataFrame with results
        results_df = pd.DataFrame([{
            'Analysis': 'Spearman Rank Correlation',
            'Correlation_Coefficient': results['correlation'],
            'P_Value': results['p_value'],
            'N_Observations': results['n_observations'],
            'Statistically_Significant': results['significant'],
            'Interpretation': results['interpretation']
        }])
        
        results_df.to_csv(filename, index=False)
        print(f"Results exported to {filename}")


def main():
    """
    Main function to run the statistical analysis.
    """
    print("Statistical Analysis of AI vs Developer Documentation Quality Ratings")
    print("="*70)
    
    try:
        # Initialize analyzer
        analyzer = StatisticalAnalyzer()
        
        # Display data summary
        analyzer.display_data_summary()
        
        # Perform and print correlation analysis
        analyzer.print_correlation_results()
        
        # Perform and print paired t-test analysis
        analyzer.print_paired_t_test_results()
        
        # Create visualizations
        print("\nCreating visualizations...")
        analyzer.create_scatter_plot()
        analyzer.create_rank_comparison_plot()
        
        # Export results
        analyzer.export_results_to_csv()
        
        print("\nAnalysis complete!")
        
    except Exception as e:
        print(f"Error during analysis: {e}")


if __name__ == "__main__":
    main()
