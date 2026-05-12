# syntax=docker/dockerfile:1
#
# Multi-stage: sync deps in builder → slim runtime with venv only (no uv, compilers).
# Build from services/genai:
#   docker build -f Dockerfile -t tso-genai .

# --- Build: resolve and install deps into .venv (not copied as toolchain to runtime) ---
FROM ghcr.io/astral-sh/uv:python3.12-bookworm-slim AS builder
WORKDIR /app
ENV UV_COMPILE_BYTECODE=1 UV_LINK_MODE=copy

COPY pyproject.toml uv.lock ./
COPY src ./src
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --frozen --no-dev

# --- Runtime: Python + venv only ---
FROM python:3.12-slim-bookworm AS runtime
WORKDIR /app
RUN groupadd --gid 1000 app \
  && useradd --uid 1000 --gid app --shell /usr/sbin/nologin --no-create-home app

COPY --from=builder --chown=1000:1000 /app/.venv /app/.venv
COPY --from=builder --chown=1000:1000 /app/src /app/src

ENV PATH="/app/.venv/bin:$PATH" PYTHONUNBUFFERED=1
USER 1000:1000
EXPOSE 8084
CMD ["uvicorn", "genai.main:app", "--host", "0.0.0.0", "--port", "8084"]
