# Student Responsibilities & Contributions

This document maps each team member to their primary subsystem and key
contributions, traceable to GitHub commits and pull requests.

## Primary subsystem ownership

| Subsystem | Owner | Description |
|---|---|---|
| DevOps & Infrastructure | Atharva Mathapati | CI/CD pipelines, Kubernetes/Helm deployment, observability (Prometheus + Grafana), TLS, ingress, catalog service, watch-progress feature, frontend screens |
| GenAI Service & Frontend | Tejash Varsani | Python FastAPI GenAI service, LLM integration, frontend auth UI, docker-compose stack, chat service, Azure deployment |
| Backend Services | Leo Pahl | Spring Boot user-progress service (auth, JWT), initial backend architecture, Traefik API gateway |

Subsystem ownership does not mean isolated work — all three members collaborated
on integration, debugging, and cross-cutting concerns.

---

## RACI Matrix

| Task | Atharva | Tejash | Leo |
|---|---|---|---|
| Repo setup + CI foundation | R/A | C | C |
| Local docker-compose stack | C | R/A | C |
| User auth (Spring Boot) | C | C | R/A |
| Catalog service (series + episodes) | R/A | C | C |
| Watch progress service | R/A | C | C |
| Chat service (Q&A orchestration) | C | R/A | C |
| GenAI service (`/ask`, LangChain, LLM) | C | R/A | C |
| Frontend (React, pages, components) | R/A | R/A | C |
| OpenAPI spec + code generation | R/A | C | C |
| Observability (Prometheus, Grafana, alerts) | R/A | C | C |
| Kubernetes Helm chart | R/A | C | C |
| CD pipeline (auto-deploy to Rancher) | R/A | C | C |
| TLS + cert-manager | R/A | C | C |
| Azure deployment | C | R/A | C |

**R** = Responsible (does the work) · **A** = Accountable (owns the outcome) · **C** = Consulted

