# Azure Deployment Plan (AKS)

> **Status:** plan only — nothing here has been executed. This is an educational,
> step-by-step guide to deploying the system to **Azure** using **AKS** (Azure
> Kubernetes Service), reusing the *same Helm chart* from
> [kubernetes-deployment-plan.md](./kubernetes-deployment-plan.md).
>
> **Read the Kubernetes plan first.** This document only covers what is *different*
> on Azure: provisioning the cloud cluster, the cloud registry, public DNS + HTTPS,
> and cloud CD. The application, chart, Postgres-in-cluster, Flyway seeding, and
> observability are all identical — that reuse is the entire point.

---

## 0. Why Azure, and why AKS specifically

The brief requires deployment to **two** Kubernetes environments: the course
**Rancher** cluster and **one cloud — Azure**. We want to write our deployment
*once* and run it in both places.

- **AKS** is Azure's *managed Kubernetes*. Azure runs and patches the control plane
  (the cluster "brain") **for free**; you pay only for the worker VMs that run your
  pods. What you get back is a **normal Kubernetes cluster** driven by the same
  `kubectl` + `helm` + the same chart you already use on Rancher.
- That means **one chart, two value files**: `values-rancher.yaml` and
  `values-azure.yaml`. The cloud deploy is *not* a second implementation — it's the
  same artifact with different knobs (registry, hostname, TLS on, autoscaling on).
- We deliberately **do not** use Azure Container Apps / App Service: those hide
  Kubernetes, so they would not satisfy "deployable to **Kubernetes** on Azure" and
  would fork our config. AKS keeps everything aligned and is what the requirement asks for.

### Azure building blocks we'll create

| Azure resource | What it is | Role here |
|---|---|---|
| **Resource Group** | A named container for all resources (easy teardown) | `rg-tso` holds everything |
| **Azure Container Registry (ACR)** | Private Docker image registry | AKS pulls our 5 images from here |
| **AKS cluster** | Managed Kubernetes | Runs the whole stack via Helm |
| **Ingress controller** (ingress-nginx) | HTTP router inside the cluster | Public entry → web-client |
| **Public IP + DNS label** | Stable address + `*.cloudapp.azure.com` name | The tutor-facing URL |
| **cert-manager + Let's Encrypt** | Automatic TLS certificates | HTTPS on the public URL |

```
Internet (https://tso-xxxx.<region>.cloudapp.azure.com)
        │
        ▼
  Azure Public IP ──► ingress-nginx ──► web-client Svc ──► [ same in-cluster stack
                                                             as Rancher: services +
                                                             postgres StatefulSet + PVC ]
  AKS pulls images ◄── ACR (ghcr mirror / push target)
```

---

## 1. Prerequisites (one-time, local)

Things you install/verify once on the machine doing the setup:

```bash
az --version          # Azure CLI — install: https://learn.microsoft.com/cli/azure/install-azure-cli
kubectl version --client
helm version
az login              # opens a browser; authenticates you to Azure
az account show       # confirm the correct subscription is active
# If you have several subscriptions:
az account set --subscription "<subscription-id-or-name>"
```

> **Cost awareness (read this):** AKS worker VMs and the public IP cost money while
> they run. Use a **small node pool** (1–2 × `Standard_B2s`/`B2ms`) for a course
> project, and **tear everything down** when not demoing (§9). Students often have
> **Azure for Students** credit — check that first.

We'll reuse these shell variables throughout (set them once):

```bash
export RG=rg-tso
export LOC=westeurope            # pick a region near you/the tutors
export ACR=tsoacr$RANDOM         # ACR names are global + alphanumeric only
export AKS=aks-tso
export NS=tso
```

---

## 2. Create the resource group

Everything lives inside one resource group so cleanup is a single command later.

```bash
az group create --name $RG --location $LOC
```

*Why:* a resource group is a billing + lifecycle boundary. Delete it (§9) and every
resource inside vanishes — no orphaned cost. This is the cloud equivalent of
`docker compose down`.

---

## 3. Create the container registry (ACR) and push images

AKS pulls images from a registry it can authenticate to. The cleanest cloud-native
choice is **ACR**, attached to AKS so no registry passwords are needed in-cluster.

```bash
az acr create --resource-group $RG --name $ACR --sku Basic
az acr login --name $ACR        # lets your local Docker push to it
```

**Get our 5 images into ACR.** Two options:

- **Build in CI, push to ACR** (recommended for CD): the GitHub Actions workflow
  builds the images (it already knows the Dockerfiles + build args — see K8s plan §3)
  and pushes them to `${ACR}.azurecr.io/tso-*:sha-<sha>`.
