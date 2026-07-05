# Requirements Compliance & Traceability Matrix

This document maps every requirement from the course **Project Details** and the
**Grading Criteria** to its current status in this repository. It is both a
checklist for the team and a navigation guide for tutors/examiners.

It is deliberately **honest about what is not built yet** — a doc that claims
features the code does not have is worse than no doc (the rubric penalises
documentation that is "inconsistent with implementation").

**Status legend:** ✅ Done · 🟡 Partial / skeleton · ⬜ Not started

**Snapshot — last updated 2026-07-05**

| Area | Status | One-line summary |
|---|---|---|
| Mono-repo & workflow | ✅ | Mono-repo, feature-branch + PR + review, branch protection runbook |
| Client (React) | ✅ | React + Vite + TS + Tailwind, served behind reverse proxy |
| Server (3 Spring Boot microservices) | ✅ | user-progress, catalog, chat (Q&A orchestration); `chat` calls GenAI; Traefik gateway |
| Database (PostgreSQL) | ✅ | Postgres 16 via Docker, schema for users/progress/series/episodes |
| GenAI (Python) | ✅ | FastAPI `/genai/ask`; LLM call via LangChain `ChatOpenAI` (cloud/local base_url); spoiler-safe |
| OpenAPI / Swagger | ✅ | Single-source `api/openapi.yaml`, Swagger UI per service |
| Local run (docker-compose) | ✅ | `docker compose up`, ≤3 commands, sane defaults |
| CI | ✅ | Build + test + lint + e2e per service on every PR |
| CD (cloud) | ✅ | Rancher (Helm, auto-on-merge, `deploy.yml`) + Azure VM (Terraform+Ansible+Compose, `cd-azure.yml`) |
| Kubernetes (Helm/manifests) | ✅ | Helm chart in `infra/k8s/` (Rancher) incl. ServiceMonitor/PrometheusRule; Azure uses Terraform VM + Compose (documented trade-off) |
| Observability (Prometheus/Grafana/alerts) | ✅ | Prometheus + 2 Grafana dashboards + 2 alert rules; k8s ServiceMonitor/PrometheusRule/dashboard ConfigMap |
| Testing | ✅ | Unit + integration per service (incl. GenAI chain + spoiler-trap); Playwright e2e in CI |
| Engineering artefacts (UML, architecture) | ✅ | 3 UML diagrams + architecture doc |
| Documentation (README, responsibilities) | ✅ | README covers setup, CI/CD, testing, monitoring, responsibilities; `docs/responsibilities.md` RACI |

> **Biggest risks right now** (baseline features are now built — GenAI Q&A, CD,
> and Kubernetes are all implemented): (1) keeping a **stable tutor-accessible
> URL live** that reflects the final submission; (2) a **local `catalog` Flyway
> boot failure** observed on stale DB volumes (verify a clean boot / `down -v`);
> and (3) **alert notification routing** is not wired (alert *rules* exist, but
> no Alertmanager delivery). The remaining hard-fail exposure is the
> deployed-URL availability, not missing functionality.

---

## 1. Required system elements (Project Details)

| Requirement | Status | Evidence / Location | What's left |
|---|---|---|---|
| GitHub mono-repo | ✅ | repo root: `api/`, `services/`, `web-client/`, `infra/`, `docs/` | — |
| Client side | ✅ | `web-client/` (React + Vite + TS + Tailwind) | — |
| Server side (Spring Boot, ≥3 microservices) | ✅ | `services/user-progress`, `services/catalog`, `services/chat` (Q&A orchestration) | — |
| Database (persistent, documented schema) | ✅ | Postgres 16 in `infra/docker-compose.yml`; schema in service migrations + `docs/system-architecture.md` | — |
| GenAI as separate Python service | ✅ | `services/genai` (FastAPI `/genai/ask` + LLM call via LangChain `ChatOpenAI`) | — |
| CI/CD | ✅ | `.github/workflows/ci.yml` + `deploy.yml` (Rancher CD) + `cd-azure.yml` (Azure VM CD) | Keep live deployment reachable |
| Kubernetes | ✅ | `infra/k8s/` Helm chart (Rancher) incl. ServiceMonitor/PrometheusRule; Azure is a Terraform VM + Compose (documented trade-off) | — |
| Monitoring | ✅ | `infra/observability/` — Prometheus + 2 Grafana dashboards + 2 alert rules; k8s ServiceMonitor/PrometheusRule | Wire alert notification routing |

