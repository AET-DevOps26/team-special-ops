import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    openrouter_api_key: str
    llm_model: str
    openrouter_http_referer: str | None
    openrouter_app_name: str | None
    max_context_chars: int


def load_settings() -> Settings:
    api_key = os.environ.get("OPENROUTER_API_KEY", "")
    return Settings(
        openrouter_api_key=api_key,
        llm_model=os.environ.get("LLM_MODEL", "nex-agi/nex-n2-pro:free"),
        openrouter_http_referer=os.environ.get("OPENROUTER_HTTP_REFERER"),
        openrouter_app_name=os.environ.get("OPENROUTER_APP_NAME"),
        max_context_chars=int(os.environ.get("MAX_CONTEXT_CHARS", "12000")),
    )
