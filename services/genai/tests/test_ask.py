import json
from unittest.mock import AsyncMock, patch

import pytest
from fastapi.testclient import TestClient
from langchain_core.messages import AIMessage

from genai.chain import AllowedSummary, AskInput, ask_question, parse_llm_response
from genai.config import LOGOS_DEFAULT_BASE_URL, LOGOS_DEFAULT_MODEL, Settings
from genai.main import app


@pytest.fixture
def settings() -> Settings:
    return Settings(
        logos_api_key="test-key",
        llm_model=LOGOS_DEFAULT_MODEL,
        llm_base_url=LOGOS_DEFAULT_BASE_URL,
        max_context_chars=10_000,
    )


def test_parse_llm_response_handles_fenced_json():
    content = '```json\n{"answer": "Hi", "citedEpisodeIndices": [1]}\n```'
    result = parse_llm_response(content)
    assert result.answer == "Hi"
    assert result.cited_episode_indices == [1]


@pytest.mark.asyncio
async def test_ask_question_filters_citations_to_allowed_indices(settings: Settings):
    mock_llm = AsyncMock()
    mock_llm.ainvoke.return_value = AIMessage(
        content=json.dumps(
            {
                "answer": "Eleven is a girl with powers.",
                "citedEpisodeIndices": [1, 99],
            }
        )
    )

    payload = AskInput(
        question="Who is Eleven?",
        allowedSummaries=[
            AllowedSummary(
                episodeIndex=1,
                season=1,
                episodeNumber=1,
                title="Pilot",
                summary="Eleven appears.",
            )
        ],
    )

    result = await ask_question(payload, settings, llm=mock_llm)

    assert result.answer == "Eleven is a girl with powers."
    assert result.cited_episode_indices == [1]


@pytest.mark.asyncio
async def test_ask_question_without_summaries_returns_safe_message(settings: Settings):
    payload = AskInput(question="Who is Eleven?", allowedSummaries=[])

    result = await ask_question(payload, settings)

    assert "don't have any episode summaries" in result.answer
    assert result.cited_episode_indices == []


def test_genai_ask_endpoint_with_mocked_llm(monkeypatch):
    monkeypatch.setenv("LOGOS_API_KEY", "test-key")

    with patch("genai.routes.ask.ask_question", new_callable=AsyncMock) as mock_ask:
        from genai.chain import AskOutput

        mock_ask.return_value = AskOutput(
            answer="Will vanished in episode 1.",
            cited_episode_indices=[1],
        )

        client = TestClient(app)
        response = client.post(
            "/genai/ask",
            json={
                "question": "What happened to Will?",
                "allowedSummaries": [
                    {
                        "episodeIndex": 1,
                        "season": 1,
                        "episodeNumber": 1,
                        "title": "Pilot",
                        "summary": "Will vanishes.",
                    }
                ],
            },
        )

    assert response.status_code == 200
    body = response.json()
    assert body["answer"] == "Will vanished in episode 1."
    assert body["citedEpisodeIndices"] == [1]


def test_genai_ask_returns_500_when_api_key_missing(monkeypatch):
    monkeypatch.delenv("LOGOS_API_KEY", raising=False)

    client = TestClient(app)
    response = client.post(
        "/genai/ask",
        json={
            "question": "Hi?",
            "allowedSummaries": [
                {
                    "episodeIndex": 1,
                    "season": 1,
                    "episodeNumber": 1,
                    "title": "Pilot",
                    "summary": "Plot.",
                }
            ],
        },
    )

    assert response.status_code == 500
