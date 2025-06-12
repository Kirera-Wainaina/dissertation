#!/usr/bin/env python3
"""
Script to extract ML features from NumPartV2o runs on synthetic instances.
This is the second stage of the process, where we extract the first 3 log items
from each solver run and store them as features for machine learning.
"""

import os
import json
import subprocess
import re
# import shutil

# Configuration variables
TIMEOUT = 60 * 5  # Timeout in seconds (5 minutes)
RUN_CMD = "./run.sh NumPartV2o"
SYNTHETIC_SOLVED_DIR = "instances/synthetic_solved"
FEATURE_COLLECTED_DIR = "instances/feature_collected"
OUTPUT_JSON = "ml_features.json"
MAX_RETRIES = 3  # Maximum number of retries for each instance

def run_command(instance_path, retry_count=0):
    """
    Run the solver command on the given instance with a timeout.

    Args:
        instance_path: Relative path to the instance file from numpart dir
        retry_count: Current retry attempt number

    Returns:
        Dictionary with the file name and first 3 log items
    """
    file_name = os.path.basename(instance_path)
    full_cmd = f"{RUN_CMD} {instance_path} -countlogger -stackdepth=3"
    try:
        print(f"Running command: {full_cmd}")
        # Run the command with timeout
        result = subprocess.run(
            full_cmd,
            shell=True,
            capture_output=True,
            text=True,
            timeout=TIMEOUT,
            cwd=os.path.join(os.path.dirname(os.path.abspath(__file__)), "solver/numpart")
        )

        # Check if we got any output at all
        if not result.stdout.strip():
            print(f"Warning: Empty output for {instance_path}")
            if retry_count < MAX_RETRIES:
                print(f"Retrying ({retry_count + 1}/{MAX_RETRIES})...")
                return run_command(instance_path, retry_count + 1)
            return {file_name: []}

        # Check if output contains a JSON structure with a log section
        if '{' not in result.stdout or '}' not in result.stdout or '"log":' not in result.stdout:
            print(f"Warning: No valid JSON with log section found in output for {instance_path}")
            print(f"Output: {result.stdout[:200]}...")
            if retry_count < MAX_RETRIES:
                print(f"Retrying ({retry_count + 1}/{MAX_RETRIES})...")
                return run_command(instance_path, retry_count + 1)
            return {file_name: []}

        print("JSON output received with log section")

        if result.returncode != 0:
            print(f"Error running command for {instance_path}: {result.stderr}")
            if retry_count < MAX_RETRIES:
                print(f"Retrying ({retry_count + 1}/{MAX_RETRIES})...")
                return run_command(instance_path, retry_count + 1)
            return {file_name: []}

        return parse_output(result.stdout, file_name)
    except subprocess.TimeoutExpired:
        print(f"Timeout expired for {instance_path}")
        return {file_name: []}
    except Exception as e:
        print(f"Exception running command for {instance_path}: {e}")
        if retry_count < MAX_RETRIES:
            print(f"Retrying ({retry_count + 1}/{MAX_RETRIES})...")
            return run_command(instance_path, retry_count + 1)
        return {file_name: []}

def parse_output(output, file_name):
    """
    Parse the solver command output to extract the first 3 log items.

    Args:
        output: String containing the command output
        file_name: Name of the instance file

    Returns:
        Dictionary with the file name and first 3 log items
    """
    # Save the raw output for debugging
    debug_output_path = os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        "debug_outputs"
    )
    if not os.path.exists(debug_output_path):
        os.makedirs(debug_output_path)

    with open(os.path.join(debug_output_path, f"{file_name}.log"), 'w') as f:
        f.write(output)

    # Extract and parse JSON from the command output
    try:
        # Find the complete JSON object from the first '{' to the last '}'
        start_idx = output.find('{')
        end_idx = output.rfind('}')

        if start_idx >= 0 and end_idx > start_idx:
            # Extract the complete JSON string
            json_str = output[start_idx:end_idx+1]

            # Fix common JSON formatting issues
            # 1. Remove trailing commas before closing brackets or braces
            json_str = re.sub(r',(\s*[\]}])', r'\1', json_str)
            # 2. Handle any newlines inside JSON strings if needed
            json_str = json_str.replace('\n', ' ').replace('\r', '')

            # Parse the JSON
            data = json.loads(json_str)

            # Verify and extract the log entries
            if 'log' in data and isinstance(data['log'], list):
                log_entries = data['log']
                # Get the first 3 log entries
                first_three = log_entries[:3]
                if first_three:
                    print(f"Successfully extracted {len(first_three)} log entries from {len(log_entries)} total")
                    return {file_name: first_three}
                else:
                    print(f"Warning: Log array was empty for {file_name}")
                    print(f"Full JSON keys: {list(data.keys())}")
            else:
                print(f"Warning: No valid 'log' array found in JSON for {file_name}")
                print(f"Available keys in JSON: {list(data.keys())}")
        else:
            print(f"Warning: Could not locate complete JSON object in output for {file_name}")
            print(f"Output begins with: {output[:50]}...")

    except json.JSONDecodeError as e:
        print(f"JSON parsing error for {file_name}: {e}")
        # print(f"Problematic JSON: {json_str[:100]}...")
        # Try to identify specific JSON syntax error
        line_col = getattr(e, 'lineno', 0), getattr(e, 'colno', 0)
        print(f"Error at line {line_col[0]}, column {line_col[1]}")
    except Exception as e:
        print(f"Unexpected error processing output for {file_name}: {str(e)}")
        print(f"Output sample: {output[:100]}...")

    # If we get here, we couldn't extract the log entries
    print(f"Warning: No log entries extracted for {file_name}")
    return {file_name: []}

