# Kubernetes deployment (Helm)

One Helm chart deploys the whole stack — Postgres + the four services + the web
client — to the course **Rancher** Kubernetes cluster.

> **Azure is a different path.** The Azure target is a single **VM running Docker
> Compose**, provisioned with **Terraform + Ansible** — *not* Helm/Kubernetes. See
> `docs/project-guidelines/azure-deployment-plan.md` and `infra/terraform/`,
> `infra/ansible/`, `infra/docker-compose.azure.yml`. This Helm chart targets
> Rancher only.

See `docs/project-guidelines/kubernetes-deployment-plan.md` for the full rationale.

## One-command deploy

```bash
# from the repo root, with kubectl pointed at the target cluster
./infra/k8s/deploy.sh rancher
```

This installs (or upgrades) everything, waits for rollout, and prints pod status.
It is idempotent — run it on a freshly-wiped namespace or to redeploy; `helm
upgrade --install` converges either way.

Under the hood it runs:

```bash
helm upgrade --install tso ./infra/k8s/chart \
  -n team-special-ops --create-namespace \
  -f ./infra/k8s/chart/values-rancher.yaml \
  --set image.tag="sha-$(git rev-parse --short HEAD)"
```

> Namespace `team-special-ops` lives inside the team's Rancher Project. It already
> exists, so `--create-namespace` is a no-op; it's kept only so the command also
> works from a truly empty cluster.

## Prerequisites (one-time)

1. **kubeconfig** pointed at the cluster (`kubectl get nodes` works).
2. **Images published & public** — CI publishes `ghcr.io/aet-devops26/tso-*` on
   merge to `main`. They must be public (or set `image.pullSecret`).
3. **Ingress controller** present (Rancher ships ingress-nginx). Set the real
   hostname in `chart/values-rancher.yaml`.
4. **Secrets** in your environment (or a gitignored `infra/k8s/.env`):
   `JWT_SECRET`, `POSTGRES_PASSWORD`, optionally `LOGOS_API_KEY`.

## The wiped-cluster story

Rancher deletes the namespace ~weekly, taking the Postgres PVC with it. Recovery
is just re-running the one command: the namespace is recreated, and **Flyway
re-creates the schema and seeds the demo user + catalog on service startup** — no
manual DB restore. That is the whole point of the design.

## Layout

```
infra/k8s/
├── chart/
│   ├── Chart.yaml
│   ├── values.yaml            # defaults (mirror docker-compose)
│   ├── values-rancher.yaml    # course cluster overrides
│   └── templates/
│       ├── _helpers.tpl
│       ├── configmap.yaml     # non-secret datasource wiring
│       ├── secret.yaml        # DB password, JWT secret, OpenAI key
│       ├── postgres.yaml      # StatefulSet + PVC + headless Service
│       ├── deployment.yaml    # one Deployment per service (ranged)
│       ├── service.yaml       # one ClusterIP per service (ranged)
│       └── ingress.yaml       # public entry -> web-client
├── deploy.sh                  # the one-command wrapper
└── README.md
```

## Verify / wipe drill

```bash
kubectl -n team-special-ops get pods                       # all Running/Ready
kubectl -n team-special-ops get ingress                    # note the host
# open the host, log in as demo@example.com / password123, run the core flow

# wipe drill (mirrors Rancher reality — empty DB, then one-command recovery):
helm uninstall tso -n team-special-ops
kubectl -n team-special-ops delete pvc -l app.kubernetes.io/name=postgres
./infra/k8s/deploy.sh rancher                 # back from zero, schema + seed via Flyway
```

## Not yet included (later PRs)

- HorizontalPodAutoscalers
- ServiceMonitor / PrometheusRule for kube-prometheus-stack

> The Azure cloud deployment is **not** an AKS/Helm variant of this chart — it is
> a Terraform + Ansible + Docker Compose VM (see `azure-deployment-plan.md`), so
> there is no `values-azure.yaml`.
