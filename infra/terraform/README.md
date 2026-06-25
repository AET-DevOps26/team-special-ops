# Terraform — Azure infrastructure (Resource Group + Linux VM)

Provisions the cloud host the app runs on: a single Linux VM that runs the
existing [`docker-compose`](../docker-compose.yml) stack (Traefik + services +
Postgres + observability). VM *configuration* and the app *deploy* live in
[`../ansible`](../ansible); this directory only creates the infrastructure.

| File | Purpose |
|---|---|
| `main.tf` | Providers (`azurerm`, `azuread`) + remote `azurerm` state backend |
| `variables.tf` | Region, RG name, VM size, admin user/SSH key, allowed SSH CIDR, DNS label |
| `network.tf` | RG + VNet/subnet + static public IP (DNS label) + NSG (22/80/443) + NIC |
| `vm.tf` | `azurerm_linux_virtual_machine` (Ubuntu 22.04, SSH-key auth) |
| `outputs.tf` | `public_ip`, `fqdn`, `admin_username` (consumed by Ansible/CD) |
| `terraform.tfvars` | Non-secret defaults (committed) |
| `bootstrap.sh` | One-time creation of the remote-state storage container |

No registry is created: the VM pulls the **public** GHCR images
(`ghcr.io/aet-devops26/tso-*`) directly, so no registry credentials are needed.

## What you get

A `Standard_B2s` Ubuntu 22.04 VM in a single resource group (`rg-tso` by default)
with a static public IP and a DNS label, behind an NSG that allows only SSH
(port 22, from `allowed_ssh_cidr`), HTTP (80), and HTTPS (443). The VM is bare
until Ansible installs Docker and brings the compose stack up; Traefik then
terminates TLS with a Let's Encrypt cert for the FQDN.

## One-time prerequisites (cannot be automated)

1. **SSH key pair.** Auth to the VM is key-only. Generate one if needed:

   ```bash
   ssh-keygen -t ed25519 -f ~/.ssh/tso_azure -C tso-azure
   ```

   Pass the **public** key to Terraform (`TF_VAR_admin_ssh_public_key` or a
   `*.auto.tfvars` file) and keep the **private** key for Ansible/CD (stored as the
   `AZURE_VM_SSH_PRIVATE_KEY` GitHub Secret).

2. **Remote-state backend.** Terraform state is shared between your laptop and
   CI, so it lives in an Azure Storage container. Create it once:

   ```bash
   az login
   ./bootstrap.sh           # prints the four backend values + GitHub Secret names
   ```

   `bootstrap.sh` derives a **stable** storage account name from your subscription
   when `STATE_SA` is unset (safe to re-run). Store the printed
   `TFSTATE_STORAGE_ACCOUNT` in GitHub Secrets and reuse it on every run.

3. **GitHub OIDC identity for CI.** Register an Azure AD app, add a *federated
   credential* bound to this repo, and grant it **Contributor** on the
   subscription. Then store as GitHub Actions Secrets:

   - `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`
   - `TFSTATE_RESOURCE_GROUP`, `TFSTATE_STORAGE_ACCOUNT`, `TFSTATE_CONTAINER`, `TFSTATE_KEY`
   - `AZURE_VM_SSH_PUBLIC_KEY` (the VM's authorised key, passed to Terraform as `TF_VAR_admin_ssh_public_key`)
   - `AZURE_VM_SSH_PRIVATE_KEY` (the matching private key Ansible/CD connect with)
   - `JWT_SECRET`, `POSTGRES_PASSWORD`, `LOGOS_API_KEY` (consumed by Ansible -> `.env`)

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

export TF_VAR_admin_ssh_public_key="$(cat ~/.ssh/tso_azure.pub)"
terraform plan
terraform apply

terraform output -raw fqdn         # the tutor-facing host
terraform output -raw public_ip    # SSH/Ansible target
```

In CI this is driven by [`.github/workflows/cd-azure.yml`](../../.github/workflows/cd-azure.yml).

## Outputs (consumed by Ansible / CD)

- `public_ip` → SSH/Ansible inventory host
- `fqdn` → `<dns_label>.<location>.cloudapp.azure.com`, passed to the compose stack
  as `DOMAIN` (Traefik routing + the Let's Encrypt cert)
- `admin_username` → SSH user

## Teardown (stop the charges)

```bash
terraform destroy
```

or run the `cd-azure` workflow with `action=destroy` and `confirm=DESTROY` (waits
for approval via the `azure-destroy` GitHub Environment). The remote-state storage
account from `bootstrap.sh` is intentionally **not** destroyed by Terraform —
delete its resource group manually if you want it gone too.
