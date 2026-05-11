# Task 1: Repo + CI Foundation — Design

**Date:** 2026-05-11
**Product backlog item:** #1 "Repo + CI foundation"
**Status:** Approved (pending implementation plan)

## Goal

Establish a mono-repo with the layout, CI pipeline, OpenAPI-first code generation, and per-service skeletons required for every subsequent backlog item to build on. After this task, contributors can clone, regenerate clients, run any service in dev, and open a PR that runs build/test/lint per service on every change.

## Scope

### In scope
- Mono-repo directory layout
- OpenAPI 3.1 spec with `/health` paths per service + unified `Error` schema
- Code generation pipeline (Java stubs, Python client, TypeScript types)
- Real runnable skeletons for all 4 services and the web client (each with a passing `/health` endpoint, one test, lint config)
- GitHub Actions CI: spec lint + per-service build/test/lint (parallel matrix)
- PR template and branch protection runbook
- Root hygiene files (.gitignore, .editorconfig, README)

### Out of scope (deferred to later backlog items)
- Dockerfiles and `docker-compose.yml` → Task 2
- Postgres schema and seed data → Task 4
- Auth / JWT → Task 3
- Real domain endpoints → Tasks 3–7
- Prometheus / Grafana → Task 9
- Helm + Kubernetes deployment + CD → Task 10
- Pact contract tests → after the first cross-service call exists (Task 6 + 7)
- CONTRIBUTING.md, TEAM file → not required by course or product backlog

## Technical decisions

| Decision | Choice | Rationale |
|---|---|---|
| Java version | 21 (LTS) | Best LLM training coverage right now; team flagged AI assistance as a priority. |
| Spring Boot | 3.x | Pairs with Java 21; mature ecosystem. |
| Folder layout | Tips-style (`api/`, `services/`, `web-client/`) | Single source-of-truth `api/`; uniform `services/` matrix; aligns with team's stated tips. |
| Build (Java) | Maven wrapper | Course-standard, matches the tips' `./mvnw verify`. |
| Python deps | `uv` + `pyproject.toml` | Fast, modern, lock-file native. |
| Python lint | `ruff` (check + format) | Single tool replaces black/isort/flake8. |
| JS package manager | `pnpm` | Team preference; faster + disk-efficient vs npm. |
| Frontend stack | React + Vite + TypeScript + Tailwind | Already in `system-architecture.md`; allowed by course; best LLM support among allowed frameworks. |
| OpenAPI spec scope (v0) | One `openapi.yaml` covering all 4 services | "Single source of truth" per tips; each service filtered by tag at codegen time. |
| Generated code commit policy | gitignored, regenerated in CI | Cleaner diffs, no merge churn, guaranteed up-to-date. |
| Swagger UI | `springdoc-openapi-starter-webmvc-ui` (Spring) + FastAPI built-in `/docs` | Course requirement; zero-code integration. |
| Pre-commit hooks | Redocly OpenAPI lint only (Task 1) | Mirrors CI's first gate; more hooks added when configs land. |

## Section 1 — Top-level layout

```
team-special-ops/
├── api/
│   ├── openapi.yaml
│   └── scripts/
│       ├── gen-all.sh
│       └── py-config.json
├── services/
│   ├── user-progress/    # Spring Boot 3.x, port 8081
│   ├── catalog/          # Spring Boot 3.x, port 8082
│   ├── chat/             # Spring Boot 3.x, port 8083
│   └── genai/            # FastAPI + LangChain, port 8084
├── web-client/           # React + Vite + TS + Tailwind + pnpm
├── infra/                # placeholder; docker-compose lands in Task 2
├── docs/
│   ├── problem-statement.md          # moved from root
│   ├── system-architecture.md        # moved from root
│   ├── diagrams/                     # moved from root
│   ├── branch-protection.md
│   └── superpowers/specs/            # design docs
├── .github/
│   ├── workflows/
│   │   └── ci.yml
│   └── pull_request_template.md
├── .pre-commit-config.yaml
├── .editorconfig
├── .gitignore
└── README.md
```

Existing `problem-statement.md`, `system-architecture.md`, and `diagrams/` are moved into `docs/` to keep the root clean.

## Section 2 — CI workflow

**File:** `.github/workflows/ci.yml`

**Triggers:** every PR, and push to `main`.

**Jobs:**

1. `spec-lint` — runs first, blocks everything downstream.
   - Checkout
   - `pnpm dlx @redocly/cli lint api/openapi.yaml`

