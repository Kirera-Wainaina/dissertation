#!/usr/bin/env python3
"""
Script to extract ML features from NumPartV2o runs on synthetic instances.
This is the second stage of the process, where we extract log items equal to the
number of partitions (k) from each solver run and store them as features for machine learning.

Memory-efficient implementation using JSON Lines format.
"""

import os
import json
import subprocess
import re
import shutil

# Configuration variables
SOLVER_TIMEOUT = 60 * 5  # Solver timeout in seconds (5 minutes)
SUBPROCESS_TIMEOUT = SOLVER_TIMEOUT + 30  # Add 30 seconds buffer for proper shutdown
TIMEOUT_MS = SOLVER_TIMEOUT * 1000  # Convert to milliseconds for solver command
RUN_CMD = "./run.sh NumPartV2o"
SYNTHETIC_SOLVED_DIR = "instances/synthetic_solved"
FEATURE_COLLECTED_DIR = "instances/feature_collected"
OUTPUT_JSONL = "ml_features.jsonl"
MAX_RETRIES = 3  # Maximum number of retries for each instance

def get_num_partitions(instance_path):
    """
    Extract the number of partitions (k) from the instance file.

    Args:
        instance_path: Relative path to the instance file from numpart dir

    Returns:
        int: Number of partitions (k), defaults to 3 if not found
    """
    try:
        full_path = os.path.join(
            os.path.dirname(os.path.abspath(__file__)),
            "solver/numpart",
            instance_path
        )

        if not os.path.exists(full_path):
            print(f"Warning: Instance file {full_path} not found")
            return 3  # Default fallback

        with open(full_path, 'r') as f:
            # Skip first line
            f.readline()
            # Second line contains the number of partitions
            k = int(f.readline().strip())
            print(f"Found {k} partitions in instance file")
            return k
    except Exception as e:
        print(f"Error reading partitions from {instance_path}: {e}")
        return 3  # Default fallback

