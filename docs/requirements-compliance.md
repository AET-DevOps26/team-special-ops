# Requirements Compliance & Traceability Matrix

This document maps every requirement from the course **Project Details** and the
**Grading Criteria** to its current status in this repository. It is both a
checklist for the team and a navigation guide for tutors/examiners.

It is deliberately **honest about what is not built yet** — a doc that claims
features the code does not have is worse than no doc (the rubric penalises
documentation that is "inconsistent with implementation").

**Status legend:** ✅ Done · 🟡 Partial / skeleton · ⬜ Not started

**Snapshot — last updated 2026-06-28**

| Area | Status | One-line summary |
|---|---|---|
| Mono-repo & workflow | ✅ | Mono-repo, feature-branch + PR + review, branch protection runbook |
| Client (React) | ✅ | React + Vite + TS + Tailwind; auth, library, series detail, chat panel |
| Server (3 Spring Boot microservices) | ✅ | user-progress (auth + progress), catalog (series + episodes), chat (Q&A orchestration) |
| Database (PostgreSQL) | ✅ | Postgres 16 via Docker, schema for users/progress/series/episodes, Flyway migrations |
| GenAI (Python) | ✅ | FastAPI + LangChain; `/ask` endpoint with TUM Logos backend; tests |
| OpenAPI / Swagger | ✅ | Single-source `api/openapi.yaml`, Swagger UI per service |
| Local run (docker-compose) | ✅ | `docker compose up`, ≤3 commands, sane defaults |
| CI | ✅ | Build + test + lint per service on every PR |
| CD to Kubernetes (Rancher) | ✅ | Auto-deploys on merge to main via `deploy.yml`; Helm chart |
| Kubernetes (Helm chart) | ✅ | `infra/k8s/chart/` — full Helm chart, TLS via cert-manager |
| Kubernetes (Azure) | 🟡 | Terraform + Ansible plan exists; AKS provisioning owned by Tejash |
| Observability (Prometheus/Grafana/alerts) | ✅ | Local + k8s: Prometheus scraping, Grafana dashboard (exported JSON), 2 alert rules |
| Testing | 🟡 | Good coverage on user-progress, catalog, genai; chat service tests thin |
| Engineering artefacts (UML, architecture) | ✅ | 3 UML diagrams + architecture doc + OpenAPI |
| Documentation (README, responsibilities) | ✅ | README + `docs/responsibilities.md` |

---

## 1. Required system elements (Project Details)

| Requirement | Status | Evidence / Location | What's left |
|---|---|---|---|
| GitHub mono-repo | ✅ | repo root: `api/`, `services/`, `web-client/`, `infra/`, `docs/` | — |
| Client side | ✅ | `web-client/` (React + Vite + TS + Tailwind) | — |
| Server side (Spring Boot, ≥3 microservices) | ✅ | `services/user-progress`, `services/catalog`, `services/chat` | — |
| Database (persistent, documented schema) | ✅ | Postgres 16 in `infra/docker-compose.yml`; schema in Flyway migrations + `docs/system-architecture.md` | — |
| GenAI as separate Python service | ✅ | `services/genai/` — FastAPI + LangChain; `/ask` with prompt template + LLM call | — |
| CI/CD | ✅ | `ci.yml` (CI) + `deploy.yml` (CD to Rancher on merge to main) | — |
| Kubernetes | ✅ | `infra/k8s/chart/` Helm chart; deployed at `team-special-ops.stud.k8s.aet.cit.tum.de` | Azure environment (Tejash) |
| Monitoring | ✅ | `infra/observability/` — Prometheus + Grafana + 2 alert rules; k8s scraping via actuator endpoints | — |

## 2. Development workflow

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| GitHub mono-repo | ✅ | repo structure | — |
| Feature branch per change | ✅ | branch naming convention; git history | — |
| PRs mandatory before merge to main | ✅ | `docs/branch-protection.md`, PR history | — |
| Peer code review + approval | ✅ | branch protection requires 1 approval; PR history | Keep review participation visible |
| CI on every PR (build + test) | ✅ | `ci.yml` runs on `pull_request` | — |
| CD: auto-deploy to k8s on merge to main | ✅ | `deploy.yml` triggers on CI success on main; deploys via Helm to Rancher | — |

