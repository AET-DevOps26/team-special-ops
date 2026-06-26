# Terraform — Azure VM infrastructure

Provisions the Azure infrastructure that hosts the Team Special Ops stack as a
**single Ubuntu VM running Docker Compose** (see
`docs/project-guidelines/azure-deployment-plan.md` for the why). Configuration
management (Docker install + bringing the stack up) is handled separately by
`infra/ansible/`.

## What it creates

| Resource | Purpose |
|---|---|
| `azurerm_resource_group` (`rg-tso`) | One container for everything → teardown is one command |
| `azurerm_virtual_network` + `azurerm_subnet` | `10.0.0.0/16` network, `10.0.1.0/24` subnet |
| `azurerm_public_ip` (static, DNS label) | Stable `tso-special-ops.westeurope.cloudapp.azure.com` |
| `azurerm_network_security_group` | Allows 22 (locked down), 80, 443; 9090/3001 stay closed |
| `azurerm_network_interface` (+ NSG assoc.) | VM networking |
| `azurerm_linux_virtual_machine` (`vm-tso`) | Ubuntu 22.04 gen2, `Standard_B2s`, 64 GB disk, SSH-key auth |

## State

State lives **remotely** in an Azure Storage container (so CD and laptops share
it and lock against each other). The backend block in `main.tf` is intentionally
empty — supply coordinates at `init` time.

## One-time backend bootstrap

```bash
az login
az account set --subscription "<subscription-id>"
./bootstrap.sh            # creates the tfstate RG + storage account + container
```

It prints the `TFSTATE_*` values to feed back into `terraform init` (and the CD
GitHub secrets).

## Local apply

```bash
# 1. SSH key the VM will trust (no default; never committed):
export TF_VAR_admin_ssh_public_key="$(cat ~/.ssh/tso_azure.pub)"

# 2. Point Terraform at the remote backend created by bootstrap.sh:
terraform init \
  -backend-config="resource_group_name=$TFSTATE_RESOURCE_GROUP" \
  -backend-config="storage_account_name=$TFSTATE_STORAGE_ACCOUNT" \
  -backend-config="container_name=$TFSTATE_CONTAINER" \
  -backend-config="key=$TFSTATE_KEY"

# 3. Review + apply:
terraform plan
terraform apply

# 4. Read the outputs (the Ansible inventory uses these):
terraform output public_ip
terraform output fqdn
```

## Validation without Azure

```bash
terraform fmt -check
terraform init -backend=false   # skip remote backend
terraform validate
```

## Teardown

```bash
terraform destroy
```

Deletes the resource group and everything in it — charges stop. The remote-state
storage account (created by `bootstrap.sh`, in a *separate* RG) is left intact so
the next `apply` still has its state; delete it manually if you want zero cost.

## Variables worth knowing

| Variable | Default | Notes |
|---|---|---|
| `location` | `westeurope` | Region for all resources |
| `vm_size` | `Standard_B2s` | Cheap burstable; bump for more headroom |
| `admin_username` | `azureuser` | SSH user |
| `allowed_ssh_cidr` | `*` | **Tighten to your IP** to restrict SSH |
| `dns_label` | `tso-special-ops` | Public FQDN prefix |
| `admin_ssh_public_key` | — | Required, via `TF_VAR_admin_ssh_public_key` |
