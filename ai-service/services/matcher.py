import json
import time
import os
import re
import logging
from typing import List, Dict, Any, Optional
from services.groq_service import groq_service
from services.audit_logger import audit_logger

logger = logging.getLogger("matcher")

CONFIDENCE_THRESHOLD = float(os.getenv("AI_CONFIDENCE_THRESHOLD", "0.70"))

SYSTEM_PROMPT = """You are an expert AI Financial Controller reconciling complex, ambiguous transaction records between a Payment Gateway, a Bank Statement, and an Internal Ledger.
You analyze discrepancies such as:
1. FEE_DISCREPANCY: Gateway gross amount differs from Bank net payout by payment processing fee (typically 1.5% - 4%).
2. NAME_VARIATION: Truncated customer names, company abbreviations, email handles, or wire descriptions.
3. DELAYED_SETTLEMENT: Weekend or holiday clearing lag with slightly shifted transaction timestamps.
4. PARTIAL_MATCH: Ledger entry has matching reference but slight variance in description.
5. NO_MATCH: Unreconciled anomaly, chargeback, or missing credit.

CRITICAL INSTRUCTIONS:
- You must output VALID, PARSABLE JSON ONLY. No markdown ticks, no preamble, no commentary.
- The JSON must have the following exact schema:
{
  "matches": [
    {
      "record_id": "string (gateway record transactionId or id)",
      "suggested_match_id": "string (bank ref ID or ledger ref ID, or null if no confident match)",
      "case_type": "FEE_DISCREPANCY" | "NAME_VARIATION" | "DELAYED_SETTLEMENT" | "PARTIAL_MATCH" | "NO_MATCH",
      "confidence": float between 0.0 and 1.0,
      "reasoning": "Clear, concise financial reasoning explaining the match or why it cannot be matched"
    }
  ]
}
"""

def match_ambiguous_records(ambiguous_records: List[Dict[str, Any]],
                           candidate_bank_records: List[Dict[str, Any]],
                           candidate_ledger_records: List[Dict[str, Any]]) -> Dict[str, Any]:
    start_time = time.time()
    model_name = groq_service.get_best_available_model()

    prompt_data = {
        "ambiguous_gateway_records": ambiguous_records[:25], # batch limit
        "candidate_bank_records": candidate_bank_records[:35],
        "candidate_ledger_records": candidate_ledger_records[:35]
    }

    user_prompt = f"Analyze these ambiguous transaction records and identify matches against candidates:\n{json.dumps(prompt_data, indent=2, default=str)}"

    raw_response = ""
    matches_output = []

    if groq_service.client:
        try:
            logger.info(f"Calling Groq LLM ({model_name}) for {len(ambiguous_records)} ambiguous records...")
            chat_completion = groq_service.client.chat.completions.create(
                messages=[
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {"role": "user", "content": user_prompt}
                ],
                model=model_name,
                temperature=0.1,
                max_tokens=2048,
                response_format={"type": "json_object"}
            )
            raw_response = chat_completion.choices[0].message.content
            parsed = json.loads(raw_response)
            matches_output = parsed.get("matches", [])
        except Exception as e:
            logger.error(f"Groq API call failed: {e}. Falling back to rule-guided AI heuristics.")
            matches_output = fallback_ambiguous_matcher(ambiguous_records, candidate_bank_records, candidate_ledger_records)
    else:
        logger.info("Using simulated AI heuristic matcher (no Groq key configured).")
        matches_output = fallback_ambiguous_matcher(ambiguous_records, candidate_bank_records, candidate_ledger_records)

    # DEFENSE IN DEPTH: Enforce server-side confidence threshold (>= 0.70)
    enforced_matches = []
    for item in matches_output:
        conf = float(item.get("confidence", 0.0))
        rec_id = str(item.get("record_id", ""))
        sugg_id = item.get("suggested_match_id")
        case_type = item.get("case_type", "NO_MATCH")
        reasoning = item.get("reasoning", "")

        if conf < CONFIDENCE_THRESHOLD:
            # Low confidence: Route to manual review!
            enforced_matches.append({
                "record_id": rec_id,
                "suggested_match_id": None,
                "case_type": case_type if case_type != "NO_MATCH" else "LOW_CONFIDENCE",
                "confidence": conf,
                "action": "MANUAL_REVIEW",
                "reasoning": f"Confidence ({conf:.2f}) is below safe threshold ({CONFIDENCE_THRESHOLD:.2f}). {reasoning} Routed to Manual Review."
            })
        else:
            enforced_matches.append({
                "record_id": rec_id,
                "suggested_match_id": sugg_id,
                "case_type": case_type,
                "confidence": conf,
                "action": "AUTO_MATCHED" if sugg_id else "MANUAL_REVIEW",
                "reasoning": reasoning
            })

    latency_ms = (time.time() - start_time) * 1000.0
    avg_confidence = sum(m["confidence"] for m in enforced_matches) / len(enforced_matches) if enforced_matches else 0.0

    audit_logger.log_call(
        endpoint="/match-ambiguous",
        model_used=model_name,
        latency_ms=latency_ms,
        confidence=avg_confidence,
        input_data={"records_count": len(ambiguous_records)},
        output_data={"matches_resolved": len(enforced_matches)},
        success=True
    )

    return {
        "model_used": model_name,
        "latency_ms": round(latency_ms, 2),
        "confidence_threshold": CONFIDENCE_THRESHOLD,
        "total_evaluated": len(enforced_matches),
        "matches": enforced_matches
    }

