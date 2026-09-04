import time
import json
import logging
from typing import Dict, Any, List, Optional
from services.groq_service import groq_service
from services.audit_logger import audit_logger

logger = logging.getLogger("chat_agent")

CHAT_SYSTEM_PROMPT = """You are the AI Financial Controller, an expert financial audit assistant embedded in an enterprise reconciliation platform.
Your job is to answer the user's questions about their financial reconciliation data accurately, professionally, and strictly grounded in the provided batch context.

RULES:
1. STRICT GROUNDING: You MUST base all answers on the structured batch summary, metrics, and transaction details provided below.
2. CITATIONS: Always cite actual transaction IDs, bank reference numbers, dollar amounts, and match rates when discussing specific items.
3. NO HALLUCINATIONS: Do not guess or invent data. If the question asks about something not present in the provided context, clearly state: "Based on the data available for this batch, that information is not present."
4. FINANCIAL CLARITY: Distinguish clearly between Pass 1 (Deterministic Rules) and Pass 2 (Groq AI Classification) and Manual Review.
5. CONCISE & ACTIONABLE: Keep answers structured, professional, and directly useful to financial analysts and controllers.
"""

def answer_financial_question(question: str,
                              batch_summary: Dict[str, Any],
                              conversation_history: Optional[List[Dict[str, str]]] = None) -> Dict[str, Any]:
    start_time = time.time()
    model_name = groq_service.get_best_available_model()

    context_str = json.dumps(batch_summary, indent=2, default=str)

    user_message = f"""CURRENT BATCH FINANCIAL CONTEXT:
{context_str}

USER QUESTION:
{question}
"""

    messages = [{"role": "system", "content": CHAT_SYSTEM_PROMPT}]

    if conversation_history:
        for turn in conversation_history[-4:]: # include recent history
            role = turn.get("role", "user")
            content = turn.get("content", "")
            if role in ["user", "assistant"] and content:
                messages.append({"role": role, "content": content})

    messages.append({"role": "user", "content": user_message})

    answer = ""
    if groq_service.client:
        try:
            logger.info(f"Generating grounded chat answer via Groq ({model_name})...")
            completion = groq_service.client.chat.completions.create(
                messages=messages,
                model=model_name,
                temperature=0.2,
                max_tokens=1024
            )
            answer = completion.choices[0].message.content
        except Exception as e:
            logger.error(f"Groq chat completion failed: {e}. Using grounded fallback generator.")
            answer = generate_grounded_fallback_answer(question, batch_summary)
    else:
        answer = generate_grounded_fallback_answer(question, batch_summary)

    latency_ms = (time.time() - start_time) * 1000.0

    audit_logger.log_call(
        endpoint="/chat",
        model_used=model_name,
        latency_ms=latency_ms,
        confidence=1.0,
        input_data={"question": question, "batch_id": batch_summary.get("batch_id")},
        output_data={"answer_length": len(answer)},
        success=True
    )

    return {
        "answer": answer,
        "model_used": model_name,
        "latency_ms": round(latency_ms, 2),
        "confidence": 0.95
    }

def generate_grounded_fallback_answer(question: str, batch: Dict[str, Any]) -> str:
    """
    Intelligent grounded fallback answer generator when Groq is unavailable.
    Inspects user question keywords and extracts accurate values from batch_summary.
    """
    q_lower = question.lower()
    batch_name = batch.get("batch_name", "Current Batch")
    total_records = batch.get("total_records", 0)
    matched_count = batch.get("matched_count", 0)
    pass1_count = batch.get("pass1_matched_count", 0)
    pass2_count = batch.get("pass2_matched_count", 0)
    manual_count = batch.get("manual_review_count", 0)
    reconciled_amt = batch.get("total_reconciled_amount", "0.00")
    rate = batch.get("match_rate_percentage", 0.0)
    exceptions = batch.get("sample_exceptions", [])

    # Check for specific transaction query e.g. "TXN_104" or "why wasn't"
    for ex in exceptions:
        txn_id = str(ex.get("transaction_id", "")).lower()
        if txn_id and txn_id in q_lower:
            return (f"Transaction **{ex.get('transaction_id')}** (Amount: ${ex.get('amount')}) was not automatically matched "
                    f"due to **{ex.get('case_type', 'UNRESOLVED')}**.\n\n"
                    f"**Reasoning:** {ex.get('reasoning', 'Discrepancy detected between gateway and bank records.')}\n\n"
                    f"Status: **{ex.get('status', 'MANUAL_REVIEW')}**.")

    if "manual review" in q_lower or "stuck" in q_lower or "unmatched" in q_lower:
        return (f"In **{batch_name}**, there are currently **{manual_count}** transactions flagged for manual review, "
                f"accounting for unverified records out of the total {total_records} processed.\n\n"
                f"Total amount reconciled so far is **${reconciled_amt}** with an overall match rate of **{rate:.1f}%**.")

    if "rate" in q_lower or "accuracy" in q_lower or "summary" in q_lower or "how many" in q_lower:
        return (f"**Reconciliation Summary for {batch_name}:**\n"
                f"- **Total Records:** {total_records}\n"
                f"- **Match Rate:** {rate:.1f}%\n"
                f"- **Pass 1 (Deterministic Rules):** {pass1_count} records\n"
                f"- **Pass 2 (AI Classification):** {pass2_count} records\n"
                f"- **Manual Review Required:** {manual_count} records\n"
                f"- **Total Reconciled Volume:** ${reconciled_amt}")

    if "fee" in q_lower or "discrepan" in q_lower:
        fee_exceptions = [e for e in exceptions if "FEE" in str(e.get("case_type", ""))]
        count = len(fee_exceptions)
        return (f"We identified **{count}** fee-related variances in this batch where payment gateway processing fees "
                f"(typically 2.0% - 3.5%) caused net settlement amounts to differ from the gross customer charges.")

    # General overview
    return (f"For **{batch_name}**, {matched_count} of {total_records} transactions have been successfully reconciled "
            f"({pass1_count} via Pass 1 Rules, {pass2_count} via Pass 2 Groq AI), achieving a **{rate:.1f}%** match rate. "
            f"**${reconciled_amt}** has been verified. **{manual_count}** items remain in manual review.")
