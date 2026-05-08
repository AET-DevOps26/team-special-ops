# Initial System Structure & First Product Backlog

> **MVP scope note:** This document reflects the **v0 MVP**: prompt-stuffing approach, no RAG, no vector database. The chat flow fetches all episode summaries up to the user's progress from Postgres and includes them directly in the LLM prompt. RAG with a vector DB (Weaviate) is **explicitly deferred to a later phase** and is the planned bonus addition. The MVP architecture is designed so that addition is a clean extension, not a rewrite.

## 1. Initial System Structure

### Subsystem overview

| Subsystem | Technology | Responsibility |
|---|---|---|
| **Client** | React (Vite) + Tailwind | Web UI: auth, show selection, progress, chat, citation display |
| **Server** | Spring Boot (Java 21) | Three microservices exposing REST APIs (see below) |
| **GenAI Service** | Python 3+ FastAPI + LangChain | Receives question + allowed episode summaries, formats prompt, calls LLM, returns answer. *Vector retrieval added in later phase.* |
| **Database** | PostgreSQL | Persistent storage for users, progress, shows, episodes (with summaries), questions, answers, citations |
| **External LLM** | OpenAI API (cloud) / GPT4All | Generation backend, configurable via env var |

### Server-side microservice split (3 microservices)

- **User & Progress Service** — authentication, user accounts, watch progress per show
- **Catalog Service** — shows, episodes (with per-episode summaries), metadata
- **Chat Service** — receives questions, fetches allowed episode summaries, calls GenAI service, persists Q&A history

---

### 1.1 Analysis Object Model (UML Class Diagram)
https://apollon.aet.cit.tum.de/c64402a9-5121-48ed-8227-2556defd8516?view=COLLABORATE

![UML Diagram](./diagrams/Class%20Diagram.png)

*Episode carries a `summary` text field directly. The `cites` many-to-many association records which episodes' summaries informed a given answer; in v0 this means "every episode whose summary was bundled into the LLM prompt." If we later need per-citation metadata (relevance score, ordering), we can promote `cites` to an association class then.*

