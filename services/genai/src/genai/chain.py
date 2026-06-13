import json
import re
from typing import Protocol

from langchain_core.messages import AIMessage
from langchain_openai import ChatOpenAI
from pydantic import BaseModel, Field, ValidationError

from genai.config import Settings
from genai.prompts import ASK_PROMPT


class AllowedSummary(BaseModel):
    episode_index: int = Field(alias="episodeIndex")
    season: int
    episode_number: int = Field(alias="episodeNumber")
    title: str
    summary: str

    model_config = {"populate_by_name": True}


class AskInput(BaseModel):
    question: str
    allowed_summaries: list[AllowedSummary] = Field(alias="allowedSummaries")

    model_config = {"populate_by_name": True}


class AskOutput(BaseModel):
    answer: str
    cited_episode_indices: list[int] = Field(alias="citedEpisodeIndices")

    model_config = {"populate_by_name": True}


class LlmInvoker(Protocol):
    async def ainvoke(self, messages: list) -> AIMessage: ...


def format_summaries(summaries: list[AllowedSummary], max_chars: int) -> str:
    lines: list[str] = []
    total = 0
    for summary in summaries:
        label = (
            f"[S{summary.season}E{summary.episode_number}] "
            f"(episode_index={summary.episode_index}) {summary.title}"
        )
        block = f"{label}\n{summary.summary}"
        if total + len(block) > max_chars and lines:
            break
        lines.append(block)
        total += len(block)
    return "\n\n".join(lines)


def _extract_json(text: str) -> dict:
    stripped = text.strip()
    if stripped.startswith("```"):
        stripped = re.sub(r"^```(?:json)?\s*", "", stripped)
        stripped = re.sub(r"\s*```$", "", stripped)
    return json.loads(stripped)


def parse_llm_response(content: str) -> AskOutput:
    data = _extract_json(content)
    return AskOutput.model_validate(data)


def build_llm(settings: Settings) -> ChatOpenAI:
    default_headers: dict[str, str] = {}
    if settings.openrouter_http_referer:
        default_headers["HTTP-Referer"] = settings.openrouter_http_referer
    if settings.openrouter_app_name:
        default_headers["X-Title"] = settings.openrouter_app_name

    return ChatOpenAI(
        model=settings.llm_model,
        api_key=settings.openrouter_api_key,
        base_url="https://openrouter.ai/api/v1",
        temperature=0.2,
        default_headers=default_headers or None,
    )


async def ask_question(
    payload: AskInput,
    settings: Settings,
    llm: LlmInvoker | None = None,
) -> AskOutput:
    if not payload.allowed_summaries:
        return AskOutput(
            answer="I don't have any episode summaries to answer from.",
            cited_episode_indices=[],
        )

    summaries_block = format_summaries(payload.allowed_summaries, settings.max_context_chars)
    messages = ASK_PROMPT.format_messages(
        summaries_block=summaries_block,
        question=payload.question,
    )

    model = llm if llm is not None else build_llm(settings)
    response = await model.ainvoke(messages)
    content = response.content if isinstance(response.content, str) else str(response.content)

    try:
        result = parse_llm_response(content)
    except (json.JSONDecodeError, ValidationError, TypeError) as exc:
        raise ValueError("Failed to parse LLM response") from exc

    allowed_indices = {s.episode_index for s in payload.allowed_summaries}
    filtered = [idx for idx in result.cited_episode_indices if idx in allowed_indices]
    return AskOutput(answer=result.answer, cited_episode_indices=filtered)
