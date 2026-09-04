import os
import uvicorn
from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Dict, Any, Optional

from services.groq_service import groq_service
from services.matcher import match_ambiguous_records, CONFIDENCE_THRESHOLD
from services.chat_agent import answer_financial_question
from services.audit_logger import audit_logger

app = FastAPI(
    title="AI Finance Controller - AI Microservice",
    description="Groq-powered ambiguous matching and grounded financial Q&A",
    version="1.0.0"
)

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Request & Response Models
class AmbiguousMatchRequest(BaseModel):
    ambiguous_records: List[Dict[str, Any]] = Field(..., description="Leftover NEEDS_REVIEW records from Pass 1")
    candidate_bank_records: List[Dict[str, Any]] = Field(default=[], description="Unmatched bank statement records")
    candidate_ledger_records: List[Dict[str, Any]] = Field(default=[], description="Unmatched ledger records")

class ChatRequest(BaseModel):
    question: str = Field(..., description="User's natural language inquiry")
    batch_summary: Dict[str, Any] = Field(..., description="Structured summary and key records for context grounding")
    conversation_history: Optional[List[Dict[str, str]]] = Field(default=[], description="Previous conversation turns")

@app.get("/health")
def health_check():
    active_model = groq_service.get_best_available_model()
    return {
        "status": "UP",
        "service": "ai-finance-controller-ai-service",
        "active_model": active_model,
        "groq_configured": groq_service.client is not None,
        "confidence_threshold": CONFIDENCE_THRESHOLD
    }

@app.get("/models")
def get_available_models():
    models = groq_service.list_available_models()
    active_model = groq_service.get_best_available_model()
    return {
        "active_model": active_model,
        "available_models": models
    }

@app.post("/match-ambiguous")
def match_ambiguous(request: AmbiguousMatchRequest):
    if not request.ambiguous_records:
        return {
            "model_used": groq_service.get_best_available_model(),
            "latency_ms": 0.0,
            "confidence_threshold": CONFIDENCE_THRESHOLD,
            "total_evaluated": 0,
            "matches": []
        }

    try:
        result = match_ambiguous_records(
            request.ambiguous_records,
            request.candidate_bank_records,
            request.candidate_ledger_records
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"AI matching error: {str(e)}")

@app.post("/chat")
def chat_with_agent(request: ChatRequest):
    if not request.question or not request.question.strip():
        raise HTTPException(status_code=400, detail="Question cannot be empty")

    try:
        response = answer_financial_question(
            request.question,
            request.batch_summary,
            request.conversation_history
        )
        return response
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"AI Chat error: {str(e)}")

@app.get("/audit-logs")
def get_audit_logs(limit: int = 50):
    return audit_logger.get_recent_logs(limit=limit)

if __name__ == "__main__":
    port = int(os.getenv("PORT", "8000"))
    host = os.getenv("HOST", "0.0.0.0")
    uvicorn.run("main:app", host=host, port=port, reload=True)
