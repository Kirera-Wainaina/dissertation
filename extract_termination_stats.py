#!/usr/bin/env python3
"""
Step 3:
Script to extract termination statistics from NumPartV2o runs that completed successfully.
This script processes the ml_features.jsonl file, identifies entries with TERMINATE events,
runs NumPartV2o on the corresponding instances, and calculates statistical measures
on the partition sums.
"""

import os
import json
import subprocess
import re
import statistics
from pathlib import Path

# Configuration variables
SCRIPT_DIR = Path(os.path.dirname(os.path.abspath(__file__)))
INPUT_JSONL = SCRIPT_DIR / "ml_features.jsonl"
OUTPUT_JSONL = SCRIPT_DIR / "termination_stats.jsonl"
FEATURE_COLLECTED_DIR = "instances/feature_collected"
JAVA_CMD = "java -cp .. numpart.NumPartV2o"
SUBPROCESS_TIMEOUT = 60  # 1 minute timeout for Java process

def process_jsonl_file():
    """
    Process the ml_features.jsonl file to find entries with TERMINATE events.

    Returns:
        List of filenames that have TERMINATE events
    """
    terminate_files = []

    if not os.path.exists(INPUT_JSONL):
        print(f"Error: Input file {INPUT_JSONL} does not exist")
        return terminate_files

    print(f"Processing input file: {INPUT_JSONL}")

    try:
        with open(INPUT_JSONL, 'r') as f:
            line_count = 0
            terminate_count = 0

            for line in f:
                line_count += 1
                try:
                    data = json.loads(line.strip())
                    # Each line contains a single key (filename) mapped to a list of log entries
                    for filename, log_entries in data.items():
                        # Check if the last entry has a TERMINATE event
                        if log_entries and isinstance(log_entries, list) and log_entries[-1].get("event") == "TERMINATE":
                            terminate_files.append(filename)
                            terminate_count += 1
                except json.JSONDecodeError as e:
                    print(f"Error parsing line {line_count}: {e}")
                    continue

            print(f"Processed {line_count} lines, found {terminate_count} files with TERMINATE events")
    except Exception as e:
        print(f"Error reading input file: {e}")

    return terminate_files

def run_numpart_solver(filename):
    """
    Run the NumPartV2o solver on the given instance file and extract the partition sums.

    Args:
        filename: Name of the instance file

    Returns:
        Dictionary with the subset sum statistics or None if error
    """
    instance_path = os.path.join(FEATURE_COLLECTED_DIR, filename)
    full_cmd = f"{JAVA_CMD} {instance_path}"

    try:
        print(f"Running command: {full_cmd}")
        # Run the command with timeout
        result = subprocess.run(
            full_cmd,
            shell=True,
            capture_output=True,
            text=True,
            timeout=SUBPROCESS_TIMEOUT,
            cwd=SCRIPT_DIR / "solver/numpart"
        )

        if result.returncode != 0:
            print(f"Error running command for {filename}: {result.stderr}")
            return None

        return parse_solver_output(result.stdout, filename)
    except subprocess.TimeoutExpired:
        print(f"Timeout running solver for {filename}")
        return None
    except Exception as e:
        print(f"Exception running solver for {filename}: {e}")
        return None

def parse_solver_output(output, filename):
    """
    Parse the solver output to extract the partition sums and calculate statistics.

    Args:
        output: String containing the command output
        filename: Name of the instance file

    Returns:
        Dictionary with the subset sum statistics or None if error
    """
    try:
        # Extract the Sums line using regex
        sums_match = re.search(r'Sums: \[(.*?)\]', output)
        if not sums_match:
            print(f"No Sums found in output for {filename}")
            return None

        # Extract the sums as a list of integers
        sums_str = sums_match.group(1)
        sums = [int(x.strip()) for x in sums_str.split(',')]

        if not sums:
            print(f"Empty sums list for {filename}")
            return None

        # Calculate statistics
        subset_sum_max = max(sums)
        subset_sum_min = min(sums)
        subset_sum_variance = statistics.variance(sums) if len(sums) > 1 else 0

        print(f"Found sums for {filename}: {sums}")
        print(f"Statistics: max={subset_sum_max}, min={subset_sum_min}, variance={subset_sum_variance}")

        return {
            "subset_sum_max": subset_sum_max,
            "subset_sum_min": subset_sum_min,
            "subset_sum_variance": subset_sum_variance,
            "sums": sums  # Include the raw sums for reference
        }
    except Exception as e:
        print(f"Error parsing solver output for {filename}: {e}")
        return None

def main():
    print("Starting extraction of termination statistics...")

    # Find all files with TERMINATE events
    terminate_files = process_jsonl_file()

    if not terminate_files:
        print("No files with TERMINATE events found. Exiting.")
        return

    print(f"Found {len(terminate_files)} files with TERMINATE events")

    # Create output directory if it doesn't exist
    os.makedirs(os.path.dirname(OUTPUT_JSONL), exist_ok=True)

    # Process each file and collect statistics
    success_count = 0
    result_dict = {}

    for i, filename in enumerate(terminate_files):
        print(f"\nProcessing {i+1}/{len(terminate_files)}: {filename}")
        stats = run_numpart_solver(filename)

        if stats:
            result_dict[filename] = stats
            success_count += 1

            # Write results incrementally to avoid data loss
            with open(OUTPUT_JSONL, 'a') as f:
                json_line = json.dumps({filename: stats})
                f.write(json_line + '\n')

            print(f"Appended result for {filename} to {OUTPUT_JSONL}")

    print("\nTermination statistics extraction complete.")
    print(f"Successfully processed {success_count} out of {len(terminate_files)} files.")
    print(f"Results saved to {OUTPUT_JSONL}")

if __name__ == "__main__":
    main()
