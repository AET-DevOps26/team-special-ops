# Terraform — Azure infrastructure (Resource Group + AKS)

Provisions the cloud cluster the app runs on, codifying §2 and §4 of
[azure-deployment-plan.md](../../docs/project-guidelines/azure-deployment-plan.md).
Cluster *preparation* and the app *deploy* live in [`../ansible`](../ansible);
this directory only creates the infrastructure.

| File | Purpose |
|---|---|
| `main.tf` | Providers (`azurerm`, `azuread`) + remote `azurerm` state backend |
| `variables.tf` | Region, cluster name, node count/size, namespace, DNS label |
| `aks.tf` | `azurerm_resource_group` + `azurerm_kubernetes_cluster` |
| `outputs.tf` | RG/cluster names, namespace, and the computed ingress FQDN |
| `terraform.tfvars` | Non-secret defaults (committed) |
| `bootstrap.sh` | One-time creation of the remote-state storage container |

No ACR is created: the chart pulls the **public** GHCR images
(`ghcr.io/aet-devops26/tso-*`), so the cluster needs no registry credentials.

## What you get

A `Standard_B2s` × 2 AKS cluster in a single resource group (`rg-tso` by
default) with a system-assigned managed identity. The cluster has no ingress or
TLS yet — Ansible adds those.

## One-time prerequisites (cannot be automated)

1. **Remote-state backend.** Terraform state is shared between your laptop and
   CI, so it lives in an Azure Storage container. Create it once:

   ```bash
   az login
   ./bootstrap.sh           # prints the four backend values + GitHub Secret names
   ```

2. **GitHub OIDC identity for CI.** Register an Azure AD app, add a *federated
   credential* bound to this repo, and grant it **Contributor** on the
   subscription. Then store as GitHub Actions Secrets:

   - `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`
   - `TFSTATE_RESOURCE_GROUP`, `TFSTATE_STORAGE_ACCOUNT`, `TFSTATE_CONTAINER`, `TFSTATE_KEY`
   - `JWT_SECRET`, `POSTGRES_PASSWORD`, `OPENROUTER_API_KEY` (consumed by Ansible)

   No cloud password is stored — `azure/login@v2` exchanges the GitHub OIDC token
   for Azure access at runtime.

## Run it locally

```bash
az login

terraform init \
  -backend-config="resource_group_name=<state-rg>" \
  -backend-config="storage_account_name=<state-sa>" \
  -backend-config="container_name=tfstate" \
  -backend-config="key=tso-azure.tfstate"

terraform plan
terraform apply

terraform output -raw ingress_fqdn   # the tutor-facing host
```

In CI this is driven by [`.github/workflows/cd-azure.yml`](../../.github/workflows/cd-azure.yml).

## Outputs (consumed by Ansible / CD)

- `resource_group_name`, `cluster_name` → `az aks get-credentials`
- `namespace` → Helm release namespace
- `dns_label` → annotation Ansible puts on the ingress public IP
- `ingress_fqdn` → `<dns_label>.<location>.cloudapp.azure.com`, passed to Helm as
  `ingress.host`

## Teardown (stop the charges)

```bash
terraform destroy
```

or run the `cd-azure` workflow with `action=destroy`. The remote-state storage
account from `bootstrap.sh` is intentionally **not** destroyed by Terraform —
delete its resource group manually if you want it gone too.