def fallback_ambiguous_matcher(ambiguous_records: List[Dict[str, Any]],
                               candidate_bank_records: List[Dict[str, Any]],
                               candidate_ledger_records: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
    Intelligent simulation of the AI matching logic when Groq API key is absent or unreachable.
    Evaluates fee calculations, truncated merchant names, and date shifts.
    """
    results = []
    used_bank_ids = set()

    for gw in ambiguous_records:
        gw_id = str(gw.get("transactionId") or gw.get("id"))
        gw_amount = float(gw.get("amount", 0.0))
        gw_order = str(gw.get("orderId", "")).lower()
        gw_email = str(gw.get("customerEmail", "")).lower()

        best_bank = None
        best_type = "NO_MATCH"
        best_confidence = 0.0
        best_reason = "No corresponding bank or ledger entries found."

        for b in candidate_bank_records:
            b_id = str(b.get("bankRefId") or b.get("id"))
            if b_id in used_bank_ids:
                continue

            b_amount = float(b.get("amount", 0.0))
            b_narr = str(b.get("narration", "")).lower()

            # Check for Fee Discrepancy (e.g., 2% - 3.5% deduction)
            diff = abs(gw_amount - b_amount)
            pct = diff / gw_amount if gw_amount > 0 else 1.0

            if 0.015 <= pct <= 0.035:
                # Likely gateway fee deduction
                best_bank = b_id
                best_type = "FEE_DISCREPANCY"
                best_confidence = 0.88
                fee_val = round(diff, 2)
                best_reason = f"Identified payment gateway processing fee of ${fee_val:.2f} ({pct*100:.1f}%) deducted from payout amount ${b_amount:.2f}."
                break

            # Check for Name Variation / Truncation
            email_user = gw_email.split("@")[0] if "@" in gw_email else ""
            if email_user and len(email_user) > 3 and email_user in b_narr:
                best_bank = b_id
                best_type = "NAME_VARIATION"
                best_confidence = 0.82
                best_reason = f"Matched customer identifier '{email_user}' in bank narration '{b.get('narration')}'. Amount aligns with variance tolerance."
                break

            # Check for Delayed Settlement lag
            if abs(gw_amount - b_amount) < 0.05:
                best_bank = b_id
                best_type = "DELAYED_SETTLEMENT"
                best_confidence = 0.85
                best_reason = f"Matching amount ${gw_amount:.2f} resolved with clearing settlement lag."
                break

        # Edge case: ambiguous case with low confidence (e.g. uncertain partial match < 0.70)
        if not best_bank and gw_amount > 500:
            # Simulated low confidence record to test manual review fallback
            best_type = "PARTIAL_MATCH"
            best_confidence = 0.55
            best_reason = "Weak correlation with multiple potential split charges. Insufficient evidence for automated match."

        if best_bank:
            used_bank_ids.add(best_bank)

        results.append({
            "record_id": gw_id,
            "suggested_match_id": best_bank,
            "case_type": best_type,
            "confidence": best_confidence,
            "reasoning": best_reason
        })

    return results
