# Requirements Compliance & Traceability Matrix

This document maps every requirement from the course **Project Details** and the
**Grading Criteria** to its current status in this repository. It is both a
checklist for the team and a navigation guide for tutors/examiners.

It is deliberately **honest about what is not built yet** — a doc that claims
features the code does not have is worse than no doc (the rubric penalises
documentation that is "inconsistent with implementation").

**Status legend:** ✅ Done · 🟡 Partial / skeleton · ⬜ Not started

**Snapshot — last updated 2026-06-05**

| Area | Status | One-line summary |
|---|---|---|
| Mono-repo & workflow | ✅ | Mono-repo, feature-branch + PR + review, branch protection runbook |
| Client (React) | ✅ | React + Vite + TS + Tailwind, served behind reverse proxy |
| Server (3 Spring Boot microservices) | 🟡 | 3 services exist; `chat` is still a health-only skeleton |
| Database (PostgreSQL) | ✅ | Postgres 16 via Docker, schema for users/progress/series/episodes |
| GenAI (Python) | ⬜ | FastAPI skeleton only — **no LLM call, no `/ask`, no model support** |
| OpenAPI / Swagger | ✅ | Single-source `api/openapi.yaml`, Swagger UI per service |
| Local run (docker-compose) | ✅ | `docker compose up`, ≤3 commands, sane defaults |
| CI | ✅ | Build + test + lint per service on every PR |
| CD to Kubernetes | ⬜ | Not built — owned by separate deployment workstream |
| Kubernetes (Helm/manifests) | ⬜ | Not built — owned by separate deployment workstream |
| Observability (Prometheus/Grafana/alerts) | ✅ | Local via docker-compose: Prometheus + Grafana dashboard + 2 alert rules; k8s monitoring still owned by deployment workstream |
| Testing | 🟡 | Good coverage on built services; chat/genai untested because unbuilt |
| Engineering artefacts (UML, architecture) | ✅ | 3 UML diagrams + architecture doc |
| Documentation (README, responsibilities) | 🟡 | README strong; **student-responsibilities doc missing** |

> **Biggest risks right now** (not documentation): the central **GenAI Q&A
> feature is unimplemented**, and **CD and Kubernetes do not exist**
> (observability is now in place locally). These map to the heavily-weighted
> rubric categories *User-Facing Value* and *Build and Deployment*, and to the
> hard-fail criterion "no working end-to-end system is demonstrated."

---

## 1. Required system elements (Project Details)

| Requirement | Status | Evidence / Location | What's left |
|---|---|---|---|
| GitHub mono-repo | ✅ | repo root: `api/`, `services/`, `web-client/`, `infra/`, `docs/` | — |
| Client side | ✅ | `web-client/` (React + Vite + TS + Tailwind) | — |
| Server side (Spring Boot, ≥3 microservices) | 🟡 | `services/user-progress`, `services/catalog`, `services/chat` | `chat` is health-only; build Q&A orchestration |
| Database (persistent, documented schema) | ✅ | Postgres 16 in `infra/docker-compose.yml`; schema in service migrations + `docs/system-architecture.md` | — |
| GenAI as separate Python service | ⬜ | `services/genai` (FastAPI, health endpoint only) | Implement `/ask`, prompt, LLM call |
| CI/CD | 🟡 | `.github/workflows/ci.yml` (CI only) | Add CD workflow |
| Kubernetes | ⬜ | — | Separate deployment workstream |
| Monitoring | ✅ | `infra/observability/` — Prometheus + Grafana + 2 alert rules (local via docker-compose) | k8s monitoring owned by deployment workstream |

