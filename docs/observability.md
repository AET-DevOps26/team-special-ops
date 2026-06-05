# Observability

This document describes the monitoring stack that ships with the local
development environment: Prometheus for metrics collection and alert evaluation,
and Grafana for visualisation. Everything here runs locally via
`infra/docker-compose.yml`; wiring the same metrics endpoints into a Kubernetes
monitoring stack is owned by the deployment workstream.

## Overview

All four backend services expose Prometheus-format metrics. Prometheus scrapes
them on a fixed interval, stores the time series, and evaluates alert rules.
Grafana reads from Prometheus and renders a pre-provisioned dashboard.

Because the metrics are auto-instrumented HTTP metrics (Micrometer on the Spring
side, `prometheus-fastapi-instrumentator` on the FastAPI side), request count,
latency, and error rate are available for every endpoint with no per-handler
code. Panels and alerts for `chat` and `genai` therefore populate automatically
once those features start serving real traffic.

| Component | URL | Config |
|---|---|---|
| Prometheus | http://localhost:9090 | `infra/observability/prometheus/prometheus.yml` |
| Grafana | http://localhost:3001 | `infra/observability/grafana/` |

## Metrics exposed per service

| Service | Port | Endpoint | Instrumentation |
|---|---|---|---|
| user-progress | 8081 | `/actuator/prometheus` | Micrometer + `micrometer-registry-prometheus` |
| catalog | 8082 | `/actuator/prometheus` | Micrometer + `micrometer-registry-prometheus` |
| chat | 8083 | `/actuator/prometheus` | Micrometer + `micrometer-registry-prometheus` |
| genai | 8084 | `/metrics` | `prometheus-fastapi-instrumentator` |

The `user-progress` service permits unauthenticated access to its actuator
endpoints `/actuator/health`, `/actuator/info`, and `/actuator/prometheus` only
— everything else still requires auth.

### Key metric names

**Spring Boot services** — HTTP server metrics via Micrometer:

- `http_server_requests_seconds_count` — request count
- `http_server_requests_seconds_sum` / `http_server_requests_seconds_bucket` —
  latency (histogram, used for p95 / p99)

  Labels: `application`, `method`, `uri`, `status`, `outcome`. Error rate is
  derived from `status` / `outcome` (e.g. 5xx responses).

**genai (FastAPI)**:

- `http_requests_total` — request count
- `http_request_duration_seconds_count` / `http_request_duration_seconds_bucket`
  — latency (histogram, used for p95)

The `up` metric (Prometheus-synthesised per scrape target) reports whether each
service is currently reachable.

## Running and viewing it locally

The observability stack comes up with the rest of the system:

```bash
docker compose -f infra/docker-compose.yml up --build
```

Then:

- **Prometheus** — http://localhost:9090
  - Scrape targets and their health: http://localhost:9090/targets
  - Alert state: http://localhost:9090/alerts
- **Grafana** — http://localhost:3001
  - Default login `admin` / `admin`, overridable via the
    `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` environment variables.
  - Host port defaults to `3001` (avoids colliding with apps on `3000`);
    override with `GRAFANA_PORT`.
  - The Prometheus datasource and the dashboard are auto-provisioned from
    `infra/observability/grafana/`, so no manual setup is required.

## Dashboard

Grafana auto-loads **"TSO — System Overview"**
(`infra/observability/grafana/dashboards/tso-overview.json`) with the following
panels:

- Spring request rate
- Spring 5xx error rate
- Spring latency p95
- Spring latency p99
- Service up/down
- GenAI request rate
- GenAI latency p95

The `chat` and `genai` panels stay flat until those features produce real
traffic, then populate automatically.

## Alert rules

Alert rules live in `infra/observability/prometheus/alerts.yml` and are
evaluated by Prometheus. There is no Alertmanager or notification routing yet —
alerts are visible at http://localhost:9090/alerts but do not page anyone.

| Alert | Condition | Severity | Meaning |
|---|---|---|---|
| `ServiceDown` | `up == 0` for 1m | critical | A scrape target has been unreachable for at least a minute. |
| `HighErrorRate` | Spring 5xx ratio > 5% for 5m | warning | A Spring service is returning 5xx for more than 5% of requests over a 5-minute window. |

## Extending this

- **GenAI latency / SLO alert** — once the Q&A path produces steady traffic, add
  an alert on `http_request_duration_seconds` (e.g. p95 over a threshold) and/or
  a GenAI error-rate alert, mirroring the Spring rules.
- **Notifications** — add Alertmanager so the existing alert rules can route to
  email / Slack / etc., instead of only showing in the Prometheus UI.
- **Kubernetes** — the same `/actuator/prometheus` and `/metrics` endpoints can
  be scraped under k8s (e.g. via Prometheus Operator `ServiceMonitor`s), reusing
  the metric names, dashboard, and alert rules here. This is owned by the
  deployment workstream.
