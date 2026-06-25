terraform {
  required_version = ">= 1.6.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "~> 3.0"
    }
  }

  # Remote state lives in an Azure Storage container so CI and humans share one
  # state file (local state would diverge between the laptop and the runner).
  # The storage account + container are created once by bootstrap.sh BEFORE the
  # first `terraform init`. Backend coordinates are passed via `-backend-config`
  # (the CD workflow does this), so nothing secret needs to be committed here.
  # Auth: CI sets ARM_USE_OIDC + ARM_* (see cd-azure.yml). Locally, `az login`
  # uses your user session. Explicit flags avoid guessing how state auth works.
  backend "azurerm" {
    use_azuread_auth = true
    use_oidc         = true
    # resource_group_name  = "rg-tfstate"        # set via -backend-config / env
    # storage_account_name = "tsotfstate12345"   # set via -backend-config / env
    # container_name       = "tfstate"           # set via -backend-config / env
    # key                  = "tso-azure.tfstate" # set via -backend-config / env
  }
}

# OIDC from GitHub Actions: azure/login@v2 exports ARM_* env vars and the
# provider authenticates with the federated token — no client secret stored.
# Locally it falls back to your `az login` session.
provider "azurerm" {
  features {}
}

provider "azuread" {}
