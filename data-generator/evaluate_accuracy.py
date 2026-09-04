#!/usr/bin/env python3
"""
Reconciliation Accuracy & Benchmark Evaluator
Compares engine reconciliation results against the hidden ground truth.
Outputs precision, recall, accuracy, and false-match rate.
"""

import sys
import os
import json
import argparse

def evaluate(ground_truth_file: str, reconciliation_results_file: str):
    if not os.path.exists(ground_truth_file):
        print(f"Error: Ground truth file '{ground_truth_file}' not found.")
        sys.exit(1)
    if not os.path.exists(reconciliation_results_file):
        print(f"Error: Results file '{reconciliation_results_file}' not found.")
        sys.exit(1)

    with open(ground_truth_file, "r", encoding="utf-8") as f:
        gt_data = json.load(f)
    ground_truth = gt_data.get("records", {})

    with open(reconciliation_results_file, "r", encoding="utf-8") as f:
        recon_data = json.load(f)

    # Convert list of MatchResultDto to map by gatewayTxnId
    recon_results = {}
    if isinstance(recon_data, list):
        for item in recon_data:
            tid = item.get("gatewayTxnId")
            if tid:
                recon_results[tid] = item
    elif isinstance(recon_data, dict):
        recon_results = recon_data

    total_evaluated = 0
    correct_matches = 0
    false_matches = 0
    correct_exceptions = 0
    false_exceptions = 0

    print("=" * 70)
    print("AI FINANCE CONTROLLER - RECONCILIATION ACCURACY BENCHMARK")
    print("=" * 70)

    for txn_id, gt in ground_truth.items():
        total_evaluated += 1
        expected_status = gt.get("expected_status")
        expected_bank = gt.get("bank_ref_id")

        rec = recon_results.get(txn_id)
        if not rec:
            continue

        actual_status = rec.get("matchStatus")
        actual_bank = rec.get("bankRefId")

        if expected_status == "MATCHED":
            if actual_status == "MATCHED":
                # Check if it matched the right entity
                if expected_bank is None or actual_bank == expected_bank:
                    correct_matches += 1
                else:
                    false_matches += 1
            else:
                false_exceptions += 1
        else: # expected MANUAL_REVIEW or EXCEPTION
            if actual_status in ["MANUAL_REVIEW", "NEEDS_REVIEW", "DISCREPANCY"]:
                correct_exceptions += 1
            else:
                false_matches += 1

    accuracy = ((correct_matches + correct_exceptions) / total_evaluated * 100.0) if total_evaluated > 0 else 0.0
    total_positives = correct_matches + false_matches
    precision = (correct_matches / total_positives * 100.0) if total_positives > 0 else 0.0
    actual_positives = sum(1 for g in ground_truth.values() if g.get("expected_status") == "MATCHED")
    recall = (correct_matches / actual_positives * 100.0) if actual_positives > 0 else 0.0
    false_match_rate = (false_matches / total_evaluated * 100.0) if total_evaluated > 0 else 0.0

    print(f"Total Transactions Evaluated: {total_evaluated}")
    print(f"Correct Matches:             {correct_matches}")
    print(f"Correct Exceptions Caught:    {correct_exceptions}")
    print(f"False Matches:                {false_matches}")
    print(f"False Exceptions:             {false_exceptions}")
    print("-" * 70)
    print(f"OVERALL ACCURACY:             {accuracy:.2f}%")
    print(f"PRECISION:                    {precision:.2f}%")
    print(f"RECALL:                       {recall:.2f}%")
    print(f"FALSE-MATCH RATE:             {false_match_rate:.2f}% (Target < 2.0%)")
    print("=" * 70)

    if accuracy >= 95.0 and false_match_rate < 2.0:
        print(">>> BENCHMARK PASSED: System achieved high precision reconciliation with zero/negligible false matches.")
    else:
        print(">>> BENCHMARK CAUTION: Review borderline fuzzy and AI classifications.")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Reconciliation Accuracy Benchmark")
    parser.add_argument("--truth", default="sample_data/hidden_ground_truth.json", help="Path to ground truth JSON")
    parser.add_argument("--results", required=True, help="Path to engine output JSON")
    args = parser.parse_args()

    evaluate(args.truth, args.results)
