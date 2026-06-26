# Azure Deployment (VM + Docker Compose)

> **What this is:** the runbook for deploying Team Special Ops to **Azure** as a
> single **Ubuntu VM running Docker Compose**, provisioned with **Terraform** and
> configured with **Ansible**. Infrastructure-as-code + config-management +
> manual CD, reusing the existing `infra/docker-compose.yml`, observability
> config, and the public GHCR images CI already publishes.
>
> The course **Rancher** cluster remains the Kubernetes / auto-CD path
> (`infra/k8s/`, [kubernetes-deployment-plan.md](./kubernetes-deployment-plan.md)).
> This document is the *cloud* deployment and is independent of it.

---

## 0. Why a VM + Compose (and not AKS)

The brief asks for a cloud deployment reachable at a public HTTPS URL. We use an
Azure **VM + Docker Compose** rather than AKS because:

- **One artifact, reused.** The VM runs the *same* `infra/docker-compose.yml` we
  already run locally, plus a thin production overlay
  (`infra/docker-compose.azure.yml`). No second implementation of the stack, no
  Helm chart to keep in sync for the cloud.
- **Cheap and simple for a course demo.** A single `Standard_B2s` burstable VM is
  far cheaper and quicker to reason about than a managed control plane + node
  pool, and it bills predictably while it runs.
- **Real IaC + config management.** We still demonstrate the graded DevOps
  building blocks — Terraform for infrastructure, Ansible for configuration,
  remote state, OIDC-based CD, TLS — just targeting a VM instead of a cluster.

This is a deliberate trade-off, documented as such: **Rancher is the Kubernetes
story; Azure is the IaC-provisioned VM story.** Kubernetes-on-Azure (AKS) was
explicitly declined to avoid forking the deployment config.

### Building blocks

| Layer | Tool | Files |
|---|---|---|
| Infrastructure | Terraform (`azurerm`) | `infra/terraform/` |
| Config management | Ansible (`community.docker`) | `infra/ansible/` |
| Runtime | Docker Compose | `infra/docker-compose.yml` + `infra/docker-compose.azure.yml` |
| TLS / routing | Traefik + Let's Encrypt (ACME HTTP-01) | overlay `traefik` service |
| Images | Public GHCR (`ghcr.io/aet-devops26/tso-*`) | published by CI |
| CD | GitHub Actions (`workflow_dispatch`, OIDC) | `.github/workflows/cd-azure.yml` |

```
Internet ── https://tso-special-ops.westeurope.cloudapp.azure.com
   │  (Azure NSG: only 22 / 80 / 443 open)
   ▼
Azure static Public IP ──► VM (Ubuntu 22.04, Standard_B2s)
                              └─ Docker Compose:
                                 Traefik (:443 TLS, :80→:443 redirect)
                                   ├─► web-client  (catch-all Host)
                                   ├─► /api/user-progress
                                   ├─► /api/catalog
                                   ├─► /api/chat
                                   └─► /api/genai
                                 postgres · prometheus · grafana
```

---

## 1. Architecture on the VM

- **Terraform** creates a resource group (`rg-tso`), a VNet/subnet, a **static
  public IP with a DNS label**, an NSG (opens only 22/80/443), a NIC, and an
  **Ubuntu 22.04 gen2 VM** (`Standard_B2s`, 64 GB disk, SSH-key-only auth).
  State is stored **remotely** in Azure Storage so CD and laptops share it.
- **Ansible** installs Docker CE + the compose plugin, ships the compose files
  and `observability/` config, renders `/opt/tso/.env` from secrets, and runs
  `docker compose up` with **`pull: always`, `build: never`** (images come from
  GHCR — nothing is built on the VM).
- **Traefik** terminates TLS on `:443` using a Let's Encrypt cert obtained via
  the ACME HTTP-01 challenge (resolver `le`), redirects HTTP→HTTPS, and routes by
  `Host(${DOMAIN})`: the web client is the catch-all and the four APIs keep their
  `/api/*` prefixes.
- **Prometheus (9090) and Grafana (3001) stay closed** at the NSG — reach them
  over an SSH tunnel (§7).

---

## 2. One-time prerequisites

Done once per project/subscription.

### 2.1 Tooling (local, only if deploying by hand)

```bash
az --version           # Azure CLI
terraform -version     # >= 1.5
ansible --version
az login
az account set --subscription "<subscription-id>"
```

### 2.2 Terraform remote-state backend

State lives in an Azure Storage container so CD and any laptop share one locked
state. Create it once:

```bash
cd infra/terraform
./bootstrap.sh         # creates rg-tso-tfstate + storage account + container
```

It prints the `TFSTATE_*` values to use for `terraform init` and the GitHub
secrets below.

### 2.3 SSH key for the VM

```bash
ssh-keygen -t ed25519 -f ~/.ssh/tso_azure -C tso-azure
# public half  → Terraform (TF_VAR_admin_ssh_public_key / AZURE_VM_SSH_PUBLIC_KEY)
# private half → Ansible / CD (AZURE_VM_SSH_PRIVATE_KEY)
```

### 2.4 Azure OIDC app for CD

