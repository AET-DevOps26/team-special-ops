# team-special-ops

Spoiler-safe TV series Q&A — a web app that answers questions about a series using
only episodes the viewer has already seen. Built as a DevOps course project:
mono-repo, OpenAPI-first, CI/CD, observability, Kubernetes deployment.

## Structure

| Path | What |
|---|---|
| `api/openapi.yaml` | Single source of truth — OpenAPI 3.1 spec |
| `api/scripts/gen-all.sh` | Regenerates typed code (Java stubs, Python client, TS types) from the spec |
| `services/user-progress/` | Spring Boot — auth + watch progress |
| `services/catalog/` | Spring Boot — series + episodes |
| `services/chat/` | Spring Boot — Q&A orchestration |
| `services/genai/` | FastAPI + LangChain — LLM calls |
| `web-client/` | React + Vite + TS + Tailwind |
| `infra/docker-compose.yml` | Local stack: Postgres + all services + web client |
| `docs/` | Architecture, diagrams, runbooks |
| `.github/workflows/ci.yml` | Build + test + lint per service |

## Run the full stack

Requires [Docker](https://docs.docker.com/get-docker/) with Compose v2. From the repo root:

```bash
# build images and start everything
docker compose -f infra/docker-compose.yml up --build

# stop and remove the containers
docker compose -f infra/docker-compose.yml down

# stop, and also wipe the database volume (fresh DB on next start)
docker compose -f infra/docker-compose.yml down -v
```

First startup builds all images and can take several minutes.

Optional — the Postgres credentials and JWT settings have dev-only defaults
(user/password/db all `tso`, port `5432`). To override them, copy
`infra/env.example` to `infra/.env` (gitignored), edit it, and append
`--env-file infra/.env` to the commands above.

## Using it

Open the app at **http://localhost:8080** and sign in with the seeded demo user:

| email | password |
|-------|----------|
| `demo@example.com` | `password123` |

The browser only ever talks to `:8080`. A reverse proxy there serves the web app
and forwards API calls to the backend services over Docker's internal network, so
there's a single origin and no CORS to configure.

The individual service ports are also published for development — handy for
Swagger UIs, `curl`, and debugging:

| Service | URL |
|---|---|
| Web app | http://localhost:8080 |
| user-progress API docs | http://localhost:8081/swagger-ui.html |
| catalog API docs | http://localhost:8082/swagger-ui.html |
| chat API docs | http://localhost:8083/swagger-ui.html |
| genai API docs | http://localhost:8084/docs |
| Postgres | `localhost:5432` (default user/password/db: `tso`) |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 (default login `admin` / `admin`) |

## Monitoring & observability

The local stack ships with a Prometheus + Grafana setup that comes up alongside
the services via the same `docker compose` command.

- **Prometheus** — http://localhost:9090 — scrapes all four backend services and
  evaluates alert rules.
- **Grafana** — http://localhost:3001 — default login `admin` / `admin`
  (override with `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD`; change the host
  port with `GRAFANA_PORT`). The datasource and dashboard are auto-provisioned.

Each service exposes Prometheus metrics out of the box — the three Spring Boot
services at `/actuator/prometheus` (Micrometer) and the genai service at
`/metrics`. From these we track **request count, latency (histograms), and error
rate** for every HTTP endpoint without any per-handler code.

Grafana auto-loads the **"TSO — System Overview"** dashboard, with panels for
Spring request rate, Spring 5xx error rate, Spring latency p95 & p99, service
up/down, GenAI request rate, and GenAI latency p95.

Two alert rules are defined in Prometheus (no notification routing yet):

- **ServiceDown** — `up == 0` for 1m (critical).
- **HighErrorRate** — Spring 5xx ratio > 5% for 5m (warning).

Configuration lives under `infra/observability/`:
`prometheus/prometheus.yml` (scrape config), `prometheus/alerts.yml` (alert
rules), and `grafana/` (datasource + dashboard provisioning, including
`grafana/dashboards/tso-overview.json`). See
[docs/observability.md](./docs/observability.md) for the operator guide.

## Architecture

See [docs/system-architecture.md](./docs/system-architecture.md) and
[docs/diagrams/](./docs/diagrams/).

## Workflow

- Every change goes through a feature branch and a pull request
- `main` is branch-protected (see [docs/branch-protection.md](./docs/branch-protection.md))
- CI must be green and ≥1 teammate must approve before merge
- API changes start in `api/openapi.yaml`, then run `./api/scripts/gen-all.sh`
