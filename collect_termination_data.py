#!/usr/bin/env python3
"""
Script to run NumPartV2o on synthetic instances and collect features for ML.
"""

import os
import json
import subprocess
import re
import shutil

# Configuration variables
TIMEOUT = 60 * 5  # Timeout in seconds
JAVA_CMD = "java -cp .. numpart.NumPartV2o"
SYNTHETIC_DIR = "instances/synthetic"
SYNTHETIC_SOLVED_DIR = "instances/synthetic_solved"
FEATURE_COLLECTED_DIR = "instances/feature_collected"
OUTPUT_JSON = "solver_output.json"

def run_command(instance_path):
    """
    Run the Java command on the given instance with a timeout.

    Args:
        instance_path: Relative path to the instance file from numpart dir

    Returns:
        Dictionary with the collected data or None if timeout
    """
    full_cmd = f"{JAVA_CMD} {instance_path} -countlogger"
    try:
        # Run the command with timeout
        result = subprocess.run(
            full_cmd,
            shell=True,
            capture_output=True,
            text=True,
            timeout=TIMEOUT,
            cwd=os.path.join(os.path.dirname(os.path.abspath(__file__)), "solver/numpart")
        )

        if result.returncode != 0:
            print(f"Error running command for {instance_path}: {result.stderr}")
            return {"file": instance_path, "time": "unlimited"}

        return parse_output(result.stdout, instance_path)
    except subprocess.TimeoutExpired:
        print(f"Timeout expired for {instance_path}")
        return {"file": instance_path, "time": "unlimited"}
    except Exception as e:
        print(f"Exception running command for {instance_path}: {e}")
        return {"file": instance_path, "time": "unlimited"}

def parse_output(output, instance_path):
    """
    Parse the Java command output to extract required information.

    Args:
        output: String containing the command output
        instance_path: Path to the instance file

    Returns:
        Dictionary with the extracted data
    """
    result = {"file": instance_path}

    # Extract JSON stats
    json_match = re.search(r'(\{.*?\})', output)
    if json_match:
        try:
            stats = json.loads(json_match.group(1))
            result.update(stats)
        except json.JSONDecodeError:
            print(f"Error parsing JSON for {instance_path}")

    # Helper function to parse values and handle exceptions
    def parse_pattern(pattern, key, convert_func):
        match = re.search(pattern, output)
        if match:
            try:
                result[key] = convert_func(match.group(1))
            except Exception as e:
                print(f"Error parsing {key} for {instance_path}: {e}")

    # Extract array values
    parse_pattern(r'Sums: \[(.*?)\]', "sums",
                 lambda x: [int(val.strip()) for val in x.split(',')])
    parse_pattern(r'Pi: \[(.*?)\]', "pi",
                 lambda x: [int(val.strip()) for val in x.split(',')])

    # Extract scalar values
    parse_pattern(r'MaxSum: (\d+)', "maxsum", int)
    parse_pattern(r'Time: (\d+)ms', "time", int)

    return result

def find_synthetic_instances():
    """
    Find all synthetic instance files.

    Returns:
        List of relative paths to synthetic instance files
    """
    synthetic_path = os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        "solver/numpart",
        SYNTHETIC_DIR
    )

    # Create solved directory if it doesn't exist
    solved_path = os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        "solver/numpart",
        SYNTHETIC_SOLVED_DIR
    )
    if not os.path.exists(solved_path):
        os.makedirs(solved_path)

    # List comprehension is more concise
    instances = [
        os.path.join(SYNTHETIC_DIR, file)
        for file in os.listdir(synthetic_path)
        if file.endswith(".txt")
    ]

    return sorted(instances)

def main():
    print("Starting data collection...")

    # Create output directory if it doesn't exist
    output_path = os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        OUTPUT_JSON
    )

    # Find all synthetic instances
    instances = find_synthetic_instances()
    print(f"Found {len(instances)} synthetic instances")

    # Initialize the output file with an empty array if it doesn't exist
    if not os.path.exists(output_path):
        with open(output_path, 'w') as f:
            json.dump([], f)

    # Process each instance and save results incrementally
    for i, instance in enumerate(instances):
        print(f"Processing {i+1}/{len(instances)}: {instance}")
        result = run_command(instance)

        # Load existing results
        with open(output_path, 'r') as f:
            results = json.load(f)

        # Append new result and save
        results.append(result)
        with open(output_path, 'w') as f:
            json.dump(results, f, indent=2)

        # Move the processed file to the synthetic_solved directory
        file_name = os.path.basename(instance)
        source_path = os.path.join(
            os.path.dirname(os.path.abspath(__file__)),
            "solver/numpart",
            instance
        )
        dest_path = os.path.join(
            os.path.dirname(os.path.abspath(__file__)),
            "solver/numpart",
            SYNTHETIC_SOLVED_DIR,
            file_name
        )
        shutil.move(source_path, dest_path)

        print(f"Saved result for {instance} and moved to {SYNTHETIC_SOLVED_DIR}")

    print(f"Data collection complete. Results saved to {output_path}")

if __name__ == "__main__":
    main()
