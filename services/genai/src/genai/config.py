import os
from dataclasses import dataclass

LOGOS_DEFAULT_BASE_URL = "https://logos.aet.cit.tum.de/v1"
LOGOS_DEFAULT_MODEL = "openai/gpt-oss-120b"


@dataclass(frozen=True)
class Settings:
    logos_api_key: str
    llm_model: str
    llm_base_url: str
    max_context_chars: int


def load_settings() -> Settings:
    return Settings(
        logos_api_key=os.environ.get("LOGOS_API_KEY", ""),
        llm_model=os.environ.get("LLM_MODEL", LOGOS_DEFAULT_MODEL),
        llm_base_url=os.environ.get("LLM_BASE_URL", LOGOS_DEFAULT_BASE_URL),
        max_context_chars=int(os.environ.get("MAX_CONTEXT_CHARS", "12000")),
    )