2. `java-services` — matrix over `[user-progress, catalog, chat]`.
   - Checkout
   - Setup JDK 21 + cache `~/.m2/repository`
   - Run `./api/scripts/gen-all.sh` (regenerates Java stubs into `services/<svc>/generated/`)
   - `cd services/<svc> && ./mvnw -B verify` (compile, test, Spotless check)

3. `genai` — single job.
   - Checkout
   - Setup Python 3.12 + install `uv` + cache pip wheels
   - Run `./api/scripts/gen-all.sh`
   - `cd services/genai && uv sync && uv run ruff check . && uv run pytest`

4. `web-client` — single job.
   - Checkout
   - Setup Node 20 + setup pnpm + cache pnpm store
   - Run `./api/scripts/gen-all.sh`
   - `cd web-client && pnpm install --frozen-lockfile && pnpm lint && pnpm test && pnpm build`

**Dependencies:** jobs 2–4 each depend on `spec-lint` succeeding.

**Branch protection** (manually configured per `docs/branch-protection.md`): all jobs above must pass; 1 approving review required; up-to-date branch required before merge.

## Section 3 — OpenAPI spec + codegen

### `api/openapi.yaml` (v0)

OpenAPI 3.1 document with:
- `info`: title, version 0.1.0, description noting v0 contains only health endpoints
- `servers`: localhost entries for each service's port
- `paths`: `/user-progress/health`, `/catalog/health`, `/chat/health`, `/genai/health` — each tagged with the service name, returning `HealthStatus`
- `components/schemas`:
  - `HealthStatus` — `{ status: "ok", service: <name> }`
  - `Error` — `{ code, message, details? }` (unified per team tips; ready for downstream tasks)
- `components/responses/Health` — references `HealthStatus`

### `api/scripts/gen-all.sh`

