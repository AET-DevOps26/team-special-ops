# genai

FastAPI + LangChain service that answers spoiler-safe questions via TUM Logos.

## Setup

Copy `infra/env.example` to `infra/.env` and set `LOGOS_API_KEY` (team key from your
tutor). Off campus you need [eduVPN](https://www.eduvpn.org/). Never commit real keys.

```bash
export LOGOS_API_KEY=lg-...
export LLM_MODEL=openai/gpt-oss-120b
# export LLM_BASE_URL=https://logos.aet.cit.tum.de:8080/v1   # default
```

## Run

```bash
uv sync --extra dev
uv run uvicorn genai.main:app --port 8084 --reload
```

Service listens on http://localhost:8084

- Health: http://localhost:8084/genai/health
- Ask (internal): `POST /genai/ask` — called by the chat service, not the browser
- Swagger UI: http://localhost:8084/docs
- Metrics: http://localhost:8084/metrics

## Test

```bash
uv run pytest -v
```

Tests mock the LLM — no live API key required in CI.

## Lint

```bash
uv run ruff check .
uv run ruff format --check .
```

Regenerate the client from the OpenAPI spec before development if the spec changed:

```bash
../../api/scripts/gen-all.sh
```

## Manual test (with a real key)

```bash
curl -s http://localhost:8084/genai/ask \
  -H 'Content-Type: application/json' \
  -d '{
    "question": "Who is Eleven?",
    "allowedSummaries": [{
      "episodeIndex": 1,
      "season": 1,
      "episodeNumber": 1,
      "title": "Chapter One",
      "summary": "Eleven appears in Hawkins."
    }]
  }' | jq
```
