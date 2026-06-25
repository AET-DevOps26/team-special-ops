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
# as the CD workflow's TFSTATE_* secrets). Re-running with the same STATE_SA is
# safe: every `az ... create` is a no-op if the resource already exists.

set -euo pipefail

# --- knobs (override via env) -------------------------------------------------
LOCATION="${LOCATION:-westeurope}"
STATE_RG="${STATE_RG:-rg-tfstate}"
# Storage account names are GLOBAL and must be 3-24 lowercase alphanumerics.
# Set STATE_SA explicitly before the first run and reuse it forever (also store
# it as TFSTATE_STORAGE_ACCOUNT in GitHub Secrets). If unset, we derive a stable
# name from the active subscription id so re-runs do not fork state via $RANDOM.
if [[ -z "${STATE_SA:-}" ]]; then
  sub_id="$(az account show --query id -o tsv)"
  sub_suffix="$(printf '%s' "$sub_id" | tr -d '-' | cut -c1-8)"
  STATE_SA="tsotfstate${sub_suffix}"
  echo ">> STATE_SA not set; using subscription-stable name: $STATE_SA" >&2
  echo ">> Pin it for clarity: STATE_SA=$STATE_SA $0" >&2
fi
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