Bash script, idempotent, invokable from anywhere (cd's to repo root):

1. Lint spec via Redocly (matches pre-commit + CI).
2. For each Java service in `[user-progress, catalog, chat]`, with Java package = hyphens stripped (`user-progress` → `userprogress`):
   `pnpm dlx @openapitools/openapi-generator-cli generate -i api/openapi.yaml -g spring -o services/<svc>/generated --additional-properties=interfaceOnly=true,useTags=true,apiPackage=com.tso.<pkg>.api,modelPackage=com.tso.<pkg>.model`

   Java package mapping: `user-progress` → `com.tso.userprogress`, `catalog` → `com.tso.catalog`, `chat` → `com.tso.chat`.
3. Python client into `services/genai/generated/` via `openapi-python-client --overwrite`.
4. TypeScript types into `web-client/src/api/types.ts` via `openapi-typescript`.

Key codegen flags:
- `interfaceOnly=true` — generator produces interfaces only; controllers are handwritten and `implement` them.
- `useTags=true` — each service receives only the endpoints tagged with its name.

### `.pre-commit-config.yaml`

Single hook: Redocly CLI lint against `api/openapi.yaml`. More hooks added later when stack-specific configs (ruff, prettier, eslint) are wired up.

## Section 4 — Per-service skeletons

### Spring Boot services (`user-progress`, `catalog`, `chat`)

Structure (identical across the three):

```
services/<dir>/
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/wrapper/
├── src/main/java/com/tso/<pkg>/
│   ├── Application.java
│   └── health/HealthController.java
├── src/main/resources/application.yml
├── src/test/java/com/tso/<pkg>/health/HealthControllerTest.java
├── generated/          # gitignored
└── README.md
```

Where `<dir>` is the hyphenated directory name and `<pkg>` is the hyphen-stripped Java package:
| `<dir>` | `<pkg>` (Java package suffix) |
|---|---|
| `user-progress` | `userprogress` |
| `catalog` | `catalog` |
| `chat` | `chat` |

**`pom.xml` dependencies:**
- `spring-boot-starter-web`
- `spring-boot-starter-actuator`
- `springdoc-openapi-starter-webmvc-ui`
- `spring-boot-starter-test` (test scope)

**Plugins:** Spring Boot Maven plugin; Spotless (Google Java Format) with `mvn spotless:check` wired into the default `verify` phase.

**`HealthController`** implements the codegen-produced API interface for its service's health tag, returning `new HealthStatus("ok", "<service-name>")`.

**`HealthControllerTest`** — `@WebMvcTest(HealthController.class)`, uses MockMvc to assert `GET /<service>/health` → 200 with the expected JSON body. Slice test, no full context, fast.

**Ports:** `user-progress=8081`, `catalog=8082`, `chat=8083`. Configured in each `application.yml`.

### GenAI service (`services/genai/`)

```
genai/
├── pyproject.toml          # uv-managed; ruff config inline
├── uv.lock
├── src/genai/
│   ├── __init__.py
│   └── main.py             # FastAPI app + /genai/health
├── tests/test_health.py    # pytest + httpx
├── generated/              # gitignored
└── README.md
```

**Dependencies:** `fastapi`, `uvicorn[standard]`, `langchain`, `langchain-openai`. Dev: `pytest`, `pytest-asyncio`, `httpx`, `ruff`.

**`main.py`:** instantiates FastAPI, defines `GET /genai/health` returning `HealthStatus`. Swagger UI auto-mounts at `/docs`.

**Run command:** `uv run uvicorn genai.main:app --port 8084 --reload`

**Port:** `8084`.

### Web client (`web-client/`)

```
web-client/
├── package.json
├── pnpm-lock.yaml
├── tsconfig.json, tsconfig.node.json
├── vite.config.ts
├── tailwind.config.js, postcss.config.js
├── .eslintrc.cjs, .prettierrc
├── index.html
├── src/
│   ├── main.tsx, App.tsx
│   ├── index.css
│   ├── api/types.ts        # gitignored; openapi-typescript output
│   └── __tests__/App.test.tsx
└── README.md
```

**`package.json` scripts:** `dev`, `build` (= `tsc && vite build`), `lint`, `test` (= `vitest run`), `gen`.

**`App.tsx`:** placeholder UI ("TV Q&A — coming soon") styled with Tailwind.

**`App.test.tsx`:** one Vitest unit test asserting the placeholder heading renders.

**Dev port:** Vite default `5173`.

## Section 5 — Root hygiene files

### `README.md`

Sections:
- One-line project description + course attribution
- Structure table (path → description)
- One-time setup: `pre-commit install` (enables local OpenAPI lint hook)
- Quickstart (per-service dev commands, until Task 2 adds docker-compose)
- Swagger UI URLs per service

No team table, no CONTRIBUTING link.

### `.github/pull_request_template.md`

Checklist:
- Summary
- Affects API? → spec updated + codegen regenerated
- Tests added/updated
- Swagger UI verified for new endpoints
- CI green
- Docs updated if user-facing or ops-facing
- Test plan

### `docs/branch-protection.md`

Step-by-step GitHub UI instructions for the repo admin:
- Require PR before merge
- Require 1 approval
- Dismiss stale approvals on new commits
- Require status checks: `spec-lint`, three `java-services` matrix jobs, `genai`, `web-client`
- Require up-to-date branch
- Disallow bypassing

### `.gitignore`

Excludes:
- Generated artifacts: `services/*/generated/`, `web-client/src/api/types.ts`
- Java: `target/`, `*.class`, `.mvn/wrapper/maven-wrapper.jar`
- Python: `.venv/`, `__pycache__/`, `*.pyc`, `.pytest_cache/`, `.ruff_cache/`
- Node: `node_modules/`, `dist/`, `.vite/`
- IDEs: `.idea/`, `.vscode/`, `*.iml`
- OS: `.DS_Store`, `Thumbs.db`
- Env: `.env`, `.env.local`

### `.editorconfig`

UTF-8, LF endings, trim trailing whitespace, final newline. 2-space indent default; 4-space for Java and Python; tabs for Makefile.

## Definition of Done (Task 1)

- [ ] Repo layout matches Section 1
- [ ] `api/openapi.yaml` validates against Redocly
- [ ] `./api/scripts/gen-all.sh` succeeds locally and regenerates all clients
- [ ] All 4 services start on their assigned ports and respond 200 to `/health`
- [ ] Each service's tests pass locally (`./mvnw verify`, `pytest`, `pnpm test`)
- [ ] Each service exposes Swagger UI (Spring: `/swagger-ui.html`; FastAPI: `/docs`)
- [ ] PR opened against `main` triggers `ci.yml` with all jobs green
- [ ] Branch protection configured per `docs/branch-protection.md` (admin action)
- [ ] README at repo root explains structure + quickstart

## Known follow-ups (not blockers for Task 1)

- The course requires UML diagrams: **Subsystem Decomposition**, **Use Case Diagram**, **Analysis Object Model**. The repo currently has Class / Use Case / Component. "Class" maps to "Analysis Object Model"; "Component" is close to but not identical to "Subsystem Decomposition." Verify with the tutor and rename or supplement if needed.
- Team registration info (GitHub username + TUMonline + matriculation) must be **provided** (likely via the course platform); the brief does not mandate it be in the repo.
