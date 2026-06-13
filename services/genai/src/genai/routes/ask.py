from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from genai.chain import AskInput, AskOutput, ask_question
from genai.config import Settings, load_settings

router = APIRouter(tags=["genai"])


class ErrorBody(BaseModel):
    code: str
    message: str


class GenAiAskRequest(BaseModel):
    question: str = Field(min_length=1, max_length=2000)
    allowed_summaries: list[dict] = Field(alias="allowedSummaries")

    model_config = {"populate_by_name": True}


class GenAiAskResponse(BaseModel):
    answer: str
    cited_episode_indices: list[int] = Field(alias="citedEpisodeIndices")

    model_config = {"populate_by_name": True}


@router.post("/genai/ask", response_model=GenAiAskResponse)
async def genai_ask(body: GenAiAskRequest) -> GenAiAskResponse:
    settings: Settings = load_settings()
    if not settings.openrouter_api_key:
        raise HTTPException(
            status_code=500,
            detail=ErrorBody(
                code="LLM_NOT_CONFIGURED",
                message="OPENROUTER_API_KEY is not set",
            ).model_dump(),
        )

    try:
        payload = AskInput.model_validate(body.model_dump(by_alias=True))
        result: AskOutput = await ask_question(payload, settings)
    except ValueError as exc:
        raise HTTPException(
            status_code=500,
            detail=ErrorBody(code="LLM_PARSE_ERROR", message=str(exc)).model_dump(),
        ) from exc
    except Exception as exc:
        raise HTTPException(
            status_code=500,
            detail=ErrorBody(
                code="LLM_ERROR",
                message="The language model request failed",
            ).model_dump(),
        ) from exc

    return GenAiAskResponse(
        answer=result.answer,
        cited_episode_indices=result.cited_episode_indices,
    )
