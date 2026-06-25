# Azure Deployment Plan (Single Linux VM + Docker Compose)

> Step-by-step guide to deploying the system to **Azure** on a **single Linux VM**
> running the same [`docker-compose`](../../infra/docker-compose.yml) stack we use
> locally, fronted by **Traefik** with a Let's Encrypt TLS cert.
>
> The course **Rancher** environment still uses the Helm chart
> ([kubernetes-deployment-plan.md](./kubernetes-deployment-plan.md)) — that path is
> unchanged. Azure is the *cloud* environment, and it deliberately reuses the local
> compose stack instead of the chart.

---

## 0. Why a VM + Compose (not AKS)

The brief requires a cloud deployment with a public HTTPS URL. The local stack
already solves the hard part: [`docker-compose.yml`](../../infra/docker-compose.yml)
runs Traefik routing `/` to the SPA and `/api/*` to every backend on **one origin**,
plus Postgres and observability. Running that *same* compose on a cloud VM gives a
working app with **no `/api` routing gaps** and almost nothing new to learn.

- **One artifact, two runtimes.** Rancher uses the Helm chart; Azure uses the exact
  compose file developers run locally. The only Azure-specific additions are an
  overlay that (a) pulls prebuilt GHCR images instead of building, and (b) turns on
  Traefik TLS — see [`docker-compose.azure.yml`](../../infra/docker-compose.azure.yml).
- **Cheap + simple.** A single `Standard_B2s` VM with a public IP is the minimal
  billable footprint, and teardown is one `terraform destroy`.
- We **do not** use AKS/ACR here: that would fork config into a second Kubernetes
  implementation. The VM keeps the cloud path identical to local dev.

### Azure building blocks we create (all via Terraform)

| Azure resource | Role |
|---|---|
| **Resource Group** (`rg-tso`) | Single teardown/billing boundary |
| **VNet + Subnet** | Network the VM's NIC lives in |
| **Public IP + DNS label** | Stable `*.cloudapp.azure.com` host (the tutor URL) |
| **Network Security Group** | Firewall: allow 22 (admin), 80, 443 |
| **Linux VM** (Ubuntu 22.04) | Runs the docker-compose stack |

```
Internet (https://<dns_label>.<region>.cloudapp.azure.com)
        │
        ▼
  Azure Public IP ──► NSG (22/80/443) ──► VM
                                           │
                                  Traefik :80/:443 (Let's Encrypt)
                                    ├── /        → web-client
                                    └── /api/*   → user-progress / catalog / chat / genai
                                                        │
                                                   postgres (+ volume)
                                  prometheus / grafana (SSH-only, NSG-blocked)
```

---

## 1. Prerequisites (one-time, local)

```bash
az --version          # Azure CLI — https://learn.microsoft.com/cli/azure/install-azure-cli
terraform -version
az login              # authenticate to Azure
az account show       # confirm the right subscription is active
```

An **SSH key pair** authenticates you to the VM (key-only — no passwords):

```bash
ssh-keygen -t ed25519 -f ~/.ssh/tso_azure -C tso-azure
```

The **public** key goes to Terraform (`TF_VAR_admin_ssh_public_key`); the
**private** key is what Ansible/CD connect with (`AZURE_VM_SSH_PRIVATE_KEY` secret).

> **Cost awareness:** the VM and public IP bill while they run. Tear everything
> down (§5) when not demoing. Check for **Azure for Students** credit first.

---

## 2. Provision the VM (Terraform)

Terraform code lives in [`infra/terraform`](../../infra/terraform):
`network.tf` (RG + VNet + public IP + NSG + NIC) and `vm.tf` (the Linux VM).
State is shared via an Azure Storage backend (run `bootstrap.sh` once).

```bash
cd infra/terraform
az login

terraform init \
  -backend-config="resource_group_name=<state-rg>" \
  -backend-config="storage_account_name=<state-sa>" \
  -backend-config="container_name=tfstate" \
  -backend-config="key=tso-azure.tfstate"

export TF_VAR_admin_ssh_public_key="$(cat ~/.ssh/tso_azure.pub)"
terraform apply

terraform output -raw fqdn        # tutor-facing host
terraform output -raw public_ip   # SSH/Ansible target
```

