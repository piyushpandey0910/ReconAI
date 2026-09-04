#!/usr/bin/env python3
"""
Synthetic Financial Data Generator
Produces realistic, messy multi-source financial datasets across:
1. gateway_settlements.csv (Payment Gateway)
2. bank_statement.csv (Bank Statement)
3. internal_ledger.csv (Internal General Ledger)
4. hidden_ground_truth.json (Hidden evaluation key for accuracy scoring)
"""

import os
import csv
import json
import random
from datetime import datetime, timedelta

random.seed(42)

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "sample_data")
os.makedirs(OUTPUT_DIR, exist_ok=True)

CUSTOMERS = [
    ("Alice Smith", "alice.smith@acme.com"),
    ("Bob Johnson", "bob.j@fintech.io"),
    ("Charlie Brown", "charlie@peanuts.org"),
    ("Diana Prince", "diana@themyscira.net"),
    ("Evan Wright", "evan.wright@enterprise.com"),
    ("Fiona Gallagher", "fiona@gallagher.co"),
    ("George Miller", "gmiller@apex.org"),
    ("Hannah Abbott", "hannah@hogwarts.edu"),
    ("Ian Malcolm", "ian.malcolm@chaos.com"),
    ("Julia Roberts", "jroberts@cinema.org"),
    ("Kevin Bacon", "kbacon@sixdegrees.com"),
    ("Laura Croft", "laura@tomb.org")
]

BASE_DATE = datetime(2026, 8, 15, 10, 0, 0)

gateway_rows = []
bank_rows = []
ledger_rows = []
ground_truth = {}

rec_idx = 100