- **Or build locally** for a first manual deploy:
  ```bash
  az acr build -r $ACR -t tso-catalog:dev \
    -f services/Dockerfile.spring \
    --build-arg SERVICE=catalog --build-arg JAVA_PKG=catalog --build-arg APP_PORT=8082 .
  # …repeat for user-progress, chat (Dockerfile.spring), genai, web-client…
  ```
  `az acr build` builds *in the cloud* (no local Docker needed) and stores the result
  in ACR in one step — handy and educational.

*Why ACR over public GHCR for the cloud path:* attaching ACR to AKS (next step) gives
the cluster pull access via Azure identity — **zero image-pull secrets to manage**,
which is exactly the "no hardcoded credentials" posture the brief wants. (Rancher can
keep using GHCR; the chart's `image.registry` value is per-environment.)

---

## 4. Create the AKS cluster

```bash
az aks create \
  --resource-group $RG \
  --name $AKS \
  --node-count 2 \
  --node-vm-size Standard_B2s \
  --attach-acr $ACR \
  --enable-managed-identity \
  --generate-ssh-keys
```

What each flag means (this is the educational core of the cloud step):

- `--node-count 2` — two worker VMs. Enough to run the stack with headroom and to
  *demonstrate self-healing* (kill a pod; it reschedules onto the other node).
- `--node-vm-size Standard_B2s` — small/cheap burstable VM. Right-size for a demo.
- `--attach-acr $ACR` — grants the cluster pull rights to our registry via managed
  identity. **This is why we needed ACR**: no secrets, automatic auth.
- `--enable-managed-identity` — the cluster gets an Azure identity instead of a
  password/service-principal secret. More secure, less to leak.

Then point `kubectl`/`helm` at the new cluster:

```bash
az aks get-credentials --resource-group $RG --name $AKS
kubectl get nodes        # should list 2 Ready nodes
```

`get-credentials` merges the AKS cluster into your kubeconfig and switches context.
**From this line on, every `kubectl`/`helm` command targets AKS** — the exact same
commands you run against Rancher. That symmetry is the payoff.

---

## 5. Install the ingress controller (public entry point)

AKS has no public HTTP router by default. We install **ingress-nginx**, which prov
isions an **Azure Public IP** and routes inbound traffic to our `web-client`
Service (driven by the chart's `Ingress` object — no app change).

```bash
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update
helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx --create-namespace

# Wait for Azure to assign the public IP, then read it:
kubectl -n ingress-nginx get svc ingress-nginx-controller -w
```

*Why ingress-nginx (not Azure's AGIC):* it's identical to what runs on Rancher, so
the chart's `ingress.className: nginx` works unchanged in both environments. One less
thing that differs between clouds.

### 5.1 Give it a stable DNS name

Azure can attach a free DNS label to that public IP so the tutor gets a real URL
(not a bare IP). Set the label on the public IP (or via the controller's
`service.beta.kubernetes.io/azure-dns-label-name` annotation), yielding:

```
tso-<label>.<region>.cloudapp.azure.com
```

Put that hostname into `values-azure.yaml` under `ingress.host`. (If the team owns a
custom domain, point a CNAME at it instead — optional.)

---

## 6. HTTPS with cert-manager + Let's Encrypt

A public URL should be `https://`. **cert-manager** automatically obtains and renews
free **Let's Encrypt** certificates — no manual cert handling.

```bash
helm repo add jetstack https://charts.jetstack.io
helm repo update
helm upgrade --install cert-manager jetstack/cert-manager \
  --namespace cert-manager --create-namespace \
  --set crds.enabled=true
```

Then add a `ClusterIssuer` (Let's Encrypt) and set, in `values-azure.yaml`:

```yaml
ingress:
  enabled: true
  className: nginx
  host: tso-<label>.<region>.cloudapp.azure.com
  tls: true
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
```

On deploy, cert-manager sees the Ingress, completes the ACME HTTP-01 challenge
(routed through ingress-nginx), and installs the cert. The URL is now HTTPS, renewed
automatically. *Why it matters:* a credible, tutor-facing public URL is part of the
*"deployed instance available via URL"* requirement, and TLS is table stakes.

---

## 7. Deploy the app — the same one command

Everything above was **one-time cloud infrastructure**. The actual app deploy is the
*same idempotent Helm command* as Rancher, only the values file and image registry
differ:

```bash
helm upgrade --install tso ./infra/k8s/chart \
  -n $NS --create-namespace \
  -f ./infra/k8s/chart/values-azure.yaml \
  --set image.registry="${ACR}.azurecr.io" \
  --set image.tag="sha-$(git rev-parse --short HEAD)"
```

…or, wrapped: `./infra/k8s/deploy.sh azure`.

What `values-azure.yaml` overrides vs the defaults:

```yaml
image:
  registry: <acr-name>.azurecr.io     # pulls from ACR instead of GHCR
ingress:
  host: tso-<label>.<region>.cloudapp.azure.com
  tls: true
autoscaling:
  enabled: true                        # show the autoscaling bonus on real cloud
postgres:
  storageClass: managed-csi            # Azure managed disks back the PVC
```

**Postgres is still in-cluster** (StatefulSet + PVC), same as Rancher — the PVC is
simply backed by an **Azure managed disk** via the `managed-csi` storage class, so it
survives pod restarts. **Flyway still seeds schema + demo data on first boot**
(K8s plan §5), so a fresh AKS cluster becomes a fully usable, logged-in-able app from
this one command. No DB import step. *(If you later want managed Azure Postgres for
backups/HA, it's a `values-azure.yaml` swap — a good oral-exam talking point, not
needed for the requirement.)*

### 7.1 Cloud CD (merge to `main` → live on Azure)

The GitHub Actions deploy job (K8s plan §8) gets an Azure variant:

- **Auth with OIDC federated credentials** (no stored secrets): register an Azure AD
  app, federate it to the GitHub repo, then `azure/login@v2` exchanges the GitHub
  OIDC token for Azure access — **no service-principal password in GitHub**. This is
  the modern, secrets-light pattern and a strong thing to demo.
- Steps: `az login` (OIDC) → `az acr build`/push images by SHA →
  `az aks get-credentials` → `helm upgrade --install … -f values-azure.yaml` →
  smoke-test the public HTTPS URL.

---

## 8. Observability on Azure (same as Rancher)

Install the **same `kube-prometheus-stack`** into the `monitoring` namespace; the
**same** ServiceMonitors, dashboard JSON, and alert rule from the chart apply
unchanged (K8s plan §7). Optionally expose Grafana through its own Ingress host for a
live cloud dashboard in the demo. *(Azure Monitor / Managed Prometheus exists and is
a nice bonus mention, but reusing kube-prometheus-stack keeps both environments
identical and the dashboards portable — which is what's graded.)*

---

## 9. Verify, then tear down (don't burn credit)

**Verify:**

```bash
kubectl -n $NS get pods                 # all Running/Ready
kubectl -n $NS get ingress              # shows the HTTPS host
# Open https://tso-<label>.<region>.cloudapp.azure.com
#   → log in demo@example.com / password123 → run the core flow
```

Demo the cloud-only wins: kill a pod (`kubectl delete pod …`) and watch it
self-heal; generate load and watch the HPA add pods.

**Tear down when done** (everything is in one resource group):

```bash
az group delete --name $RG --yes --no-wait
```

This deletes AKS, ACR, disks, public IP — **stopping all charges**. Because the whole
setup is one Helm command + a handful of `az` commands, **recreating it before the
next demo costs minutes**, not a rebuild. Keep the `az` commands in
`infra/k8s/azure-setup.md` (or a script) so any teammate can stand the cloud up from
scratch.

---

## 10. Cost & safety notes

- Prefer **Azure for Students** credit; set a **budget alert** in Cost Management.
- `Standard_B2s` × 2 + 1 public IP is the cheap baseline; scale down/off between demos.
- Never commit ACR creds, kubeconfig, or the OpenAI key — all via GitHub Secrets /
  OIDC / in-cluster Secrets, matching the brief's "no hardcoded credentials" rule.
- `az aks stop --name $AKS -g $RG` pauses the cluster (stops VM charges) without
  deleting it — cheaper than recreating if you'll demo again soon.

---

## 11. Cloud setup at a glance (the runbook)

```bash
# one-time infra
az group create -n $RG -l $LOC
az acr create -g $RG -n $ACR --sku Basic
az aks create -g $RG -n $AKS --node-count 2 --node-vm-size Standard_B2s \
  --attach-acr $ACR --enable-managed-identity --generate-ssh-keys
az aks get-credentials -g $RG -n $AKS
helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx -n ingress-nginx --create-namespace
helm upgrade --install cert-manager jetstack/cert-manager -n cert-manager --create-namespace --set crds.enabled=true
# (apply ClusterIssuer + DNS label)

# every deploy (also what CD runs)
./infra/k8s/deploy.sh azure          # = helm upgrade --install … -f values-azure.yaml

# teardown
az group delete -n $RG --yes --no-wait
```

That's the whole cloud story: **provision once, deploy with one command, reachable at
one HTTPS URL, reproducible from scratch, and tear down to zero cost** — the same
chart that runs on Rancher.
