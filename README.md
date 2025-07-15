# [WIP] Multiway Number Partitioning Workload Prediction

## Overview
This repository contains code and documentation for predicting solver workload (`final_expandEvts`) in multiway number partitioning, an NP-hard combinatorial optimization problem with applications in scheduling, load balancing, and high-performance computing. The project leverages machine learning to estimate the total number of expansion events based on early-stage solver behavior (stack depth=3 logs), addressing the challenge of unpredictable backtracking runtimes [8, 9]. Using a dataset of ~690 instances, we train Random Forest models to predict workload, achieving a Mean Absolute Percentage Error (MAPE) of 12.58% for a model incorporating all instances and 22.86% for non-censored instances.

## Objectives
The project aims to:
1. Build a Python pipeline to generate and solve multiway number partitioning instances, capturing early runtime logs.
2. Extract and structure solver features (e.g., `avg_evts`, `avg_pruneBacktrackEvts`, `avg_expandEvts`) for machine learning.
3. Train Random Forest models to predict `final_expandEvts`, evaluating performance with relative error metrics (MAPE, MdAPE, Normalized RMSE).
4. Analyze model generalizability across diverse instances (varying \( n \), \( k \)).
5. Communicate findings through a research poster and dissertation.

## Methodology
The methodology processes a dataset of ~690 instances, imputing missing solver logs (e.g., `evts_i`, `expandEvts_i`) with zeros based on `num_stackdepth3_logs` and \( k=3 \). Feature engineering emphasizes aggregate metrics (`avg_evts`, `avg_pruneBacktrackEvts`, `avg_expandEvts`) and derived features (log-scaled, log differences). Two Random Forest models are trained: one on non-censored instances (`censored=0`, ~300–400 instances) and another on all instances with `censored` as a feature. Performance is evaluated using MAPE, MdAPE, and Normalized RMSE with 5-fold cross-validation.

## Results
The all-data model achieves a MAPE of 12.58% and Normalized RMSE of 7.40%, indicating strong predictive accuracy across diverse instances (mean `final_expandEvts`: 761.16M). The censored=0 model yields a MAPE of 22.86% and Normalized RMSE of 121.01% (mean: 53.23M), reflecting challenges with smaller instances. Feature importance highlights `avg_pruneBacktrackEvts` (34–35%), `avg_evts`, and `avg_expandEvts` as key predictors, underscoring their role in capturing solver behavior.

## Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/your-repo-name.git
   ```
2. Install dependencies:
   ```bash
   pip install pandas numpy scikit-learn matplotlib
   ```
3. Ensure `structured_data.xlsx` is in the repository root or update the data path in scripts.

## Usage
1. **Prepare Data**: Run `add_ratio_features.py` to preprocess the dataset and generate features:
   ```bash
   python add_ratio_features.py
   ```
   This script expects `structured_data.xlsx` and outputs a processed DataFrame.

2. **Train Models**: Run `random_forest_relative_error.py` to train Random Forest models and evaluate performance:
   ```bash
   python random_forest_relative_error.py
   ```
   Outputs include console logs (MAPE, MdAPE, Normalized RMSE, feature importance) and files in the `plots/` directory (predictions, plots).

3. **Example Script**:
   ```python
   import pandas as pd
   from add_ratio_features import add_ratio_features
   from random_forest_relative_error import train_random_forest_relative_error

   df = pd.read_excel('structured_data.xlsx', sheet_name='Sheet1')
   df = add_ratio_features(df)
   results = train_random_forest_relative_error(df, include_ratios=False, save_plots=True)
   ```

## Directory Structure
- `add_ratio_features.py`: Script for data preprocessing and feature engineering.
- `random_forest_relative_error.py`: Script for training and evaluating Random Forest models.
- `structured_data.xlsx`: Sample dataset with solver logs and `final_expandEvts`.
- `plots/`: Directory for saved predictions, feature importance, and plots.

## License
**All Rights Reserved**

## Contact
For questions or contributions, contact [your-email@example.com] or open an issue on GitHub.
