# team-special-ops

Spoiler-safe TV show Q&A — a web app that answers questions about a show using
only episodes the viewer has already seen. Built as a DevOps course project:
mono-repo, OpenAPI-first, CI/CD, observability, Kubernetes deployment.

## Structure

| Path | What |
|---|---|
| `api/openapi.yaml` | Single source of truth — OpenAPI 3.1 spec |
| `api/scripts/gen-all.sh` | Regenerates Java stubs, Python client, TS types |
| `services/user-progress/` | Spring Boot — auth + watch progress (port 8081) |
| `services/catalog/` | Spring Boot — shows + episodes (port 8082) |
| `services/chat/` | Spring Boot — Q&A orchestration (port 8083) |
| `services/genai/` | FastAPI + LangChain — LLM calls (port 8084) |
| `web-client/` | React + Vite + TS + Tailwind (dev port 5173; Docker serves on 8080) |
| `infra/docker-compose.yml` | Local stack: Postgres + all services + web (multi-stage images) |
| `docs/` | Architecture, diagrams, runbooks, specs, plans |
| `.github/workflows/ci.yml` | Build + test + lint per service |

## Quickstart — full stack with Docker (≤3 commands)

Requires [Docker](https://docs.docker.com/get-docker/) with Compose v2.

```bash
git clone <repo-url> team-special-ops
cd team-special-ops
docker compose -f infra/docker-compose.yml up --build
```

Optional: customize Postgres (defaults are dev-only):

```bash
cp infra/env.example infra/.env   # edit values; infra/.env is gitignored
docker compose --env-file infra/.env -f infra/docker-compose.yml up --build
```

When containers are healthy, open the URLs below. First startup builds images and can take several minutes.

| What | URL |
|---|---|
| Web UI (nginx + static build) | http://localhost:8080 |
| user-progress | http://localhost:8081/swagger-ui.html |
| catalog | http://localhost:8082/swagger-ui.html |
| chat | http://localhost:8083/swagger-ui.html |
| genai | http://localhost:8084/docs |
| Postgres | `localhost:5432` (user / password / db default: `tso`) |

Stop the stack: `docker compose -f infra/docker-compose.yml down` (add `-v` to drop the database volume).

## Local development without Docker

Install the toolchain if you prefer running services on the host (see also [docs/system-architecture.md](./docs/system-architecture.md)).

### Toolchain (macOS example)

```bash
brew install openjdk@21 uv node pnpm pre-commit
```

Use Java 21 for Maven. On non-macOS, install JDK 21, Node 20+, pnpm, and uv using your usual package manager.

### Bootstrap (OpenAPI hooks + codegen)

```bash
cd team-special-ops
pre-commit install
./api/scripts/gen-all.sh
```

### Run individual services

```bash
# Spring (from services/)
cd services && ./mvnw -pl catalog spring-boot:run

# All three Spring modules build + test
cd services && ./mvnw verify

# GenAI
cd services/genai && uv sync --extra dev && uv run uvicorn genai.main:app --port 8084 --reload

# Web client (Vite dev server on 5173)
cd web-client && pnpm install && pnpm dev
```

After any change to `api/openapi.yaml`, regenerate clients:

```bash
./api/scripts/gen-all.sh
```

## Architecture

See [docs/system-architecture.md](./docs/system-architecture.md) and
[docs/diagrams/](./docs/diagrams/).

## Workflow

- Every change goes through a feature branch and a pull request
- `main` is branch-protected (see [docs/branch-protection.md](./docs/branch-protection.md))
- CI must be green and ≥1 teammate must approve before merge
- API changes start in `api/openapi.yaml`, then run `./api/scripts/gen-all.sh`

## Specs and plans

Design docs live in [docs/superpowers/specs/](./docs/superpowers/specs/).
Implementation plans live in [docs/superpowers/plans/](./docs/superpowers/plans/).