# ==========================================================
# CATEGORY 1: EXACT MATCHES (32 records)
# Exactly matching IDs, amounts, and dates across all 3 systems
# ==========================================================
for i in range(32):
    rec_idx += 1
    txn_id = f"GW_TXN_{rec_idx}"
    order_id = f"ORD_{20000 + rec_idx}"
    amount = round(random.uniform(25.0, 850.0), 2)
    cust_name, cust_email = random.choice(CUSTOMERS)
    dt = BASE_DATE + timedelta(days=i // 4, hours=random.randint(0, 12))
    date_str = dt.strftime("%Y-%m-%d")

    # Gateway Record
    gateway_rows.append({
        "transactionId": txn_id,
        "amount": f"{amount:.2f}",
        "currency": "USD",
        "status": "SUCCESS",
        "paymentMethod": random.choice(["CARD", "ACH", "APPLE_PAY"]),
        "orderId": order_id,
        "timestamp": dt.strftime("%Y-%m-%d %H:%M:%S"),
        "customerEmail": cust_email
    })

    # Bank Record
    bank_ref = f"BNK_REF_{rec_idx}"
    bank_rows.append({
        "bankRefId": bank_ref,
        "amount": f"{amount:.2f}",
        "type": "CREDIT",
        "narration": f"SETTLEMENT {order_id} REF {txn_id}",
        "date": date_str,
        "fee": "0.00"
    })

    # Ledger Record
    ledger_entry = f"LEDGER_{rec_idx}"
    ledger_rows.append({
        "ledgerEntryId": ledger_entry,
        "internalRef": order_id,
        "amount": f"{amount:.2f}",
        "accountCode": "1010-REVENUE",
        "description": f"Customer Payment {order_id}",
        "entryDate": date_str
    })

    ground_truth[txn_id] = {
        "expected_status": "MATCHED",
        "expected_pass": "PASS1_RULES",
        "rule_type": "EXACT_ID_AND_AMOUNT",
        "bank_ref_id": bank_ref,
        "ledger_entry_id": ledger_entry,
        "amount": amount
    }

# ==========================================================
# CATEGORY 2: AMOUNT + DATE WINDOW MATCH (14 records)
# Matching amount, bank/ledger clearing delayed by 1-2 days
# ==========================================================
for i in range(14):
    rec_idx += 1
    txn_id = f"GW_TXN_{rec_idx}"
    order_id = f"ORD_{20000 + rec_idx}"
    amount = round(random.uniform(50.0, 1200.0), 2)
    cust_name, cust_email = random.choice(CUSTOMERS)
    gw_dt = BASE_DATE + timedelta(days=8 + (i // 3))
    bank_dt = gw_dt + timedelta(days=random.choice([1, 2])) # 1-2 days lag

    gateway_rows.append({
        "transactionId": txn_id,
        "amount": f"{amount:.2f}",
        "currency": "USD",
        "status": "SUCCESS",
        "paymentMethod": "CARD",
        "orderId": order_id,
        "timestamp": gw_dt.strftime("%Y-%m-%d %H:%M:%S"),
        "customerEmail": cust_email
    })

    bank_ref = f"BNK_REF_{rec_idx}"
    bank_rows.append({
        "bankRefId": bank_ref,
        "amount": f"{amount:.2f}",
        "type": "CREDIT",
        "narration": f"STRIPE TRANSFER {order_id}",
        "date": bank_dt.strftime("%Y-%m-%d"),
        "fee": "0.00"
    })

    ledger_entry = f"LEDGER_{rec_idx}"
    ledger_rows.append({
        "ledgerEntryId": ledger_entry,
        "internalRef": order_id,
        "amount": f"{amount:.2f}",
        "accountCode": "1010-REVENUE",
        "description": f"Online Sales Order {order_id}",
        "entryDate": gw_dt.strftime("%Y-%m-%d")
    })

    ground_truth[txn_id] = {
        "expected_status": "MATCHED",
        "expected_pass": "PASS1_RULES",
        "rule_type": "AMOUNT_DATE_WINDOW",
        "bank_ref_id": bank_ref,
        "ledger_entry_id": ledger_entry,
        "amount": amount
    }

# ==========================================================
# CATEGORY 3: FUZZY NARRATION & STANDARD FEE VARIANCE (8 records)
# Bank amount has 2.5% standard gateway processing fee deducted
# ==========================================================
for i in range(8):
    rec_idx += 1
    txn_id = f"GW_TXN_{rec_idx}"
    order_id = f"ORD_{20000 + rec_idx}"
    gross_amount = round(random.uniform(100.0, 900.0), 2)
    fee_amount = round(gross_amount * 0.025, 2)
    net_bank_amount = round(gross_amount - fee_amount, 2)
    cust_name, cust_email = random.choice(CUSTOMERS)
    dt = BASE_DATE + timedelta(days=14 + i)

    gateway_rows.append({
        "transactionId": txn_id,
        "amount": f"{gross_amount:.2f}",
        "currency": "USD",
        "status": "SUCCESS",
        "paymentMethod": "CARD",
        "orderId": order_id,
        "timestamp": dt.strftime("%Y-%m-%d %H:%M:%S"),
        "customerEmail": cust_email
    })

    bank_ref = f"BNK_REF_{rec_idx}"
    email_user = cust_email.split("@")[0]
    bank_rows.append({
        "bankRefId": bank_ref,
        "amount": f"{net_bank_amount:.2f}",
        "type": "CREDIT",
        "narration": f"ACH PAYOUT {email_user} NET OF FEES",
        "date": dt.strftime("%Y-%m-%d"),
        "fee": f"{fee_amount:.2f}"
    })

    ledger_entry = f"LEDGER_{rec_idx}"
    ledger_rows.append({
        "ledgerEntryId": ledger_entry,
        "internalRef": order_id,
        "amount": f"{gross_amount:.2f}",
        "accountCode": "1010-REVENUE",
        "description": f"Gross sales for {email_user}",
        "entryDate": dt.strftime("%Y-%m-%d")
    })

    ground_truth[txn_id] = {
        "expected_status": "MATCHED",
        "expected_pass": "PASS1_RULES",
        "rule_type": "FUZZY_NARRATION_AMOUNT",
        "bank_ref_id": bank_ref,
        "ledger_entry_id": ledger_entry,
        "amount": gross_amount
    }

# ==========================================================
# CATEGORY 4: AMBIGUOUS CASES RESOLVED BY AI (PASS 2) (6 records)
# Truncated vendor names, non-standard processing fee, delayed batch
# ==========================================================
ambiguous_cases = [
    ("Acme Global Inc Trunc", 420.00, 407.40, 12.60, "FEE_DISCREPANCY"),
    ("Fintech Solution Intl", 750.00, 727.50, 22.50, "FEE_DISCREPANCY"),
    ("Charlie Brown Peanuts", 310.00, 310.00, 0.00, "NAME_VARIATION"),
    ("Enterprise Client Corp", 890.00, 890.00, 0.00, "DELAYED_SETTLEMENT"),
    ("Apex Dynamics Global", 560.00, 543.20, 16.80, "FEE_DISCREPANCY"),
    ("Gallagher Logistics Ltd", 680.00, 680.00, 0.00, "NAME_VARIATION"),
]

for name, gross, net, fee, case_type in ambiguous_cases:
    rec_idx += 1
    txn_id = f"GW_TXN_{rec_idx}"
    order_id = f"ORD_{20000 + rec_idx}"
    dt = BASE_DATE + timedelta(days=22)

    gateway_rows.append({
        "transactionId": txn_id,
        "amount": f"{gross:.2f}",
        "currency": "USD",
        "status": "SUCCESS",
        "paymentMethod": "CARD",
        "orderId": order_id,
        "timestamp": dt.strftime("%Y-%m-%d %H:%M:%S"),
        "customerEmail": f"billing@{name.lower().replace(' ', '')}.com"
    })

    bank_ref = f"BNK_REF_{rec_idx}"
    bank_rows.append({
        "bankRefId": bank_ref,
        "amount": f"{net:.2f}",
        "type": "CREDIT",
        "narration": f"WIRE TRF {name[:12].upper()} BATCH CLEARING",
        "date": (dt + timedelta(days=2)).strftime("%Y-%m-%d"),
        "fee": f"{fee:.2f}"
    })

    ledger_entry = f"LEDGER_{rec_idx}"
    ledger_rows.append({
        "ledgerEntryId": ledger_entry,
        "internalRef": order_id,
        "amount": f"{gross:.2f}",
        "accountCode": "1010-REVENUE",
        "description": f"Invoice Settlement {name}",
        "entryDate": dt.strftime("%Y-%m-%d")
    })

    ground_truth[txn_id] = {
        "expected_status": "MATCHED",
        "expected_pass": "PASS2_AI",
        "rule_type": f"GROQ_AI_{case_type}",
        "bank_ref_id": bank_ref,
        "ledger_entry_id": ledger_entry,
        "amount": gross
    }

# ==========================================================
# CATEGORY 5: GENUINE ANOMALIES & MANUAL REVIEW ITEMS (5 records)
# Fraud duplicates, missing bank credit, customer chargeback
# ==========================================================
# 1. Missing Bank Credit (Gateway recorded, Bank never received)
rec_idx += 1
txn_missing = f"GW_TXN_{rec_idx}"
gateway_rows.append({
    "transactionId": txn_missing,
    "amount": "999.00",
    "currency": "USD",
    "status": "SUCCESS",
    "paymentMethod": "WIRE",
    "orderId": f"ORD_{20000 + rec_idx}",
    "timestamp": BASE_DATE.strftime("%Y-%m-%d %H:%M:%S"),
    "customerEmail": "stuck.funds@anomaly.io"
})
ground_truth[txn_missing] = {
    "expected_status": "MANUAL_REVIEW",
    "expected_pass": "PASS2_AI",
    "rule_type": "UNMATCHED_MISSING_DEPOSIT",
    "bank_ref_id": None,
    "ledger_entry_id": None,
    "amount": 999.00
}

# 2. Duplicate Gateway Charge (Only 1 bank deposit)
rec_idx += 1
txn_dup = f"GW_TXN_{rec_idx}"
gateway_rows.append({
    "transactionId": txn_dup,
    "amount": "150.00",
    "currency": "USD",
    "status": "SUCCESS",
    "paymentMethod": "CARD",
    "orderId": f"ORD_{20000 + rec_idx}",
    "timestamp": (BASE_DATE + timedelta(minutes=2)).strftime("%Y-%m-%d %H:%M:%S"),
    "customerEmail": "double.charge@victim.com"
})
ground_truth[txn_dup] = {
    "expected_status": "MANUAL_REVIEW",
    "expected_pass": "PASS2_AI",
    "rule_type": "DUPLICATE_CHARGE",
    "bank_ref_id": None,
    "ledger_entry_id": None,
    "amount": 150.00
}

# 3, 4, 5. Unresolved Discrepancies
for i in range(3):
    rec_idx += 1
    txn_discrep = f"GW_TXN_{rec_idx}"
    amt = 350.00 + i * 50.0
    gateway_rows.append({
        "transactionId": txn_discrep,
        "amount": f"{amt:.2f}",
        "currency": "USD",
        "status": "DISPUTED",
        "paymentMethod": "CARD",
        "orderId": f"ORD_{20000 + rec_idx}",
        "timestamp": (BASE_DATE + timedelta(days=25)).strftime("%Y-%m-%d %H:%M:%S"),
        "customerEmail": f"disputed_{i}@chargeback.com"
    })
    ground_truth[txn_discrep] = {
        "expected_status": "MANUAL_REVIEW",
        "expected_pass": "PASS2_AI",
        "rule_type": "CHARGEBACK_DISPUTE",
        "bank_ref_id": None,
        "ledger_entry_id": None,
        "amount": amt
    }

# Shuffle bank and ledger rows so matching engine has to work to find them!
random.shuffle(bank_rows)
random.shuffle(ledger_rows)

# Write CSV files
def write_csv(filename, rows):
    path = os.path.join(OUTPUT_DIR, filename)
    if not rows: return
    with open(path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)
    print(f"Generated {filename}: {len(rows)} records")

write_csv("gateway_settlements.csv", gateway_rows)
write_csv("bank_statement.csv", bank_rows)
write_csv("internal_ledger.csv", ledger_rows)

# Write Hidden Ground Truth
gt_path = os.path.join(OUTPUT_DIR, "hidden_ground_truth.json")
with open(gt_path, "w", encoding="utf-8") as f:
    json.dump({
        "metadata": {
            "generated_at": datetime.now().isoformat(),
            "total_gateway_records": len(gateway_rows),
            "total_bank_records": len(bank_rows),
            "total_ledger_records": len(ledger_rows),
            "category_breakdown": {
                "exact_matches": 32,
                "amount_date_window": 14,
                "fuzzy_narration_fees": 8,
                "ai_ambiguous_cases": 6,
                "true_anomalies": 5
            }
        },
        "records": ground_truth
    }, f, indent=2)

print(f"Generated hidden_ground_truth.json: {len(ground_truth)} records mapped.")
print(f"All sample datasets saved to {OUTPUT_DIR}")
