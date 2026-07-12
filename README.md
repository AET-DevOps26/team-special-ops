# team-special-ops

**SceneIt** — a spoiler-safe TV series Q&A web app that answers questions about a
series using only episodes the viewer has already seen. Built as a DevOps course
project: mono-repo, OpenAPI-first, CI/CD, observability, Kubernetes deployment.

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
| `infra/k8s/` | Helm chart for Rancher/Kubernetes deployment |
| `infra/terraform/`, `infra/ansible/` | Azure VM provisioning and configuration |
| `infra/observability/` | Prometheus, Grafana dashboards, alert rules |
| `docs/` | Architecture, diagrams, runbooks, responsibilities |
| `.github/workflows/ci.yml` | Build + test + lint + e2e + publish images to GHCR |
| `.github/workflows/deploy.yml` | Auto-deploy to Rancher on green CI (`main`) |
| `.github/workflows/cd-azure.yml` | Manual deploy/destroy to Azure VM |

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

## Ask a question (spoiler-safe chat)

1. Copy `infra/env.example` to `infra/.env` and add your `LOGOS_API_KEY`.
2. Start the stack: `docker compose -f infra/docker-compose.yml up --build`
3. Open http://localhost:8080, sign in, open **Stranger Things**, and set your
   current episode on the episode list.
4. Use the **Ask a question** sidebar on the right — answers cite only episodes
   up to your progress.

Quick API check (replace `$TOKEN` and `$SERIES_ID`):

```bash
curl -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"seriesId":"958661e6-226c-5117-9318-5e3265598767","question":"Who is Eleven?"}' \
  http://localhost:8080/api/chat/questions
```

Spoiler trap checks: at progress S1E1, asking about S2 characters should not leak
future plot; cited episode indices must always be ≤ your progress.

## Using it

Open the app at **http://localhost:8080** and sign in with the seeded demo user:

| email | password |
|-------|----------|
| `demo@example.com` | `password123` |

### API Gateway (Traefik)

All traffic goes through **Traefik**, the reverse proxy API gateway running on port 8080:

| Service | URL                                     | Purpose |
|---|-----------------------------------------|---|
| Web app | http://localhost:8080/                  | React SPA |
| User Progress API | http://localhost:8080/api/user-progress | Auth + watch progress |
| Catalog API | http://localhost:8080/api/catalog       | Series + episodes |
| Chat API | http://localhost:8080/api/chat          | Q&A orchestration |
| GenAI API | http://localhost:8080/api/genai         | LLM calls |


Traefik routes `/api/*` paths to the backend services
and `/` to the static web app, all over Docker's internal network—so there's a single origin and no CORS to configure.

### Development & Debugging

The individual service ports are also published for direct access—useful for Swagger UIs and debugging:


| Service | URL                                                    |
|---|--------------------------------------------------------|
| Web app | http://localhost:8080                                  |
| user-progress API docs | http://localhost:8081/swagger-ui.html                  |
| catalog API docs | http://localhost:8082/swagger-ui.html                  |
| chat API docs | http://localhost:8083/swagger-ui.html                  |
| genai API docs | http://localhost:8084/docs                             |
| Postgres | `localhost:5432` (default user/password/db: `tso`)     |
| Prometheus | http://localhost:9090                                  |
| Grafana | http://localhost:3001 (default login `admin` / `admin`) |
| Traefik Dashboard | http://localhost:8000/dashboard/ for Routing & config  |

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

## CI/CD

GitHub Actions workflows in [`.github/workflows/`](./.github/workflows/):

| Workflow | Trigger | What it does |
|---|---|---|
| [`ci.yml`](./.github/workflows/ci.yml) | Every PR + push to `main` | Lint OpenAPI; build + test + lint Java, GenAI, web-client; Playwright e2e; on `main`, publish images to GHCR |
| [`deploy.yml`](./.github/workflows/deploy.yml) | Green CI on `main`; manual; PR label `deploy-staging` | Helm deploy to the course **Rancher** cluster ([`infra/k8s/`](./infra/k8s/)) |
| [`cd-azure.yml`](./.github/workflows/cd-azure.yml) | Manual (`workflow_dispatch`) | Provision or destroy an **Azure VM** (Terraform + Ansible + Docker Compose) |

On merge to `main`, CI publishes `ghcr.io/<org>/tso-*` images (`sha-<commit>` +
`latest`) and `deploy.yml` rolls the SHA tag out to Rancher. Deploy prerequisites
and manual steps: [`infra/k8s/README.md`](./infra/k8s/README.md) (Rancher) and
[azure-handoff](./docs/project-guidelines/azure-handoff.md) (Azure).

## Workflow

- Every change goes through a feature branch and a pull request
- `main` is branch-protected (see [docs/branch-protection.md](./docs/branch-protection.md))
- CI must be green and ≥1 teammate must approve before merge
- API changes start in `api/openapi.yaml`, then run `./api/scripts/gen-all.sh`

## Students responsibilities

Three students; each owns a primary subsystem (server, GenAI, or client) but all
collaborate on integration and deployment. See
[docs/responsibilities.md](./docs/responsibilities.md) for the RACI matrix,
GitHub usernames, and oral-exam artefact pointers.