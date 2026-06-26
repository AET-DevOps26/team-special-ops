#!/usr/bin/env bash
#
# One-time bootstrap of the Terraform remote-state backend.
#
# Terraform stores its state in an Azure Storage container so that CD and any
# teammate's laptop share a single source of truth (and lock against each other).
# That backend has to exist *before* the first `terraform init`, so this script
# creates it idempotently. Run it once per subscription.
#
#   ./bootstrap.sh
#
# Then init Terraform against the backend it created:
#   terraform init \
#     -backend-config="resource_group_name=$TFSTATE_RESOURCE_GROUP" \
#     -backend-config="storage_account_name=$TFSTATE_STORAGE_ACCOUNT" \
#     -backend-config="container_name=$TFSTATE_CONTAINER" \
#     -backend-config="key=$TFSTATE_KEY"
#
# Requires: az CLI, already logged in (`az login`) to the right subscription.

set -euo pipefail

# Override any of these via environment before running.
LOCATION="${TFSTATE_LOCATION:-westeurope}"
RESOURCE_GROUP="${TFSTATE_RESOURCE_GROUP:-rg-tso-tfstate}"
# Storage account names are globally unique + 3-24 lowercase alphanumeric chars.
STORAGE_ACCOUNT="${TFSTATE_STORAGE_ACCOUNT:-tsotfstate$RANDOM}"
CONTAINER="${TFSTATE_CONTAINER:-tfstate}"

echo ">> Bootstrapping Terraform backend in subscription:"
az account show --query '{name:name, id:id}' -o table

echo ">> Resource group: $RESOURCE_GROUP ($LOCATION)"
az group create --name "$RESOURCE_GROUP" --location "$LOCATION" --output none

echo ">> Storage account: $STORAGE_ACCOUNT"
if ! az storage account show --name "$STORAGE_ACCOUNT" --resource-group "$RESOURCE_GROUP" --output none 2>/dev/null; then
  az storage account create \
    --name "$STORAGE_ACCOUNT" \
    --resource-group "$RESOURCE_GROUP" \
    --location "$LOCATION" \
    --sku Standard_LRS \
    --kind StorageV2 \
    --min-tls-version TLS1_2 \
    --allow-blob-public-access false \
    --output none
fi

echo ">> Blob container: $CONTAINER"
az storage container create \
  --name "$CONTAINER" \
  --account-name "$STORAGE_ACCOUNT" \
  --auth-mode login \
  --output none

cat <<EOF

>> Backend ready. Use these as your backend config / GitHub secrets:

  TFSTATE_RESOURCE_GROUP = $RESOURCE_GROUP
  TFSTATE_STORAGE_ACCOUNT = $STORAGE_ACCOUNT
  TFSTATE_CONTAINER      = $CONTAINER
  TFSTATE_KEY            = tso.tfstate   (suggested)

Then:
  terraform init \\
    -backend-config="resource_group_name=$RESOURCE_GROUP" \\
    -backend-config="storage_account_name=$STORAGE_ACCOUNT" \\
    -backend-config="container_name=$CONTAINER" \\
    -backend-config="key=tso.tfstate"
EOF