## 3. System architecture

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| Client communicates with server over REST | ✅ | `web-client/src/api/`, generated TS types from OpenAPI | — |
| Server exposes REST APIs | ✅ | `api/openapi.yaml` paths for user-progress, catalog, chat | — |
| Server = Spring Boot | ✅ | `services/*/pom.xml`, Java 21, Spring Boot 3 | — |
| ≥3 microservices, distinct responsibilities | ✅ | user-progress (auth+progress), catalog (series+episodes), chat (Q&A orchestration → GenAI) | — |
| Database with documented schema | ✅ | `docs/system-architecture.md` §1.1 (Analysis Object Model); Flyway migration files | — |
| GenAI runs as independent service over defined interface | ✅ | separate FastAPI service; `/ask` contract in `api/openapi.yaml`; chat service calls it via HTTP | — |

## 4. GenAI component

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| Implemented in Python, separate, containerised | ✅ | `services/genai/` FastAPI + `Dockerfile` | — |
| Networked with server over defined interface | ✅ | `/ask` in `api/openapi.yaml`; `chat` service calls genai via `GenAiClient` | — |
| Real user-facing use case (Q&A) | ✅ | Spoiler-safe chat: question + allowed episode summaries → LLM answer with citations | — |
| Cloud model support | ✅ | TUM Logos (`openai/gpt-oss-120b`); configurable via `LLM_BASE_URL` + `LLM_MODEL` env vars | — |
| Local model support (e.g. GPT4All/LLaMA) | 🟡 | `LLM_BASE_URL` is env-switchable; compatible with any OpenAI-compatible endpoint (e.g. Ollama) | Document Ollama setup in README |
| **Bonus:** full RAG with vector DB (Weaviate) | ⬜ | explicitly deferred — see `docs/system-architecture.md` "What's deferred" | Post-MVP |

## 5. Environment & deployment

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| Every component has its own Dockerfile | ✅ | `web-client/Dockerfile`, `services/Dockerfile.spring`, `services/genai/Dockerfile` | — |
| `docker-compose.yml` runs system end-to-end locally | ✅ | `infra/docker-compose.yml` (postgres + 3 java + genai + web) | — |
| Runnable in ≤3 commands, sane defaults | ✅ | `docker compose -f infra/docker-compose.yml up --build`; defaults in compose | — |
| Externalised config (env vars / secrets) | ✅ | `infra/env.example`; env-driven datasource, JWT, LLM keys; k8s uses Helm + K8s Secrets | — |
| Deployable to Kubernetes (Helm) | ✅ | `infra/k8s/chart/` — deployed on Rancher | — |
| Rancher environment | ✅ | `https://team-special-ops.stud.k8s.aet.cit.tum.de` | — |
| Azure environment | 🟡 | Terraform + Ansible automation exists; AKS deployment in progress | Tejash |

## 6. CI/CD

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| GitHub Actions | ✅ | `.github/workflows/ci.yml`, `.github/workflows/deploy.yml` | — |
| CI builds all services | ✅ | matrix: java-services (user-progress, catalog, chat) + genai + web-client | — |
| CI tests all services | 🟡 | java `verify`, `pytest`, `pnpm test` — chat tests thin | Broaden chat test coverage |
| CI static analysis / linting | ✅ | redocly (spec), ruff (py), eslint (web), spotless (java) | — |
| CD auto-deploy to k8s on merge | ✅ | `deploy.yml` triggers on `workflow_run` success on main; runs `deploy.sh rancher` | — |
| Secrets / env-specific config in pipeline | ✅ | `JWT_SECRET`, `POSTGRES_PASSWORD`, `LOGOS_API_KEY`, `KUBECONFIG_B64` via GitHub Secrets | — |

## 7. Observability

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| Prometheus metrics collection | ✅ | `infra/observability/prometheus/` scrapes Spring `/actuator/prometheus` + genai `/metrics` | — |
| Track request count, latency, error rate | ✅ | Micrometer `http_server_requests_*` (Spring) + `http_requests_total` / `http_request_duration_seconds_*` (genai) | — |
| Grafana dashboards reflecting system state | ✅ | "TSO — System Overview" auto-provisioned from `infra/observability/grafana/` | — |
| Dashboards exported as `.json` in repo | ✅ | `infra/observability/grafana/dashboards/tso-overview.json` | — |
| ≥1 meaningful alert rule | ✅ | `infra/observability/prometheus/alerts.yml`: `ServiceDown`, `HighErrorRate` | Alertmanager notification routing not wired |

