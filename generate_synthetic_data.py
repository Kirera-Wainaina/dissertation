#!/usr/bin/env python3
"""
Synthetic Data Generator for Number Partitioning Problem

This script generates synthetic data for the number partitioning problem in the 
format required by the generic backtracking solver. It creates multiple instances 
for different problem sizes.

Parameters:
- n (number of digits): 5 to 50
- k (partitions): 3 to 5
- d (digits): Random integers between 1 and 99

For each value of n, 5 different random instances are created.
"""

import os
import random
import argparse

def generate_instance(n, k, version):
    """
    Generate a single instance of a number partitioning problem.
    
    Args:
        n (int): Number of digits in the instance
        k (int): Number of partitions
        version (int): Version number (1-5)
        
    Returns:
        list: Lines of the instance file
    """
    # Generate n random integers between 1 and 99
    numbers = [random.randint(1, 99) for _ in range(n)]
    
    # Sort the numbers in ascending order
    numbers.sort()
    
    # Format the instance
    lines = [
        "-1",  # First line is -1 (solution unknown)
        str(k)  # Second line is number of partitions
    ]
    
    # Add each number on a separate line
    lines.extend(str(num) for num in numbers)
    
    return lines

def create_directory(directory):
    """
    Create a directory if it doesn't exist.
    
    Args:
        directory (str): Path to the directory
    """
    if not os.path.exists(directory):
        os.makedirs(directory)
        print(f"Created directory: {directory}")

def main():
    """Main function to generate all synthetic data instances."""
    parser = argparse.ArgumentParser(description="Generate synthetic data for number partitioning")
    parser.add_argument("--output", default="./solver/numpart/instances/synthetic", 
                        help="Output directory for synthetic data")
    args = parser.parse_args()
    
    # Create output directory
    create_directory(args.output)
    
    # Generate instances for each n from 5 to 50
    for n in range(5, 51):
        for k in range(3, 6):  # k ranges from 3 to 5
            for version in range(1, 6):  # 5 versions for each n
                # Generate the instance
                instance_data = generate_instance(n, k, version)
                
                # Create filename
                filename = f"n{n}k{k}_v{version}.txt"
                filepath = os.path.join(args.output, filename)
                
                # Write to file
                with open(filepath, "w") as f:
                    f.write("\n".join(instance_data))
                
                print(f"Generated {filepath}")

if __name__ == "__main__":
    main()