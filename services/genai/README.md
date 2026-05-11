# genai

FastAPI + LangChain service that handles LLM calls.

## Run

    uv sync --extra dev
    uv run uvicorn genai.main:app --port 8084 --reload

Service listens on http://localhost:8084

- Health: http://localhost:8084/genai/health
- Swagger UI: http://localhost:8084/docs
- ReDoc: http://localhost:8084/redoc

## Test

    uv run pytest -v

## Lint

    uv run ruff check .
    uv run ruff format --check .

Regenerate the client from the OpenAPI spec before development if the spec changed:

    ../../api/scripts/gen-all.sh