def run_command(instance_path, retry_count=0):
    """
    Run the solver command on the given instance with a timeout.

    Args:
        instance_path: Relative path to the instance file from numpart dir
        retry_count: Current retry attempt number

    Returns:
        Dictionary with the file name and log items.
        Contains the first 'num_partitions' log entries plus any TIMEOUT or TERMINATE events.
    """
    file_name = os.path.basename(instance_path)
    # Get number of partitions to determine how many log entries to collect
    num_partitions = get_num_partitions(instance_path)
    full_cmd = f"{RUN_CMD} {instance_path} -countlogger -timeout={TIMEOUT_MS}ms -stackdepth={3}"
    try:
        print(f"Running command: {full_cmd}")
        # Run the command with timeout
        result = subprocess.run(
            full_cmd,
            shell=True,
            capture_output=True,
            text=True,
            timeout=SUBPROCESS_TIMEOUT,
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

        return parse_output(result.stdout, file_name, num_partitions)
    except subprocess.TimeoutExpired:
        print(f"Python subprocess timeout expired for {instance_path} - this should be rare since we have a buffer over the solver timeout")
        return {file_name: []}
    except Exception as e:
        print(f"Exception running command for {instance_path}: {e}")
        if retry_count < MAX_RETRIES:
            print(f"Retrying ({retry_count + 1}/{MAX_RETRIES})...")
            return run_command(instance_path, retry_count + 1)
        return {file_name: []}

def parse_output(output, file_name, num_partitions=3):
    """
    Parse the solver command output to extract log items based on the number of partitions.

    Args:
        output: String containing the command output
        file_name: Name of the instance file
        num_partitions: Number of partitions (k) to determine how many log entries to extract

    Returns:
        Dictionary with the file name and log items.
        Contains the first 'num_partitions' log entries plus any TIMEOUT or TERMINATE events.
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
                # Check for important events (TIMEOUT, TERMINATE) in the log entries
                timeout_entry = None
                terminate_entry = None

                for entry in log_entries:
                    if isinstance(entry, dict):
                        if entry.get("event") == "TIMEOUT":
                            print("Solver timeout detected in log entry")
                            timeout_entry = entry
                        elif entry.get("event") == "TERMINATE":
                            print("Solver terminate event detected in log entry")
                            terminate_entry = entry

                # Build our collection of log items to return
                # Start with the initial entries up to num_partitions
                log_items = log_entries[:num_partitions]

                # If we have a TIMEOUT or TERMINATE event that isn't already included, add it
                if timeout_entry and timeout_entry not in log_items:
                    log_items.append(timeout_entry)
                    print("Added TIMEOUT event to log items")

                if terminate_entry and terminate_entry not in log_items:
                    log_items.append(terminate_entry)
                    print("Added TERMINATE event to log items")

                if log_items:
                    print(f"Successfully extracted {len(log_items)} log entries from {len(log_entries)} total")
                    return {file_name: log_items}
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

def get_processed_files(output_path):
    """
    Read the JSONL file to determine which files have already been processed.

    Args:
        output_path: Path to the JSONL output file

    Returns:
        Set of filenames that have already been processed
    """
    processed_files = set()

    if not os.path.exists(output_path):
        return processed_files

    try:
        with open(output_path, 'r') as f:
            for line in f:
                try:
                    data = json.loads(line.strip())
                    # Each line contains a single key (filename)
                    processed_files.update(data.keys())
                except json.JSONDecodeError:
                    # Skip invalid lines
                    continue

        print(f"Found {len(processed_files)} already processed files")
    except Exception as e:
        print(f"Error reading processed files: {e}")

    return processed_files

def main():
    print("Starting ML feature collection...")

    script_dir = os.path.dirname(os.path.abspath(__file__))
    print(f"Script directory: {script_dir}")

    solver_dir = os.path.join(script_dir, "solver/numpart")
    print(f"Solver directory: {solver_dir}")

    if not os.path.exists(solver_dir):
        print(f"ERROR: Solver directory {solver_dir} does not exist!")
        return

    # Output file path
    output_path = os.path.join(script_dir, OUTPUT_JSONL)
    print(f"Output file: {output_path}")

    # Find all synthetic instances in the solved directory
    instances = find_instances()
    if not instances:
        print("No instances found to process. Exiting.")
        return

    print(f"Found {len(instances)} solved instances to process for ML features")

    # Get list of already processed files (memory efficient)
    processed_files = get_processed_files(output_path)

    # Process each instance and append results incrementally
    success_count = 0
    for i, instance in enumerate(instances):
        file_name = os.path.basename(instance)

        # Skip if already processed
        if file_name in processed_files:
            print(f"Skipping {i+1}/{len(instances)}: {file_name} (already processed)")
            continue

        print(f"\nProcessing {i+1}/{len(instances)}: {instance}")
        result = run_command(instance)

        # Check if we got valid results
        if not result[file_name]:
            print(f"WARNING: No valid features extracted for {file_name}")
        else:
            success_count += 1

            # Append this result to the JSONL file (no need to read the file)
            try:
                with open(output_path, 'a') as f:
                    json_line = json.dumps(result)
                    f.write(json_line + '\n')
                print(f"Appended result for {file_name} to {output_path}")
            except Exception as e:
                print(f"ERROR saving result for {file_name}: {e}")
                # Create a backup for this specific result
                backup_path = os.path.join(script_dir, "backup_results", f"{file_name}.json")
                os.makedirs(os.path.dirname(backup_path), exist_ok=True)
                with open(backup_path, 'w') as f:
                    json.dump(result, f)
                print(f"Saved backup to {backup_path}")

        # Move the processed file to the feature_collected directory
        try:
            source_path = os.path.join(
                script_dir,
                "solver/numpart",
                instance
            )
            dest_path = os.path.join(
                script_dir,
                "solver/numpart",
                FEATURE_COLLECTED_DIR,
                file_name
            )

            if os.path.exists(source_path):
                shutil.move(source_path, dest_path)
                print(f"Moved {file_name} to {FEATURE_COLLECTED_DIR}")
            else:
                print(f"WARNING: Source file {source_path} does not exist")
        except Exception as e:
            print(f"ERROR moving file: {e}")

    print("\nML feature collection complete.")
    print(f"Successfully processed {success_count} out of {len(instances)} instances.")
    print(f"Results saved to {output_path}")

if __name__ == "__main__":
    main()
