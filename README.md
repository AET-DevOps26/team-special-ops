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
| `web-client/` | React + Vite + TS + Tailwind (port 5173) |
| `infra/` | docker-compose, Helm charts (added in later tasks) |
| `docs/` | Architecture, diagrams, runbooks, specs, plans |
| `.github/workflows/ci.yml` | Build + test + lint per service |

## First-time setup

> **Heads up:** docker-compose is coming in a follow-up task. Once it lands,
> setup will be `docker compose up` and most of this section goes away.
> Until then, install the toolchain below.

### 1. Install the toolchain (macOS, one-time)

```bash
brew install openjdk@21 uv node pnpm pre-commit
```

Make sure `java -version` reports 21 and `JAVA_HOME` points to your JDK 21 install (Maven needs this). On Homebrew the JDK lives at `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home` — wire it into your shell however you normally do.

Sanity check:

```bash
java -version    # openjdk 21.x
node --version   # v20+
pnpm --version   # 9+
uv --version
```

For non-macOS, install equivalents:
- Java 21: any LTS JDK (Temurin, OpenJDK, Zulu)
- Node 20+ and pnpm via corepack: `corepack enable`
- uv: `curl -LsSf https://astral.sh/uv/install.sh | sh`
- pre-commit: `pipx install pre-commit`

### 2. Clone and bootstrap

```bash
git clone <repo-url> team-special-ops
cd team-special-ops

# Enable the local OpenAPI lint hook
pre-commit install

# Generate clients/stubs from the OpenAPI spec
./api/scripts/gen-all.sh
```

That's it. After this, you can run any service in dev (see Quickstart below).

## Quickstart — run a service in dev

> Run each command in its own terminal. Until docker-compose lands, you start
> services individually.

```bash
# A Spring Boot service (example: catalog) — run from services/ parent
cd services && ./mvnw -pl catalog spring-boot:run

# All three Spring services at once: cd services && ./mvnw verify

# The GenAI service
cd services/genai && uv sync --extra dev && uv run uvicorn genai.main:app --port 8084 --reload

# The web client
cd web-client && pnpm install && pnpm dev
```

After any change to `api/openapi.yaml`, regenerate clients:

```bash
./api/scripts/gen-all.sh
```

## Swagger UI

| Service | URL |
|---|---|
| user-progress | http://localhost:8081/swagger-ui.html |
| catalog | http://localhost:8082/swagger-ui.html |
| chat | http://localhost:8083/swagger-ui.html |
| genai | http://localhost:8084/docs |

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
