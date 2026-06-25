# Kubernetes Deployment Plan

> **Status:** plan only — nothing in this document has been executed yet. It is a
> step-by-step, educational guide for taking the current `docker-compose` system
> and making it deploy to **Kubernetes** (course **Rancher** cluster first, then
> **Azure/AKS** — see [azure-deployment-plan.md](./azure-deployment-plan.md)).
>
> **Design goals, in priority order:**
> 1. **One command** brings the whole system up from nothing (graded under
>    *Reproducibility*; also survives Rancher's weekly namespace wipe).
> 2. **One chart, two environments** — the same Helm chart deploys to Rancher and
>    Azure, differing only by a values file.
> 3. **Production-ready** — health probes, resource limits, autoscaling,
>    externalised secrets, persistent storage, observability, automated CD.

---

## 0. What we are trying to achieve (and why)

The course requires (`docs/project-details.md`, *Environment and Deployment*):

- The same system that runs locally via Compose must **also deploy to Kubernetes**.
- It must deploy to **two** environments: the course **Rancher** cluster *and*
  **Azure**.
- Configuration must be **externalised** via env vars + **Secrets** — no hardcoded
  credentials or environment-specific values.
- The database must have **persistent storage** in the deployed setup.
- **CD**: merge to `main` must **automatically deploy** to Kubernetes.
- A **public URL** must let tutors use the running system with no extra setup.

Mapped to grading (`docs/project-grading.md`):

| Grading row | How this plan scores it |
|---|---|
| Build and Deployment (Excellent) | Fully automated CI/CD: build → test → image → deploy |
| Environment and Reproducibility | One Helm command from zero to running; sane defaults |
| Runtime and Observability | Prometheus + Grafana + alert wired into the chart |
| Bonus: Advanced DevOps | HPA autoscaling + self-healing probes + rolling deploys |

### The mental model

Kubernetes runs your containers and *keeps them running the way you declared*.
You describe the desired state as YAML objects; Kubernetes makes reality match and
re-converges after any disruption (crash, node loss, namespace wipe).

```
                         ┌──────────────────────── Kubernetes namespace: tso ──────────────────────┐
   Internet ──► Ingress ─┤                                                                          │
   (public URL)          │   web-client (Deployment)  ── nginx serves SPA + reverse-proxies /api   │
                         │        │  /catalog  /user-progress  /chat  (and /genai internally)       │
                         │        ▼                                                                  │
                         │   user-progress  catalog  chat ──► genai   (Deployments, ClusterIP Svcs) │
                         │        │            │       │                                            │
                         │        └────────────┴───────┴──► postgres (StatefulSet) ──► PVC (disk)   │
                         └──────────────────────────────────────────────────────────────────────────┘
```

Every box is one **Deployment** (or **StatefulSet** for Postgres) fronted by a
**Service** (a stable in-cluster DNS name + virtual IP). Only the web-client is
reachable from outside, via the **Ingress**. This mirrors the Compose topology
*exactly* — the browser still talks to a single origin and nginx proxies the rest —
so behaviour is identical to local, which is the whole point of reproducibility.

---

## 1. Kubernetes concepts you need (5-minute primer)

If these are already familiar, skip to §2.

| Object | What it is | Our use |
|---|---|---|
| **Pod** | Smallest unit; one (or few) containers sharing a network identity | One pod = one running service instance |
| **Deployment** | Declares "run N identical pods of this image"; self-heals, rolling updates | `user-progress`, `catalog`, `chat`, `genai`, `web-client` |
| **StatefulSet** | Like a Deployment but for stateful apps: stable name, stable storage, ordered start | `postgres` |
| **Service** | Stable virtual IP + DNS name load-balancing to a set of pods | In-cluster addressing: `catalog`, `postgres`, … |
| **Ingress** | HTTP(S) router from outside the cluster to a Service | Public entrypoint → `web-client` |
| **ConfigMap** | Non-secret config as key/values, injected as env or files | Service URLs, ports, feature flags |
| **Secret** | Same, but for sensitive values (base64, RBAC-restricted) | DB password, JWT secret, OpenAI key |
| **PersistentVolumeClaim (PVC)** | A request for durable disk that outlives pods | Postgres data dir |
| **Probe** | Liveness/Readiness/Startup health checks | Spring Actuator + genai `/genai/health` |
| **HPA** | HorizontalPodAutoscaler — scales pod count on CPU/metrics | `catalog`, `chat` (bonus) |
| **Namespace** | A logical partition of the cluster | `tso` (app), `monitoring` (Prometheus/Grafana) |

**Helm** sits on top of all this: it is the *package manager* for Kubernetes. A
**chart** is a parameterised bundle of the YAML above; a **values file** supplies the
parameters. `helm upgrade --install` renders the templates with your values and
applies them idempotently.

---

## 2. Pre-work in the application (close these gaps first)

The deployment is only as good as the app it deploys. These are real gaps in the
current repo that must be closed **before** (or alongside) the chart, or the
deployed system will be incomplete. Track each as its own PR.

> ⚠️ Per team rule *discuss-before-scope-creep*: items 2.1–2.3 touch shared service
> code, not just infra. Raise them with the owning subsystem owner before bundling.

### 2.1 Finish the vertical slice (chat → genai)
`services/chat` is still a stub (`application.yml` has no datasource, no genai
client) and `genai/main.py` is only a health endpoint. The deployment can ship
today, but the demo needs the real Q&A flow. This is functionality work, tracked
separately — listed here so the deployment plan doesn't hide it.

### 2.2 Externalise *all* config (no hardcoded hosts)
Compose already injects `SPRING_DATASOURCE_*`. Audit each service so **every**
environment-specific value reads from an env var with a sane local default:

- `services/user-progress/application.yml` hardcodes
  `jdbc:postgresql://localhost:5432/user_progress` and `username: postgres`.
  Compose overrides it via `SPRING_DATASOURCE_URL`, but make the YAML use
  `${SPRING_DATASOURCE_URL:...}` like `catalog` does, so intent is explicit.
- `JWT_SECRET` must come from a **Secret** in-cluster, never the baked default.
- The genai service must read its LLM key + model from env
  (`LOGOS_API_KEY`, `LLM_MODEL`, `LLM_BASE_URL`), and chat must read
  `GENAI_BASE_URL` (in-cluster: `http://genai:8084`).

This directly satisfies *"Hardcoded credentials … are not acceptable."*

### 2.3 Add the Prometheus metrics endpoint
The parent `services/pom.xml` already has `spring-boot-starter-actuator`, but there
is **no Prometheus registry** and no exposure config. For each Spring service:

- Add dependency `io.micrometer:micrometer-registry-prometheus`.
- In `application.yml`:
  ```yaml
  management:
    endpoints.web.exposure.include: health,info,prometheus
    endpoint.health.probes.enabled: true   # exposes /actuator/health/liveness + /readiness
    metrics.tags.application: ${spring.application.name}
  ```
- genai (FastAPI): add `prometheus-fastapi-instrumentator` exposing `/metrics`
  (request count, latency, error rate — the three required metrics).

### 2.4 Complete the web-client reverse proxy
`web-client/nginx.default.conf` proxies only `/catalog/` and `/user-progress/`. Add
`location /chat/` (→ `chat:8083`) so the SPA keeps a single origin in-cluster too.
(genai stays internal — only chat calls it.) Keep the `resolver` + variable
`proxy_pass` pattern that already exists; it works the same with Kubernetes DNS.

### 2.5 Health endpoints to standardise
- Spring: `/actuator/health/liveness` and `/actuator/health/readiness` (from 2.3).
- genai: `/genai/health` already exists — reuse it for both probes for now.
- Postgres: use a `pg_isready` exec probe (same as the Compose healthcheck).

---

## 3. Container images: build, tag, publish

Kubernetes does **not** build images — it pulls pre-built images from a **registry**.
(Compose builds locally; that's the key difference.) We'll publish to **GitHub
Container Registry (GHCR)** because the repo is already on GitHub and `GITHUB_TOKEN`
can push with no extra account.

**Five images**, built from the existing Dockerfiles (no Docker changes needed):

| Image | Dockerfile | Build args |
|---|---|---|
| `ghcr.io/<org>/tso-user-progress` | `services/Dockerfile.spring` | `SERVICE=user-progress JAVA_PKG=userprogress APP_PORT=8081` |
| `ghcr.io/<org>/tso-catalog` | `services/Dockerfile.spring` | `SERVICE=catalog JAVA_PKG=catalog APP_PORT=8082` |
| `ghcr.io/<org>/tso-chat` | `services/Dockerfile.spring` | `SERVICE=chat JAVA_PKG=chat APP_PORT=8083` |
| `ghcr.io/<org>/tso-genai` | `services/genai/Dockerfile` | — |
| `ghcr.io/<org>/tso-web-client` | `web-client/Dockerfile` | — |

**Tagging strategy (traceability — a best-practices requirement):** tag every
image with **both** the git commit SHA *and* a moving tag:
- `:sha-<short-sha>` — immutable, exact provenance (the chart deploys *this*).
- `:main` / `:latest` — convenience pointer to newest `main`.

Deploying by **SHA** (not `latest`) is what makes a deploy reproducible and
rollback-able: the chart's `values` records the exact image that is running.

Postgres uses the upstream `postgres:16-alpine` image unchanged.

---

## 4. The Helm chart (one chart, two environments)

Proposed layout (lives in the repo so it's versioned with the code it deploys):

```
infra/k8s/
├── chart/
│   ├── Chart.yaml                 # name: tso, version, appVersion
│   ├── values.yaml                # defaults (sane, mirror Compose)
│   ├── values-rancher.yaml        # course cluster overrides
│   ├── values-azure.yaml          # AKS overrides (see Azure plan)
│   └── templates/
│       ├── _helpers.tpl           # naming/label helpers
│       ├── configmap.yaml         # non-secret env (service URLs, ports)
│       ├── secret.yaml            # DB pwd, JWT secret, OpenAI key (from values/CI)
│       ├── postgres-statefulset.yaml
│       ├── postgres-service.yaml  # headless ClusterIP
│       ├── postgres-pvc.yaml      # (or volumeClaimTemplate in the StatefulSet)
│       ├── deployment.yaml        # templated once, ranged over .Values.services
│       ├── service.yaml           # one ClusterIP per service
│       ├── ingress.yaml           # web-client → public host
│       ├── hpa.yaml               # optional autoscalers
│       └── servicemonitor.yaml    # Prometheus scrape config (see §7)
├── deploy.sh                      # the "one command" wrapper (see §6)
└── README.md                      # how to deploy + troubleshoot
```

### 4.1 `values.yaml` — the single source of knobs

```yaml
image:
  registry: ghcr.io/<org>
  tag: ""                 # set by CD to sha-<short-sha>; empty = chart appVersion
  pullPolicy: IfNotPresent

# One block per service → templates/deployment.yaml ranges over this.
services:
  user-progress: { port: 8081, replicas: 1, db: true }
  catalog:       { port: 8082, replicas: 1, db: true }
  chat:          { port: 8083, replicas: 2, db: true }   # scaled: it fans out to genai
  genai:         { port: 8084, replicas: 1, db: false }
  web-client:    { port: 80,   replicas: 2, db: false }

postgres:
  image: postgres:16-alpine
  db: tso
  user: tso
  storage: 5Gi
  storageClass: ""        # "" = cluster default (works on Rancher and AKS)

ingress:
  enabled: true
  className: nginx
  host: tso.<your-rancher-domain>   # overridden per env
  tls: false                        # true on Azure (cert-manager)

# Secrets: NEVER commit real values. Defaults are dev-only; CD injects real ones.
secrets:
  jwtSecret: "dev-only-change-me-min-32-characters-long"
  postgresPassword: "tso"
  logosApiKey: ""

resources:                # production-readiness: requests + limits on every pod
  default:
    requests: { cpu: 100m, memory: 256Mi }
    limits:   { cpu: 500m, memory: 512Mi }

autoscaling:
  enabled: false          # turned on in values-rancher/azure for catalog+chat
```

### 4.2 What each template encodes (and the "why")

- **`deployment.yaml`** (one template, ranged over `.Values.services`): image =
  `{{ registry }}/tso-{{ name }}:{{ tag }}`; env from the ConfigMap + Secret;
  **liveness/readiness probes**; **resource requests/limits**;
  `securityContext` non-root (the Dockerfiles already run as UID 1000). DB-backed
  services additionally get the `SPRING_DATASOURCE_*` + JWT env. This is *self-healing*:
  a crashed/unready pod is restarted/removed from rotation automatically.
- **`service.yaml`**: a `ClusterIP` per service so `chat` reaches genai at
  `http://genai:8084` and every service reaches Postgres at `postgres:5432` — by
  DNS name, exactly like Compose service names.
- **`postgres-statefulset.yaml` + PVC**: the database with a `volumeClaimTemplate`
  → durable disk. **This is the persistence requirement.** Pod can die/reschedule;
  data stays. (See §5 for the Flyway angle.)
- **`ingress.yaml`**: routes the public host to `web-client:80`. The single public
  door. On Azure it also terminates TLS (Azure plan §6).
- **`secret.yaml` / `configmap.yaml`**: split sensitive vs non-sensitive config —
  the externalised-config requirement, made concrete.
- **`hpa.yaml`**: optional CPU-based autoscaling for `catalog`/`chat` — the
  *Advanced DevOps* bonus.

---

## 5. The database & the "wiped cluster" problem (critical)

Rancher deletes your namespace ~weekly. A namespace delete also deletes the
**PVC**, so Postgres comes back **empty**. "One command" therefore must also
**recreate schema and seed data automatically** — otherwise the tutor opens a blank
app. Good news: **the repo already does this**, via **Flyway**.

On startup each Spring service runs its Flyway migrations against the DB:

```
services/user-progress/.../db/migration/
  V1__create_users_table.sql
  V2__seed_demo_user.sql        → demo@example.com / password123
  V3__create_progress_table.sql
services/catalog/.../db/migration/
  V1__create_catalog_schema.sql
  V2__seed_stranger_things.sql  → the catalog content
```

So the boot sequence after a wipe is:

```
helm upgrade --install …  →  Postgres pod starts (empty PVC)
                          →  Spring services start, Flyway runs V1..Vn
                          →  schema created + demo user + catalog seeded
                          →  app is immediately usable, demo login works
```

**No manual seed step. No "restore the DB" step.** That is precisely the property
that makes the system reproducible and wipe-proof, and it's already in place. Two
operational notes baked into the chart:

1. **Ordering / readiness:** services must wait for Postgres. We rely on Spring's
   retry + readiness probes (a service that can't reach the DB stays *not ready* and
   the Ingress won't route to it) rather than brittle init-ordering. Optionally add
   an init-container that blocks on `pg_isready`.
2. **Shared DB, separate Flyway histories:** Compose already shows
   `user-progress` uses `SPRING_FLYWAY_TABLE=flyway_schema_history_user_progress`
   + baseline so its migrations don't collide with `catalog`'s in the shared `tso`
   database. The chart's ConfigMap must carry the **same** env so behaviour is
   identical in-cluster.

---

## 6. The one-command deploy (the headline feature)

The idempotent core command:

```bash
helm upgrade --install tso ./infra/k8s/chart \
  -n tso --create-namespace \
  -f ./infra/k8s/chart/values-rancher.yaml \
  --set image.tag="sha-$(git rev-parse --short HEAD)"
```

- `upgrade --install` = install if absent, converge if present. **Run the same line
  on a fresh cluster or to redeploy** — no delete-first dance.
- `--create-namespace` = even on a freshly-wiped Rancher, the namespace is recreated.
- One chart pulls in *everything*: Postgres, all 5 services, Ingress, config, secrets.

Wrapped into a literal one-liner (`infra/k8s/deploy.sh`):

```bash
./infra/k8s/deploy.sh rancher      # or:  ./infra/k8s/deploy.sh azure
```

The script (sketch) does, in order:
1. Select kube-context / verify `kubectl` reachability.
2. Create/ensure secrets (from env or a `.env` not in git — never committed).
3. `helm upgrade --install` the app chart with the right values file.
4. (Optional) ensure the monitoring stack (§7) is installed.
5. `kubectl rollout status` on each Deployment → exits non-zero if anything is
   unhealthy, so "one command" also *verifies* itself.

> After a weekly Rancher wipe, recovery is literally: `./infra/k8s/deploy.sh rancher`.
> This is the single most important sentence to be able to say in the demo.

---

## 7. Observability (Prometheus + Grafana + alert)

Required: Prometheus (request count, latency, error rate), Grafana dashboards
exported as `.json`, and ≥1 meaningful alert.

**Approach — install the `kube-prometheus-stack` Helm chart** into a `monitoring`
namespace (bundles Prometheus + Grafana + Alertmanager in one shot), then point it
at our services:

1. **Expose metrics** (done in §2.3): Spring → `/actuator/prometheus`, genai →
   `/metrics`.
2. **Scrape config** via a `ServiceMonitor` per service (templated in the chart),
   telling Prometheus which Service + path + port to scrape.
3. **Dashboards:** build one Grafana dashboard showing, per service: request rate,
   p95 latency, error rate (4xx/5xx), plus genai latency. **Export it as
   `.json`** into `infra/monitoring/dashboards/` (a deliverable). Auto-load it via a
   ConfigMap with the `grafana_dashboard: "1"` label so it appears on every fresh
   deploy (reproducible, not hand-clicked).
4. **Alert rule** (a `PrometheusRule`): e.g. *GenAI service down* (`up{job="genai"}==0`
   for 1m) **or** *p95 latency > 8s*. Meaningful = tied to real failure, per the
   brief's "don't just install monitoring" warning.

Store all of it in-repo (`infra/monitoring/`) so it deploys with one command too.

---

## 8. Continuous Deployment (merge to `main` → live)

Extend `.github/workflows/ci.yml` (or add `cd.yml`) with a `deploy` job that runs
**only on push to `main`, after CI passes**:

```
on push main → [CI: build+test+lint, already exists]
            → build-and-push: build all 5 images, tag :sha-<sha> + :main, push to GHCR
            → deploy-rancher:  configure kubeconfig (from secret) → helm upgrade --install
            → smoke-test:      curl the public URL / health endpoints; fail the job if down
```

Key practices (graded under CI/CD *correct use of secrets*):
- **No hardcoded tokens.** Registry + kubeconfig + JWT/OpenAI come from **GitHub
  Actions Secrets**, injected at run time.
- **Deploy by SHA**, so `main` and the cluster are always in provable lockstep.
- **Gate on CI green** — never deploy a red build (the brief's "fake CI/CD" pitfall).
- Rancher access: store the cluster's kubeconfig (or a scoped service-account token)
  as a GitHub Secret; the job writes it to `$KUBECONFIG` then runs `helm`.

The same workflow, with a different values file + cluster credentials, deploys to
Azure — see the Azure plan §7.

---

## 9. Production-readiness checklist

These are what move the *DevOps & Infrastructure* rows from "Basic" to "Excellent"
and unlock the bonus. Each maps to a concrete chart field:

- [ ] **Liveness + readiness probes** on every pod (no traffic to unready pods).
- [ ] **Resource requests + limits** on every container (scheduling + stability).
- [ ] **Non-root containers** (Dockerfiles already UID 1000 — assert via `securityContext`).
- [ ] **HPA** on `catalog`/`chat` (CPU target ~70%) — *autoscaling bonus*.
- [ ] **PersistentVolumeClaim** for Postgres — durable storage.
- [ ] **Externalised secrets** — `JWT_SECRET`, DB password, OpenAI key via Secret.
- [ ] **Rolling updates** (default Deployment strategy) → zero-downtime redeploys.
- [ ] **`PodDisruptionBudget`** (optional) so voluntary disruptions keep ≥1 pod.
- [ ] **`imagePullPolicy` + SHA tags** → deterministic, rollback-able rollouts.
- [ ] **`helm rollback tso`** documented → instant revert to the previous release.
- [ ] **ServiceMonitor + PrometheusRule + dashboard JSON** in-repo.
- [ ] **One-command deploy verified from a clean namespace** (test it ≥twice).

---

## 10. Verification (prove it works, don't assume)

After `./infra/k8s/deploy.sh rancher`:

```bash
kubectl -n tso get pods                 # all Running/Ready
kubectl -n tso rollout status deploy    # all rolled out
kubectl -n tso get ingress              # note the address/host
# open the public URL → log in as demo@example.com / password123 → run the core flow
kubectl -n monitoring port-forward svc/grafana 3000:80   # dashboards show live traffic
```

Then the **wipe drill** (the one that mirrors Rancher reality): delete the namespace,
re-run the one command, confirm the app is fully usable again with seeded data. If
that passes from a clean slate twice in a row, the *Reproducibility* requirement is
genuinely met — not just claimed.

---

## 11. Suggested execution order (PR by PR)

1. **App pre-work** (§2): config externalisation, Prometheus registry, `/chat` proxy.
   *(Coordinate 2.1–2.3 with subsystem owners.)*
2. **Image publishing** (§3): CI job builds + pushes all 5 images to GHCR by SHA.
3. **Helm chart, Postgres + services + Ingress** (§4–5): deploy to Rancher manually;
   pass the wipe drill.
4. **One-command wrapper** (§6) + README runbook.
5. **Observability** (§7): kube-prometheus-stack, ServiceMonitors, dashboard JSON, alert.
6. **CD** (§8): wire deploy-on-merge to Rancher.
7. **Production-readiness pass** (§9): probes, limits, HPA, rollback docs.
8. **Azure** — see [azure-deployment-plan.md](./azure-deployment-plan.md): same chart,
   `values-azure.yaml`, AKS + ACR + TLS + CD.

Ship 3 early (deploy something real fast — the brief's #1 lesson is *make it
deployable early and iterate*), then layer the rest. Don't leave deployment for the
final week; that's the documented #1 way these projects fail.