The NSG opens only **22** (locked to `allowed_ssh_cidr`), **80**, and **443**.
Prometheus/Grafana ports are intentionally closed to the internet — reach them over
an SSH tunnel.

---

## 3. Configure the VM + deploy (Ansible)

Ansible code lives in [`infra/ansible`](../../infra/ansible). The playbook runs
over SSH and:

1. Installs **Docker CE + the compose plugin**.
2. Copies `docker-compose.yml`, `docker-compose.azure.yml`, and `observability/`
   to the VM (`/opt/tso`).
3. Templates `.env` (Postgres/JWT/LOGOS secrets + `DOMAIN`, `IMAGE_TAG`,
   `LETSENCRYPT_EMAIL`).
4. Pulls the GHCR images and runs `docker compose up` (no building on the VM).

```bash
cd infra/ansible
ansible-galaxy collection install -r requirements.yml
pip install docker

export JWT_SECRET=... POSTGRES_PASSWORD=... LOGOS_API_KEY=...
ansible-playbook playbook.yml \
  --inventory "$(terraform -chdir=../terraform output -raw public_ip)," \
  --user "$(terraform -chdir=../terraform output -raw admin_username)" \
  --private-key ~/.ssh/tso_azure \
  -e "domain=$(terraform -chdir=../terraform output -raw fqdn)" \
  -e "image_tag=sha-$(git rev-parse --short HEAD)"
```

Traefik then completes the ACME **HTTP-01** challenge on port 80 and serves HTTPS
on 443 for the FQDN, redirecting HTTP → HTTPS automatically.

---

## 4. Cloud CD (GitHub Actions)

[`.github/workflows/cd-azure.yml`](../../.github/workflows/cd-azure.yml) wires the
two steps together on a manual dispatch:

- **Auth with OIDC** (`azure/login@v2`) — no service-principal password stored.
- `terraform apply` (with `TF_VAR_admin_ssh_public_key`) → capture `public_ip`,
  `fqdn`, `admin_username` outputs.
- Write the SSH private key, wait for SSH, then run the Ansible playbook passing
  `domain`/`image_tag` and the app secrets via `env:`.
- A separate, double-gated `destroy` job (`confirm=DESTROY` + the `azure-destroy`
  environment's required reviewer) tears the VM down.

### Required GitHub Secrets

- `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID` (OIDC)
- `TFSTATE_RESOURCE_GROUP`, `TFSTATE_STORAGE_ACCOUNT`, `TFSTATE_CONTAINER`, `TFSTATE_KEY`
- `AZURE_VM_SSH_PUBLIC_KEY` (→ `TF_VAR_admin_ssh_public_key`), `AZURE_VM_SSH_PRIVATE_KEY`
- `JWT_SECRET`, `POSTGRES_PASSWORD`, `LOGOS_API_KEY`

---

## 5. Verify, then tear down (don't burn credit)

**Verify:** open `https://<dns_label>.<region>.cloudapp.azure.com`, log in, and run
the core flow (catalog + chat). Traefik routes `/api/*` to the backends on the same
origin, and the cert is Let's Encrypt-trusted.

**Tear down:**

```bash
terraform -chdir=infra/terraform destroy
```

or run the `cd-azure` workflow with `action=destroy` and `confirm=DESTROY`. This
deletes the VM, disk, NIC, and public IP — **stopping all charges**. The
remote-state storage account is intentionally **not** destroyed; delete its
resource group manually if you want it gone too.

---

## 6. Cost & safety notes

- Prefer **Azure for Students** credit; set a **budget alert** in Cost Management.
- `Standard_B2s` + 1 public IP is the cheap baseline; destroy between demos.
- Never commit the SSH private key, OIDC creds, or the LLM key — all via GitHub
  Secrets / OIDC / the VM's `0600` `.env`, matching the "no hardcoded credentials" rule.
- Tighten `allowed_ssh_cidr` from the `*` demo default to your own IP for anything
  beyond a throwaway environment.