## 2. Development workflow

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| GitHub mono-repo | ✅ | repo structure | — |
| Feature branch per change | ✅ | branch naming e.g. `atharva/update-docs`; git history | — |
| PRs mandatory before merge to main | ✅ | `docs/branch-protection.md`, merged PRs (#6–#10) | — |
| Peer code review + approval | ✅ | branch-protection requires 1 approval; PR history | Keep review participation visible (graded) |
| CI on every PR (build + test) | ✅ | `ci.yml` runs on `pull_request` | — |
| CD: auto-deploy to k8s on merge to main | ⬜ | — | Build CD workflow + k8s target |

## 3. System architecture

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| Client communicates with server over REST | ✅ | `web-client/src/api/`, generated TS types from OpenAPI | — |
| Server exposes REST APIs | ✅ | `api/openapi.yaml` paths for user-progress, catalog, chat | — |
| Server = Spring Boot | ✅ | `services/*/pom.xml`, Java 21, Spring Boot | — |
| ≥3 microservices, distinct responsibilities | ✅ (count) / 🟡 (depth) | user-progress (auth+progress), catalog (series+episodes), chat (Q&A) | `chat` responsibility not yet implemented |
| Database with documented schema | ✅ | `docs/system-architecture.md` §1.1 (Analysis Object Model) | — |
| GenAI runs as independent service over defined interface | 🟡 | separate service + container; interface in OpenAPI is health-only | Define + implement `/ask` contract |

## 4. GenAI component

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| Implemented in Python, separate, containerised | ✅ (shell) | `services/genai/` FastAPI + `Dockerfile` | — |
| Networked with server over defined interface | ⬜ | only `/genai/health` in spec | Add `/ask` to OpenAPI; wire `chat` → `genai` |
| Real user-facing use case (Q&A) | ⬜ | — | Implement spoiler-safe Q&A end to end |
| Cloud model support (e.g. OpenAI API) | ⬜ | — | Add provider abstraction + OpenAI backend |
| Local model support (e.g. GPT4All/LLaMA) | ⬜ | — | Add local backend, env-switchable |
| **Bonus:** full RAG with vector DB (Weaviate) | ⬜ | explicitly deferred — see `docs/system-architecture.md` "What's deferred" | Post-MVP |

## 5. Environment & deployment

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| Every component has its own Dockerfile | ✅ | `web-client/Dockerfile`, `services/Dockerfile.spring`, `services/genai/Dockerfile` | — |
| `docker-compose.yml` runs system end-to-end locally | ✅ | `infra/docker-compose.yml` (postgres + 3 java + genai + web) | — |
| Runnable in ≤3 commands, sane defaults | ✅ | `docker compose -f infra/docker-compose.yml up --build`; defaults in compose | — |
| Externalised config (env vars / secrets) | 🟡 | `infra/env.example`, env-driven datasource/JWT | No secrets manager yet (fine until k8s) |
| Deployable to Kubernetes (Helm or manifests) | ⬜ | — | Deployment workstream |
| Rancher + Azure environments | ⬜ | — | Deployment workstream |

## 6. CI/CD

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| GitHub Actions | ✅ | `.github/workflows/ci.yml` | — |
| CI builds all services | ✅ | matrix: java-services + genai + web-client | Add `chat` Q&A build once implemented |
| CI tests all services | 🟡 | java `verify`, `pytest`, `pnpm test` | genai/chat tests thin (services unbuilt) |
| CI static analysis / linting | ✅ | redocly (spec), ruff (py), eslint (web) | Consider adding Java static analysis |
| CD auto-deploy to k8s on merge | ⬜ | — | Deployment workstream |
| Secrets / env-specific config in pipeline | ⬜ | — | Comes with CD |

## 7. Observability

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| Prometheus metrics collection | ✅ | `infra/observability/prometheus/` scrapes Spring `/actuator/prometheus` + genai `/metrics` | k8s scraping owned by deployment workstream |
| Track request count, latency, error rate | ✅ | Micrometer `http_server_requests_*` (Spring) + `http_requests_total` / `http_request_duration_seconds_*` (genai) | — |
| Grafana dashboards reflecting system state | ✅ | "TSO — System Overview" auto-provisioned from `infra/observability/grafana/` | — |
| Dashboards exported as `.json` in repo | ✅ | `infra/observability/grafana/dashboards/tso-overview.json` | — |
| ≥1 meaningful alert rule | ✅ | `infra/observability/prometheus/alerts.yml`: `ServiceDown`, `HighErrorRate` | Alertmanager / notification routing not yet wired |

## 8. Testing

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| Unit tests for critical server logic | ✅ | `user-progress` (auth, JWT, progress, seed), `catalog`, `chat` (health) | Add tests as `chat` grows |
| Tests for GenAI logic | ⬜ | only `services/genai/tests/test_health.py` | Add once `/ask` exists |
| Client tests on core workflows | ✅ | `web-client/src/**/__tests__/*` (pages, components, api) | — |
| All tests run automatically in CI | ✅ | `ci.yml` per-service test steps | — |
| Edge-case / failure coverage | 🟡 | mostly happy-path + integration | Spoiler-trap suite is the key correctness artifact (see architecture doc) |

## 9. Engineering artefacts & documentation

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| High-level architecture description | ✅ | `docs/system-architecture.md` | — |
| Subsystem decomposition + interfaces | ✅ | `docs/system-architecture.md` §1.3 + component table | — |
| UML: Analysis Object Model | ✅ | `docs/diagrams/Class Diagram.png` | — |
| UML: Use Case Diagram | ✅ | `docs/diagrams/Use Case Diagram.png` | — |
| UML: Subsystem Decomposition / top-level architecture | ✅ | `docs/diagrams/Component Diagram.png` | — |
| OpenAPI/Swagger documentation | ✅ | `api/openapi.yaml`; Swagger UI per service (README) | Extend spec as Q&A lands |
| README: setup, architecture, API, CI/CD, monitoring, responsibilities | 🟡 | `README.md` (setup/arch/API/workflow + monitoring); `docs/observability.md` | Add responsibilities |
| Student responsibilities documented & traceable | ⬜ | — | **Add `docs/responsibilities.md` (RACI)** — fail criterion |
| Problem statement | ✅ | `docs/problem-statement.md` | — |

## 10. Deliverables checklist

| Deliverable | Status | Location |
|---|---|---|
| Source: client | ✅ | `web-client/` |
| Source: server (3 services) | 🟡 | `services/` (chat skeleton) |
| Source: GenAI | ⬜ | `services/genai/` (skeleton) |
| Dockerfiles + docker-compose | ✅ | `web-client/`, `services/`, `infra/docker-compose.yml` |
| Kubernetes (Helm/YAML) + instructions | ⬜ | deployment workstream |
| Monitoring config + exported dashboards + alert rules | ✅ | `infra/observability/` (Prometheus config, `tso-overview.json`, `alerts.yml`) |
| Testing suite + run instructions | 🟡 | tests present; run instructions to add |
| Documentation (README + responsibilities) | 🟡 | `README.md`; responsibilities missing |

## 11. Hard-fail criteria — must all be avoided

| Fail condition | Current standing | Note |
|---|---|---|
| Contributions not transparently documented (Artemis + GitHub) | ⚠️ at risk | GitHub history good; **add responsibilities doc** + keep Artemis activity visible |
| A member cannot explain their own subsystem | n/a (process) | Maintain clear ownership; oral exam prep |
| No working end-to-end system demonstrated | ⚠️ at risk | **Core Q&A path not yet wired end to end** — top priority |

## 12. Bonus opportunities (rubric)

| Bonus | Status | Note |
|---|---|---|
| Advanced DevOps (autoscaling, self-healing, deploy strategies) | ⬜ | After baseline CD/k8s |
| Advanced observability (tracing, log aggregation, custom metrics) | ⬜ | After baseline metrics |
| Advanced AI (RAG, vector DB) | ⬜ | Deferred north-star (Weaviate) |

---

## Immediate focus (agreed)

Per current team decision, the active workstream — **excluding k8s/deployment,
which a separate effort owns** — is:

1. **Testing infrastructure** — broaden beyond happy-path; stand up the
   spoiler-trap correctness suite; ensure CI gates on it.
2. **CI/CD** — strengthen CI (coverage, Java static analysis) and lay the CD
   groundwork that the deployment workstream will target.

**✅ DONE (local):** Observability — Prometheus metrics (request count, latency,
error rate) on the Spring services and GenAI, a Grafana dashboard exported as
`.json`, and two alert rules. Delivered locally via docker-compose; see
`infra/observability/` and `docs/observability.md`. Wiring the same metrics
endpoints into the k8s monitoring stack remains with the deployment workstream.

The GenAI Q&A feature and Kubernetes deployment are tracked here but owned
elsewhere; this matrix should be updated as each item lands.