*On progress storage: `currentEpisodeIndex` (a global episode index counted from 1 across the whole show) is the position used for ordering and filtering. `currentSeasonDisplay` and `currentEpisodeDisplay` are human readable fields written alongside it, used only for UI rendering (so the progress label doesn't require a JOIN to Episode on every read). On ChatQuestion, only `progressAtAsk` (the global index snapshot) is stored. Display fields can be derived via JOIN at read time, since questions are read less frequently than progress. Using a single integer for ordering also means the safety filter `episode_index ≤ progress` is one comparison in v0 SQL and translates directly to vector-DB metadata filtering when RAG is added.*

---

### 1.2 Use Case Diagram
https://apollon.aet.cit.tum.de/576d2a13-3291-4a6f-92ca-62f99afbd548?view=COLLABORATE

![Use Case Diagram](./diagrams/Use%20Case%20Diagram.png)

---

### 1.3 Component Diagram (Top-level Architecture)

https://apollon.aet.cit.tum.de/b0b5e19f-b63a-4df2-a5e4-7b46dd9238bb?view=COLLABORATE
[Component Diagram](./diagrams/Component%20Diagram.png)

Vector DB (Weaviate) sitting next to the GenAI Service,
storing chunked content with metadata. Will be added when RAG is introduced.

**Components and interfaces for Apollon:**

| Component | Provides interface | Requires interface |
|---|---|---|
| React Frontend | (UI to user) | REST: `/auth`, `/progress`, `/catalog`, `/chat` |
| User & Progress Service | REST: `/auth`, `/progress` | JDBC to PostgreSQL |
| Catalog Service | REST: `/catalog` | JDBC to PostgreSQL |
| Chat Service | REST: `/chat` | JDBC to PostgreSQL, REST to GenAI |
| GenAI Service | REST: `/ask` | HTTPS to LLM Provider |
| PostgreSQL | JDBC | — |
| LLM Provider | HTTPS / local socket | — |

---

## 2. First Product Backlog (MVP — 10 items)

Each item below assumes its **own definition of done** includes: tests in CI, lint clean, OpenAPI/Swagger updated where relevant, and the relevant section of the README updated. We track these as expectations on every item, not as separate backlog items.

| # | Title | Description | Owner |
|---|---|---|---|
| 1 | **Repo + CI foundation** | Mono-repo with subfolders for client, server, genai, infra, docs. `main` branch protected (PR + 1 review + green CI required). GitHub Actions runs build + test + lint per service on every PR. | Shared |
| 2 | **Local docker-compose stack** | All services + Postgres boot via `docker compose up`. Sane defaults; ≤3 commands from clone to running system. | Shared |
| 3 | **User auth** | Signup, login, JWT issuance, auth middleware on protected endpoints. Lives in **User & Progress Service**. | Server |
| 4 | **Show catalog + seed data** | Postgres schema for `Show` and `Episode` (with `summary` field). Seed script loads chosen show. `GET /catalog/shows`, `GET /catalog/shows/{id}/episodes`. Lives in **Catalog Service**. | Server |
| 5 | **Watch progress** | `GET /progress`, `PUT /progress` for current user. Lives in **User & Progress Service**. | Server |
| 6 | **Chat endpoint** | `POST /chat/questions`: validates user + progress, fetches all episode summaries with `globalIndex ≤ progress` from Postgres, forwards question + summaries to GenAI service, persists question + answer with its cited episodes. Lives in **Chat Service**. | Server |
| 7 | **GenAI service `/ask`** | FastAPI + LangChain. `POST /ask` accepts `{ question, allowed_summaries[] }`, formats a prompt (LangChain prompt template), calls LLM, returns `{ answer, cited_episode_indices[] }`. LLM backend is config-switchable between OpenAI (cloud) and a local model. | GenAI |
| 8 | **Frontend MVP** | React app covering: signup/login, show selection, progress slider, chat with answer + citation display. Vitest tests on core flows. | Client |
| 9 | **Observability** | Prometheus metrics on all services (request count, latency, error rate). One Grafana dashboard, exported as `.json` in repo. One meaningful alert rule (e.g., GenAI service down or p95 latency > 8s). | Shared |
| 10 | **Kubernetes deployment + CD** | Helm chart deploys the full stack. Deploys cleanly to both Rancher and Azure (separate values files). Merge to `main` triggers automatic deployment. Secrets via Kubernetes Secrets. | Shared |

---

## What's deferred (post-MVP)

These belong on the backlog *after* the 10 items above are green:

- **RAG with vector DB.** Introduce Weaviate, embeddings, chunked content store, retrieval-time metadata filter. Replaces prompt-stuffing for scale.
- **LLM-assisted ingestion.** Enrich raw content with entity / topic / first-revealed metadata.
- **Agentic routing.** Multi-source retrieval (plot vs. character vs. world).
- **Self-RAG critique step.** Post-generation safety pass; exports a `spoiler_leakage_rate` metric.
- **Question history view.** Browseable past Q&A on the frontend.
- **Multiple shows.**

## Notes for the team

- **Item 6 + 7 form the vertical slice.** Get them connected end-to-end with stub data as early as possible — the brief warns that late integration is the most common failure mode for these projects.
- **The metadata-based safety guarantee works without RAG.** The filter `globalIndex ≤ progress` happens in our SQL query (Item 6). Spoiler-safety is preserved in v0; RAG is added later for scale and answer quality, not for safety.
- **Tests, OpenAPI/Swagger updates, lint, and README updates are part of each item's DoD** — not separate items. Don't merge an item that doesn't carry its own quality gates.
- **The "spoiler trap-question suite"** (curated questions about future-episode content asserted not to leak) should be built up as a test artifact starting from Item 6. It is our central correctness evidence for the demo and oral exam.