Register an Azure AD application with a **federated credential** for this GitHub
repo (no client secret stored anywhere), and grant it `Contributor` on the
subscription (or `rg-tso` + the tfstate RG). This lets `azure/login@v2` exchange
the GitHub OIDC token for Azure access — the secrets-light pattern.

### 2.5 GitHub secrets

| Secret | Purpose |
|---|---|
| `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID` | OIDC login |
| `AZURE_VM_SSH_PUBLIC_KEY`, `AZURE_VM_SSH_PRIVATE_KEY` | VM SSH key pair |
| `JWT_SECRET`, `POSTGRES_PASSWORD`, `LOGOS_API_KEY` | app secrets (rendered into `.env`) |
| `TFSTATE_RESOURCE_GROUP`, `TFSTATE_STORAGE_ACCOUNT`, `TFSTATE_CONTAINER`, `TFSTATE_KEY` | remote-state coordinates |

Create two **GitHub Environments**: `azure` (deploy) and `azure-destroy` (add a
required reviewer so teardown needs an explicit approval).

---

## 3. Deploy (CD — recommended)

Actions → **Deploy to Azure VM** → *Run workflow*:

- `action`: `deploy`
- `image_tag`: `latest` (or a pinned `sha-<short>`)

The workflow:

1. logs into Azure via OIDC,
2. `terraform init` (remote backend) + `terraform apply` (creates/updates the VM),
3. captures `public_ip` / `fqdn`,
4. waits for SSH, then runs the Ansible playbook (installs Docker, brings the
   stack up from GHCR),
5. runs an **HTTPS smoke test** against `https://<fqdn>`,
6. writes the URL to the run summary.

Because the VM bills while running, this is **manual** (`workflow_dispatch`), not
auto-on-merge — auto-CD stays on the Rancher path.

---

## 4. Deploy (manual / local equivalent)

```bash
# Terraform
cd infra/terraform
export TF_VAR_admin_ssh_public_key="$(cat ~/.ssh/tso_azure.pub)"
terraform init \
  -backend-config="resource_group_name=$TFSTATE_RESOURCE_GROUP" \
  -backend-config="storage_account_name=$TFSTATE_STORAGE_ACCOUNT" \
  -backend-config="container_name=$TFSTATE_CONTAINER" \
  -backend-config="key=$TFSTATE_KEY"
terraform apply

# Ansible
cd ../ansible
ansible-galaxy collection install -r requirements.yml
export JWT_SECRET=… POSTGRES_PASSWORD=… LOGOS_API_KEY=…
ansible-playbook -i "$(terraform -chdir=../terraform output -raw public_ip)," playbook.yml \
  -u azureuser --private-key ~/.ssh/tso_azure \
  -e domain="$(terraform -chdir=../terraform output -raw fqdn)" \
  -e image_tag=latest
```

---

## 5. Verify

```bash
# From the run summary or:
terraform -chdir=infra/terraform output fqdn

curl -I https://tso-special-ops.westeurope.cloudapp.azure.com
# → 200, valid Let's Encrypt cert
# open it → log in demo@example.com / password123 → run the core flow
```

If the first request fails with a TLS error, give Traefik a minute — the ACME
HTTP-01 challenge issues the cert on first request.

---

## 6. Teardown (stop billing)

Actions → **Deploy to Azure VM** → `action: destroy`, type `DESTROY` in
`confirm`. The `azure-destroy` environment requires a reviewer's approval, then
`terraform destroy` deletes `rg-tso` and everything in it.

Locally: `cd infra/terraform && terraform destroy`.

> The tfstate storage account (separate RG, created by `bootstrap.sh`) is left
> intact so the next deploy still has its state. Delete it by hand for truly zero
> cost.

---

## 7. Observability access

Prometheus and Grafana are **not** exposed publicly (NSG closes 9090/3001). Reach
them through an SSH tunnel:

```bash
ssh -i ~/.ssh/tso_azure \
  -L 9090:localhost:9090 -L 3001:localhost:3001 \
  azureuser@<public-ip>
# then open http://localhost:9090 and http://localhost:3001
```

The Prometheus config and Grafana dashboards are the same ones used locally
(`infra/observability/`), copied to the VM by Ansible.

---

## 8. Cost & safety notes

- A `Standard_B2s` VM + static public IP bill while running — **destroy after a
  demo** (§6). Prefer Azure for Students credit; set a Cost Management budget.
- SSH defaults to open (`allowed_ssh_cidr = "*"`); tighten it to your IP in
  `infra/terraform/terraform.tfvars` for anything long-lived.
- No credentials are committed: Azure access is via OIDC, app secrets via GitHub
  Secrets → `.env` (gitignored), state in Azure Storage, SSH keys never in git.

---

## 9. At a glance

```bash
# one-time
infra/terraform/bootstrap.sh          # remote state backend
# (register OIDC app, add GitHub secrets + environments, generate SSH key)

# deploy / destroy (CD)
Actions → "Deploy to Azure VM" → deploy   (image_tag = latest)
Actions → "Deploy to Azure VM" → destroy  (confirm = DESTROY)
```

Provision once with Terraform, configure with Ansible, reachable at one HTTPS
URL, reproducible from scratch, and tear down to (near) zero cost — the same
compose stack that runs locally.
