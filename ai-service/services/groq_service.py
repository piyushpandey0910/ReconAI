import os
import time
import logging
from typing import Optional, List
from groq import Groq
from dotenv import load_dotenv

load_dotenv()

logger = logging.getLogger("groq_service")
logging.basicConfig(level=logging.INFO)

class GroqService:
    def __init__(self):
        self.api_key = os.getenv("GROQ_API_KEY")
        self.client: Optional[Groq] = None
        if self.api_key and self.api_key != "your_groq_api_key_here":
            try:
                self.client = Groq(api_key=self.api_key)
                logger.info("Groq client initialized with API key.")
            except Exception as e:
                logger.warning(f"Failed to initialize Groq client: {e}")
        else:
            logger.warning("GROQ_API_KEY not provided. Service will run in fallback simulation mode.")

        self._cached_model: Optional[str] = None
        self._last_model_check: float = 0
        self._cache_ttl_seconds: float = 600  # 10 minutes cache

        # Preferred models in priority order
        self.preferred_models = [
            "llama-3.3-70b-versatile",
            "llama-3.1-70b-versatile",
            "llama-3.1-8b-instant",
            "llama3-70b-8192",
            "llama3-8b-8192",
            "mixtral-8x7b-32768",
            "gemma2-9b-it"
        ]

    def get_best_available_model(self) -> str:
        """
        Dynamically query client.models.list() to avoid hardcoding deprecated model names.
        Selects the highest priority available model.
        """
        now = time.time()
        if self._cached_model and (now - self._last_model_check < self._cache_ttl_seconds):
            return self._cached_model

        if not self.client:
            return "llama-3.3-70b-versatile (simulation)"

        try:
            logger.info("Querying Groq API for available active models via client.models.list()...")
            models_response = self.client.models.list()
            available_ids = [m.id for m in models_response.data if getattr(m, 'active', True)]
            logger.info(f"Retrieved {len(available_ids)} active Groq models: {available_ids[:5]}...")

            # Match against preferred models
            for preferred in self.preferred_models:
                if preferred in available_ids:
                    logger.info(f"Selected preferred model: {preferred}")
                    self._cached_model = preferred
                    self._last_model_check = now
                    return preferred

            # Fallback to any model containing 'llama'
            for m_id in available_ids:
                if "llama" in m_id.lower() and "guard" not in m_id.lower():
                    logger.info(f"Selected alternative llama model: {m_id}")
                    self._cached_model = m_id
                    self._last_model_check = now
                    return m_id

            # Fallback to the first available model
            if available_ids:
                selected = available_ids[0]
                self._cached_model = selected
                self._last_model_check = now
                return selected

        except Exception as e:
            logger.error(f"Error checking models via client.models.list(): {e}")
            if self._cached_model:
                return self._cached_model

        # Safe default
        return "llama-3.3-70b-versatile"

    def list_available_models(self) -> List[str]:
        if not self.client:
            return self.preferred_models
        try:
            models_response = self.client.models.list()
            return [m.id for m in models_response.data]
        except Exception as e:
            logger.error(f"Failed to list models: {e}")
            return self.preferred_models

groq_service = GroqService()
