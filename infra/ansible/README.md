# Ansible — AKS cluster prep + app deploy

Takes the bare AKS cluster from [`../terraform`](../terraform) and makes it serve
the app over HTTPS, codifying §5–§7 of
[azure-deployment-plan.md](../../docs/project-guidelines/azure-deployment-plan.md).

The playbook runs against `localhost` and talks to the cluster through the
kubeconfig that `az aks get-credentials` writes — there are no SSH hosts.

| File | Purpose |
|---|---|
| `requirements.yml` | Galaxy collections (`kubernetes.core`, `community.general`) |
| `inventory.ini` | Just `localhost` (local connection) |
| `group_vars/all.yml` | Non-secret knobs (namespace, host, chart path, versions) |
| `playbook.yml` | The ordered deploy: ingress-nginx → cert-manager → ClusterIssuer → Helm |
| `templates/clusterissuer.yaml.j2` | Let's Encrypt `ClusterIssuer` the chart references |
| `ansible.cfg` | Inventory + sensible defaults |

## What the playbook does (in order)

1. Installs **ingress-nginx** (Helm), annotating its Service with
   `azure-dns-label-name=<dns_label>` so the Azure public IP gets the stable
   `<dns_label>.<location>.cloudapp.azure.com` host.
2. Installs **cert-manager** (Helm, `crds.enabled=true`).
3. Applies the **`letsencrypt-prod` ClusterIssuer** the chart's Ingress
   references.
4. Deploys the **existing chart** ([`../k8s/chart`](../k8s/chart)) with
   `values-azure.yaml`, passing `image.tag`, `ingress.host`, and the secrets
   through `--set` — never from a committed file.

## Secrets

Read from the environment (never committed):

- `JWT_SECRET`, `POSTGRES_PASSWORD`, `OPENROUTER_API_KEY`

In CD these come from GitHub Secrets; locally, export them in your shell.

> **Known gap:** the chart wires the provided key into `OPENAI_API_KEY`, but the
> genai service expects `OPENROUTER_API_KEY`. Spoiler-safe chat won't work on the
> cluster until a small chart change injects `OPENROUTER_API_KEY` into genai —
> see the plan's "known gap" note.

## Run it locally

```bash
# 1. cluster must already exist (terraform apply) and creds be loaded:
az aks get-credentials -g rg-tso -n aks-tso

# 2. install collections once:
ansible-galaxy collection install -r requirements.yml
pip install kubernetes        # the kubernetes.core modules need the python client

# 3. deploy (override vars to match your Terraform outputs):
export JWT_SECRET=... POSTGRES_PASSWORD=... OPENROUTER_API_KEY=...
ansible-playbook playbook.yml \
  -e ingress_host="$(terraform -chdir=../terraform output -raw ingress_fqdn)" \
  -e dns_label="$(terraform -chdir=../terraform output -raw dns_label)" \
  -e image_tag="sha-$(git rev-parse --short HEAD)"
```

In CI this is driven by [`.github/workflows/cd-azure.yml`](../../.github/workflows/cd-azure.yml),
which passes the Terraform outputs in automatically.
