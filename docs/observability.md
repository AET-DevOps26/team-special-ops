# Observability

This document describes the monitoring stack: Prometheus for metrics collection and
alert evaluation, and Grafana for visualisation. The **same** stack ships two ways —
locally via `infra/docker-compose.yml`, and self-hosted in the Kubernetes (Rancher)
deployment via the Helm chart (see [On Kubernetes (Rancher)](#on-kubernetes-rancher)).
Both use the same metric names and the same `tso-overview.json` dashboard, so what you
see locally matches the cluster.

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

## On Kubernetes (Rancher)

The Rancher deployment self-hosts the same Prometheus + Grafana inside the
`team-special-ops` namespace via the Helm chart (`infra/k8s/chart`, gated behind
`selfMonitoring.enabled`, on for Rancher). This gives the team a Grafana it can
actually open **without cluster-monitoring access** — reusing the `tso-overview.json`
dashboard and metric names from the local stack.

| Component | Access |
|---|---|
| **Grafana** | `https://team-special-ops.stud.k8s.aet.cit.tum.de/grafana` — anonymous, **view-only**, no login. Open **Dashboards → "TSO — System Overview"**. |
| **Prometheus** | In-cluster only (ClusterIP). Reach it with `kubectl -n team-special-ops port-forward svc/tso-prometheus 9090:9090`. |

Prometheus scrapes the services with **static targets** (no CRDs, no cluster RBAC), so
it runs under a namespace-scoped account. The chart *also* ships a `ServiceMonitor` +
dashboard `ConfigMap` for a cluster-wide Prometheus Operator (Rancher Monitoring), for
whoever has cluster-level access. Deploy details: `infra/k8s/README.md`.

> The app Deployments use `maxSurge: 0` so a rolling update never runs a surge pod that
> would exceed the namespace ResourceQuota (4 cpu / 6 Gi) and wedge `helm upgrade`.

## Extending this

- **GenAI latency / SLO alert** — once the Q&A path produces steady traffic, add
  an alert on `http_request_duration_seconds` (e.g. p95 over a threshold) and/or
  a GenAI error-rate alert, mirroring the Spring rules.
- **Notifications** — add Alertmanager so the existing alert rules can route to
  email / Slack / etc., instead of only showing in the Prometheus UI.
