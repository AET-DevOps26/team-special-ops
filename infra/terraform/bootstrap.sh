#!/usr/bin/env bash
#
# One-time remote-state bootstrap for the Azure Terraform stack.
#
# Terraform needs somewhere shared to keep its state so the CI runner and your
# laptop don't fork into two divergent state files. That "somewhere" is an Azure
# Storage container — but it can't be created by the same Terraform that uses it
# (chicken/egg), so we create it once here with the Azure CLI.
#
# Run this ONCE per environment, before the first `terraform init`:
#
#   az login
#   ./infra/terraform/bootstrap.sh
#
# It prints the four backend values; pass them to `terraform init` (or set them
# as the CD workflow's TFSTATE_* secrets). Re-running is safe: every command is
# idempotent (`create` is a no-op if the resource already exists).

set -euo pipefail

# --- knobs (override via env) -------------------------------------------------
LOCATION="${LOCATION:-westeurope}"
STATE_RG="${STATE_RG:-rg-tfstate}"
# Storage account names are GLOBAL and must be 3-24 lowercase alphanumerics.
# Default appends a short random suffix so first run doesn't collide; pin it via
# env once chosen so re-runs reuse the same account.
STATE_SA="${STATE_SA:-tsotfstate$RANDOM}"
STATE_CONTAINER="${STATE_CONTAINER:-tfstate}"
STATE_KEY="${STATE_KEY:-tso-azure.tfstate}"
# -----------------------------------------------------------------------------

echo ">> Ensuring resource group '$STATE_RG' in '$LOCATION'"
az group create --name "$STATE_RG" --location "$LOCATION" --output none

echo ">> Ensuring storage account '$STATE_SA'"
az storage account create \
  --name "$STATE_SA" \
  --resource-group "$STATE_RG" \
  --location "$LOCATION" \
  --sku Standard_LRS \
  --encryption-services blob \
  --min-tls-version TLS1_2 \
  --allow-blob-public-access false \
  --output none

echo ">> Ensuring container '$STATE_CONTAINER'"
az storage container create \
  --name "$STATE_CONTAINER" \
  --account-name "$STATE_SA" \
  --auth-mode login \
  --output none

cat <<EOF

Remote state is ready. Initialise Terraform with:

  terraform -chdir=infra/terraform init \\
    -backend-config="resource_group_name=$STATE_RG" \\
    -backend-config="storage_account_name=$STATE_SA" \\
    -backend-config="container_name=$STATE_CONTAINER" \\
    -backend-config="key=$STATE_KEY"

For the CD workflow, store these as GitHub Secrets:

  TFSTATE_RESOURCE_GROUP  = $STATE_RG
  TFSTATE_STORAGE_ACCOUNT = $STATE_SA
  TFSTATE_CONTAINER       = $STATE_CONTAINER
  TFSTATE_KEY             = $STATE_KEY
EOF