## 8. Testing

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| Unit tests for critical server logic | 🟡 | `user-progress` (auth, JWT, progress, seed), `catalog` — chat tests thin | Broaden chat coverage |
| Tests for GenAI logic | ✅ | `services/genai/tests/test_ask.py`, `test_prompts.py`, `test_metrics.py` | — |
| Client tests on core workflows | ✅ | `web-client/src/**/__tests__/*` (pages, components, api) | — |
| All tests run automatically in CI | ✅ | `ci.yml` per-service test steps | — |

## 9. Engineering artefacts & documentation

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| High-level architecture description | ✅ | `docs/system-architecture.md` | — |
| Subsystem decomposition + interfaces | ✅ | `docs/system-architecture.md` §1.3 + component table | — |
| UML: Analysis Object Model | ✅ | `docs/diagrams/Class Diagram.png` | — |
| UML: Use Case Diagram | ✅ | `docs/diagrams/Use Case Diagram.png` | — |
| UML: Subsystem Decomposition | ✅ | `docs/diagrams/Component Diagram.png` | — |
| OpenAPI/Swagger documentation | ✅ | `api/openapi.yaml`; Swagger UI per service (see README) | — |
| README: setup, architecture, API, CI/CD, monitoring, responsibilities | ✅ | `README.md`; `docs/observability.md`; `docs/responsibilities.md` | — |
| Student responsibilities documented & traceable | ✅ | `docs/responsibilities.md` — RACI table + contribution summary per student | — |
| Problem statement | ✅ | `docs/problem-statement.md` | — |

## 10. Deliverables checklist

| Deliverable | Status | Location |
|---|---|---|
| Source: client | ✅ | `web-client/` |
| Source: server (3 services) | ✅ | `services/user-progress/`, `services/catalog/`, `services/chat/` |
| Source: GenAI | ✅ | `services/genai/` |
| Dockerfiles + docker-compose | ✅ | `web-client/Dockerfile`, `services/Dockerfile.spring`, `services/genai/Dockerfile`, `infra/docker-compose.yml` |
| Kubernetes Helm chart + instructions | ✅ | `infra/k8s/chart/`; `docs/project-guidelines/kubernetes-deployment-plan.md` |
| Monitoring config + exported dashboards + alert rules | ✅ | `infra/observability/` (Prometheus config, `tso-overview.json`, `alerts.yml`) |
| Testing suite + run instructions | 🟡 | tests present in each service; run instructions in README |
| Documentation (README + responsibilities) | ✅ | `README.md`; `docs/responsibilities.md` |

## 11. Hard-fail criteria — must all be avoided

| Fail condition | Current standing | Note |
|---|---|---|
| Contributions not transparently documented (Artemis + GitHub) | ✅ clear | `docs/responsibilities.md` + visible GitHub PR/commit history |
| A member cannot explain their own subsystem | n/a (process) | Clear ownership in responsibilities doc; maintain oral exam prep |
| No working end-to-end system demonstrated | ✅ working | Full flow live at `team-special-ops.stud.k8s.aet.cit.tum.de`; login → series → chat works |

## 12. Bonus opportunities (rubric)

| Bonus | Status | Note |
|---|---|---|
| Advanced DevOps (autoscaling, self-healing, deploy strategies) | ⬜ | After Azure baseline |
| Advanced observability (tracing, log aggregation, custom metrics) | ⬜ | After alertmanager wiring |
| Advanced AI (RAG, vector DB) | ⬜ | Deferred north-star (Weaviate) |

---

## Remaining focus

1. **Chat service tests** — broaden beyond health-only; cover the Q&A orchestration path.
2. **Azure deployment** — AKS provisioning (Tejash); reuses same Helm chart with `values-azure.yaml`.
3. **Local model documentation** — document Ollama as a drop-in `LLM_BASE_URL` backend for local inference.
4. **Alertmanager routing** — wire notification channel (Slack/email) for the existing alert rules.
