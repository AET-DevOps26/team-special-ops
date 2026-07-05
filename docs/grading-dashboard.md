# Grading Status Dashboard

A per-criterion status view mapped directly to `docs/project-guidelines/project-grading.md`.
For the detailed requirement-by-requirement matrix, see
[`docs/requirements-compliance.md`](requirements-compliance.md).

**Legend:** `Done` = meets baseline · `Partial` = works with gaps · the
*Action* column is what closes the gap.

**Snapshot — last updated 2026-07-05**

## Fail-condition gates

These three conditions fail the project outright if unmet.

| Gate | Status | Evidence | Action |
|---|---|---|---|
| Contributions documented (Artemis + GitHub) | Done | Feature branches, mandatory PRs + reviews (`docs/branch-protection.md`), merged PR history | Keep Artemis activity visible per member |
| Each member can explain their subsystem | Team task | Clear ownership per service; oral-exam prep | Rehearse artefact walkthroughs (not code) |
| Working end-to-end system demonstrated | Partial | Full flow implemented: auth → catalog → watch progress → spoiler-safe chat → GenAI (`services/chat/.../ChatService.java`, `services/genai/.../ask.py`) | Keep a stable tutor-accessible URL live; verify clean boot (see catalog note below) |

## System

| Criterion | Status | Evidence | Action |
|---|---|---|---|
| Functional System | Partial (Good) | End-to-end Q&A path works with spoiler-safe filtering; integration tests incl. spoiler-trap (`QuestionsControllerIntegrationTest`) | Confirm clean local `catalog` boot (a Flyway startup failure was observed locally; run `down -v` to rule out stale DB volume) |
| Architecture Quality | Done (Excellent) | 3 Spring Boot services + Python GenAI + React web-client; OpenAPI-first (`api/openapi.yaml`); Traefik gateway; clearly separated interfaces | — |
| User-Facing Value | Done | React SPA: login, series/episode progress, Ask-a-question workflow returning cited answers | — |

## DevOps & Infrastructure

| Criterion | Status | Evidence | Action |
|---|---|---|---|
| Build and Deployment | Done (Excellent) | `.github/workflows/ci.yml` (lint + test + build + e2e + publish to GHCR); `deploy.yml` (Rancher Helm auto-on-merge); `cd-azure.yml` (Azure VM via Terraform + Ansible, manual) | Keep a live deployment reachable for evaluation |
| Runtime and Observability | Done (Excellent) | Prometheus scrape config; 2 Grafana dashboards as JSON (`tso-overview.json`, `traefik-dashboard.json`); 2 alerts — `ServiceDown` (up==0 for 1m, critical) + `HighErrorRate` (5xx ratio >0.05 for 5m, warning); k8s `ServiceMonitor` + `PrometheusRule` + dashboard ConfigMap | Add GenAI-specific error-rate panel; wire alert notification routing (Alertmanager) |
| Environment and Reproducibility | Done | `docker compose -f infra/docker-compose.yml up --build`; seeded demo user; `infra/env.example` | Document the `down -v` step when the DB volume is stale |

## Engineering Process

| Criterion | Status | Evidence | Action |
|---|---|---|---|
| Testing Strategy | Done | Unit + integration per service (JUnit, pytest incl. GenAI chain/spoiler tests); Playwright e2e in CI; spoiler-trap correctness test | — |
| Engineering Artefacts | Done | `docs/system-architecture.md`, UML diagrams (`docs/diagrams/`), deployment docs | — |
| Documentation | Done | README + `docs/responsibilities.md` (RACI); `docs/observability.md`; requirements-compliance refreshed | Fix stale `infra/k8s/README.md` "not yet included" note (ServiceMonitor/PrometheusRule now exist) |

## Bonus (evidence to claim)

| Bonus area | Status | Evidence / note |
|---|---|---|
| Advanced DevOps | Partial | Self-healing via liveness/readiness probes (`infra/k8s/chart/templates/deployment.yaml`); IaC (Terraform + Ansible); Azure OIDC (no stored secret). Autoscaling/HPA **not** present (README lists it as future work) |
| Advanced Observability | Partial | Dual dashboards, per-service + gateway (Traefik) metrics; no distributed tracing or log aggregation yet |
| Advanced AI | Candidate | Spoiler-safe context filtering (progress-bounded summaries, double-filtered citations). Full RAG / vector DB **not** present (explicitly deferred) |

## Notes on verification (2026-07-05)

- GenAI `/genai/ask` is implemented and calls an LLM via LangChain `ChatOpenAI`
  with a configurable `base_url` (cloud or local), returning a spoiler-filtered
  answer + cited episode indices (`services/genai/src/genai/chain.py`).
- `chat` → `genai` orchestration is live: `ChatService` bounds context to the
  user's watch progress, calls `GenAiClient.postAsk` (`POST /genai/ask`), then
  re-filters citations before persisting history.
- Alert names/thresholds quoted above are taken verbatim from
  `infra/observability/prometheus/alerts.yml`.