## 2. Development workflow

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| GitHub mono-repo | ✅ | repo structure | — |
| Feature branch per change | ✅ | branch naming e.g. `atharva/update-docs`; git history | — |
| PRs mandatory before merge to main | ✅ | `docs/branch-protection.md`, merged PRs (#6–#10) | — |
| Peer code review + approval | ✅ | branch-protection requires 1 approval; PR history | Keep review participation visible (graded) |
| CI on every PR (build + test) | ✅ | `ci.yml` runs on `pull_request` | — |
| CD: auto-deploy to k8s on merge to main | ✅ | `deploy.yml` auto-deploys to Rancher on green CI; Azure VM CD is manual (`cd-azure.yml`, billed-while-running) | Keep live URL available for evaluation |

## 3. System architecture

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| Client communicates with server over REST | ✅ | `web-client/src/api/`, generated TS types from OpenAPI | — |
| Server exposes REST APIs | ✅ | `api/openapi.yaml` paths for user-progress, catalog, chat | — |
| Server = Spring Boot | ✅ | `services/*/pom.xml`, Java 21, Spring Boot | — |
| ≥3 microservices, distinct responsibilities | ✅ | user-progress (auth+progress), catalog (series+episodes), chat (spoiler-safe Q&A orchestration) | — |
| Database with documented schema | ✅ | `docs/system-architecture.md` §1.1 (Analysis Object Model) | — |
| GenAI runs as independent service over defined interface | ✅ | separate service + container; `chat` → `genai` over `POST /genai/ask` | — |

## 4. GenAI component

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| Implemented in Python, separate, containerised | ✅ | `services/genai/` FastAPI + `Dockerfile` | — |
| Networked with server over defined interface | ✅ | `chat` `GenAiClient` → `POST /genai/ask` (`services/chat/.../client/GenAiClient.java`) | — |
| Real user-facing use case (Q&A) | ✅ | spoiler-safe Q&A end to end (progress-bounded context + double-filtered citations) | — |
| Cloud model support (e.g. OpenAI API) | ✅ | `ChatOpenAI` with configurable `LLM_BASE_URL` / `LOGOS_API_KEY` (`services/genai/src/genai/chain.py`) | — |
| Local model support (e.g. GPT4All/LLaMA) | ✅ | same OpenAI-compatible client, point `LLM_BASE_URL` at a local endpoint | — |
| **Bonus:** full RAG with vector DB (Weaviate) | ⬜ | explicitly deferred — see `docs/system-architecture.md` "What's deferred" | Post-MVP |

## 5. Environment & deployment

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| Every component has its own Dockerfile | ✅ | `web-client/Dockerfile`, `services/Dockerfile.spring`, `services/genai/Dockerfile` | — |
| `docker-compose.yml` runs system end-to-end locally | ✅ | `infra/docker-compose.yml` (postgres + 3 java + genai + web) | — |
| Runnable in ≤3 commands, sane defaults | ✅ | `docker compose -f infra/docker-compose.yml up --build`; defaults in compose | — |
| Externalised config (env vars / secrets) | 🟡 | `infra/env.example`, env-driven datasource/JWT | No secrets manager yet (fine until k8s) |
| Deployable to Kubernetes (Helm or manifests) | ✅ | `infra/k8s/` Helm chart (`deploy.sh rancher`) | — |
| Rancher + Azure environments | ✅ | Rancher via Helm (`deploy.yml`); Azure via Terraform VM + Ansible + `docker-compose.azure.yml` (TLS overlay, `cd-azure.yml`) | Keep a live URL up for evaluation |

## 6. CI/CD

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| GitHub Actions | ✅ | `.github/workflows/ci.yml` | — |
| CI builds all services | ✅ | matrix: java-services + genai + web-client | — |
| CI tests all services | ✅ | java `verify` (incl. chat Q&A integration), `pytest` (genai chain + spoiler), `pnpm test` + Playwright e2e | — |
| CI static analysis / linting | ✅ | redocly (spec), ruff (py), eslint (web) | Consider adding Java static analysis |
| CD auto-deploy to k8s on merge | ✅ | `deploy.yml` (Rancher, auto-on-merge); `cd-azure.yml` (Azure VM, manual) | Keep live URL available |
| Secrets / env-specific config in pipeline | ✅ | GitHub Secrets + Azure OIDC (no stored client secret); rendered into k8s Secrets / VM `.env` | — |

## 7. Observability

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| Prometheus metrics collection | ✅ | `infra/observability/prometheus/` scrapes Spring `/actuator/prometheus` + genai `/metrics`; k8s via `ServiceMonitor` | — |
| Track request count, latency, error rate | ✅ | Micrometer `http_server_requests_*` (Spring) + `http_requests_total` / `http_request_duration_seconds_*` (genai) | — |
| Grafana dashboards reflecting system state | ✅ | "TSO — System Overview" + Traefik gateway dashboard, auto-provisioned from `infra/observability/grafana/` | — |
| Dashboards exported as `.json` in repo | ✅ | `infra/observability/grafana/dashboards/tso-overview.json`, `traefik-dashboard.json` | — |
| ≥1 meaningful alert rule | ✅ | `infra/observability/prometheus/alerts.yml`: `ServiceDown` (up==0 for 1m, critical), `HighErrorRate` (5xx ratio >0.05 for 5m, warning) | Alertmanager / notification routing not yet wired |

## 8. Testing

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| Unit tests for critical server logic | ✅ | `user-progress` (auth, JWT, progress, seed), `catalog`, `chat` (Q&A orchestration integration) | — |
| Tests for GenAI logic | ✅ | genai chain/parse tests + spoiler-trap coverage (`services/genai/tests/`) | — |
| Client tests on core workflows | ✅ | `web-client/src/**/__tests__/*` (pages, components, api) | — |
| All tests run automatically in CI | ✅ | `ci.yml` per-service test steps + Playwright e2e | — |
| Edge-case / failure coverage | ✅ | spoiler-trap suite (`QuestionsControllerIntegrationTest`): filters citations beyond progress, no-progress + unknown-series + auth failures | Extend as features grow |

## 9. Engineering artefacts & documentation

| Requirement | Status | Evidence | What's left |
|---|---|---|---|
| High-level architecture description | ✅ | `docs/system-architecture.md` | — |
| Subsystem decomposition + interfaces | ✅ | `docs/system-architecture.md` §1.3 + component table | — |
| UML: Analysis Object Model | ✅ | `docs/diagrams/Class Diagram.png` | — |
| UML: Use Case Diagram | ✅ | `docs/diagrams/Use Case Diagram.png` | — |
| UML: Subsystem Decomposition / top-level architecture | ✅ | `docs/diagrams/Component Diagram.png` | — |
| OpenAPI/Swagger documentation | ✅ | `api/openapi.yaml`; Swagger UI per service (README) | Extend spec as Q&A lands |
| README: setup, architecture, API, CI/CD, monitoring, responsibilities | ✅ | `README.md` + `docs/responsibilities.md` (RACI) |
| Student responsibilities documented & traceable | ✅ | `docs/responsibilities.md` — owners, RACI, GitHub traceability |
| Problem statement | ✅ | `docs/problem-statement.md` | — |

## 10. Deliverables checklist

| Deliverable | Status | Location |
|---|---|---|
| Source: client | ✅ | `web-client/` |
| Source: server (3 services) | ✅ | `services/user-progress`, `catalog`, `chat` (Q&A orchestration) |
| Source: GenAI | ✅ | `services/genai/` (`/genai/ask` + LLM chain) |
| Dockerfiles + docker-compose | ✅ | `web-client/`, `services/`, `infra/docker-compose.yml` |
| Kubernetes (Helm/YAML) + instructions | ✅ | `infra/k8s/` (Helm chart + README, Rancher); Azure VM IaC in `infra/terraform/` + `infra/ansible/` |
| Monitoring config + exported dashboards + alert rules | ✅ | `infra/observability/` (Prometheus config, `tso-overview.json`, `traefik-dashboard.json`, `alerts.yml`) |
| Testing suite + run instructions | 🟡 | unit + integration + e2e present; consolidate run instructions |
| Documentation (README + responsibilities) | ✅ | `README.md` + `docs/responsibilities.md` |

## 11. Hard-fail criteria — must all be avoided

| Fail condition | Current standing | Note |
|---|---|---|
| Contributions not transparently documented (Artemis + GitHub) | ✅ | GitHub history + PR reviews + `docs/responsibilities.md`; keep Artemis activity visible |
| A member cannot explain their own subsystem | n/a (process) | Maintain clear ownership; oral exam prep |
| No working end-to-end system demonstrated | ⚠️ at risk (URL) | Core Q&A path is wired end to end; risk is now **keeping a stable deployed URL live** + a clean `catalog` boot |

## 12. Bonus opportunities (rubric)

| Bonus | Status | Note |
|---|---|---|
| Advanced DevOps (autoscaling, self-healing, deploy strategies) | 🟡 | Self-healing via liveness/readiness probes + IaC (Terraform/Ansible) + Azure OIDC; autoscaling/HPA not yet added |
| Advanced observability (tracing, log aggregation, custom metrics) | 🟡 | Custom per-service + Traefik gateway metrics/dashboards; no tracing or log aggregation yet |
| Advanced AI (RAG, vector DB) | 🟡 | Spoiler-safe context filtering (progress-bounded) as a candidate extension; full RAG/vector DB deferred (Weaviate) |

---

## Immediate focus (agreed)

The baseline is now built end to end — GenAI Q&A, chat orchestration, CI/CD
(Rancher + Azure), Kubernetes (Helm), and observability are all in place. The
remaining active workstream is **hardening for evaluation**:

1. **Live deployment** — keep a stable tutor-accessible URL up that reflects the
   final submission (the primary hard-fail exposure).
2. **Clean boot** — resolve/confirm the local `catalog` Flyway startup failure
   (verify it is a stale-volume issue via `down -v`, not a real bug).
3. **Alert delivery** — wire Alertmanager/notification routing (rules exist;
   delivery does not).
4. **Documentation** — fix the stale `infra/k8s/README.md` "not yet included" note
   (ServiceMonitor/PrometheusRule now exist).

**✅ DONE:** GenAI Q&A (`/genai/ask` + LLM call, spoiler-safe), `chat` → `genai`
orchestration with integration/spoiler-trap tests, CI/CD to Rancher + Azure,
Kubernetes Helm chart (incl. ServiceMonitor/PrometheusRule), and observability
(Prometheus metrics, two Grafana dashboards exported as `.json`, two alert
rules). See `infra/observability/`, `infra/k8s/`, and `docs/observability.md`.

See [`docs/grading-dashboard.md`](grading-dashboard.md) for the criterion-level
status view.
