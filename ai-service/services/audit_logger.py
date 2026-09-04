import time
from typing import Dict, Any, List
from collections import deque
from datetime import datetime

class AiAuditLogger:
    def __init__(self, max_entries: int = 500):
        self.entries: deque = deque(maxlen=max_entries)

    def log_call(self, endpoint: str, model_used: str, latency_ms: float,
                 confidence: float, input_data: Any, output_data: Any,
                 success: bool = True, error: str = None) -> Dict[str, Any]:
        entry = {
            "id": len(self.entries) + 1,
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "endpoint": endpoint,
            "model_used": model_used,
            "latency_ms": round(latency_ms, 2),
            "confidence": round(confidence, 2) if confidence is not None else None,
            "input_preview": str(input_data)[:300],
            "output_preview": str(output_data)[:300],
            "success": success,
            "error": error
        }
        self.entries.appendleft(entry)
        return entry

    def get_recent_logs(self, limit: int = 50) -> List[Dict[str, Any]]:
        return list(self.entries)[:limit]

audit_logger = AiAuditLogger()