def find_instances():
    """
    Find all synthetic instance files in the synthetic_solved directory.

    Returns:
        List of relative paths to synthetic solved instance files
    """
    solved_path = os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        "solver/numpart",
        SYNTHETIC_SOLVED_DIR
    )

    # Ensure the solved directory exists
    if not os.path.exists(solved_path):
        print(f"Warning: {solved_path} does not exist!")
        return []

    # Create feature_collected directory if it doesn't exist
    feature_collected_path = os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        "solver/numpart",
        FEATURE_COLLECTED_DIR
    )
    if not os.path.exists(feature_collected_path):
        os.makedirs(feature_collected_path)
        print(f"Created directory: {feature_collected_path}")

    # Create debug outputs directory if it doesn't exist
    debug_output_path = os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        "debug_outputs"
    )
    if not os.path.exists(debug_output_path):
        os.makedirs(debug_output_path)
        print(f"Created directory: {debug_output_path}")

    # List all instances in the solved directory
    try:
        instances = [
            os.path.join(SYNTHETIC_SOLVED_DIR, file)
            for file in os.listdir(solved_path)
            if file.endswith(".txt")
        ]
        print(f"Found {len(instances)} instance files in {solved_path}")
        return sorted(instances)
    except Exception as e:
        print(f"Error listing files in {solved_path}: {e}")
        return []

def main():
    print("Starting ML feature collection...")

    script_dir = os.path.dirname(os.path.abspath(__file__))
    print(f"Script directory: {script_dir}")

    solver_dir = os.path.join(script_dir, "solver/numpart")
    print(f"Solver directory: {solver_dir}")

    if not os.path.exists(solver_dir):
        print(f"ERROR: Solver directory {solver_dir} does not exist!")
        return

    # Create output directory if it doesn't exist
    output_path = os.path.join(
        script_dir,
        OUTPUT_JSON
    )

    # Find all synthetic instances in the solved directory
    instances = find_instances()
    if not instances:
        print("No instances found to process. Exiting.")
        return

    print(f"Found {len(instances)} solved instances to process for ML features")

    # Initialize the output file with an empty dict if it doesn't exist
    if not os.path.exists(output_path):
        with open(output_path, 'w') as f:
            json.dump({}, f)
        print(f"Created new output file: {output_path}")

    # Load existing results
    try:
        with open(output_path, 'r') as f:
            results = json.load(f)
        print(f"Loaded existing results with {len(results)} entries")
    except (json.JSONDecodeError, FileNotFoundError) as e:
        print(f"Creating new output file due to error: {e}")
        results = {}

    # Process each instance and save results incrementally
    success_count = 0
    for i, instance in enumerate(instances):
        file_name = os.path.basename(instance)

        # Skip if already processed
        if file_name in results:
            print(f"Skipping {i+1}/{len(instances)}: {file_name} (already processed)")
            continue

        print(f"\nProcessing {i+1}/{len(instances)}: {instance}")
        result = run_command(instance)

        # Check if we got valid results
        if not result[file_name]:
            print(f"WARNING: No valid features extracted for {file_name}")
        else:
            success_count += 1

        # Update results dictionary with new data
        results.update(result)

        # Save updated results after each file (in case of crashes)
        try:
            with open(output_path, 'w') as f:
                json.dump(results, f, indent=2)
            print(f"Saved results to {output_path}")
        except Exception as e:
            print(f"ERROR saving results: {e}")
            # Create a backup in case the main file is corrupted
            backup_path = f"{output_path}.bak"
            with open(backup_path, 'w') as f:
                json.dump(results, f, indent=2)
            print(f"Saved backup to {backup_path}")

        # Move the processed file to the feature_collected directory
        # try:
        #     source_path = os.path.join(
        #         script_dir,
        #         "solver/numpart",
        #         instance
        #     )
        #     dest_path = os.path.join(
        #         script_dir,
        #         "solver/numpart",
        #         FEATURE_COLLECTED_DIR,
        #         file_name
        #     )

        #     if os.path.exists(source_path):
        #         shutil.move(source_path, dest_path)
        #         print(f"Moved {file_name} to {FEATURE_COLLECTED_DIR}")
        #     else:
        #         print(f"WARNING: Source file {source_path} does not exist")
        # except Exception as e:
        #     print(f"ERROR moving file: {e}")

    print("\nML feature collection complete.")
    print(f"Successfully processed {success_count} out of {len(instances)} instances.")
    print(f"Results saved to {output_path}")

if __name__ == "__main__":
    main()
    # run_command()
