terraform {
  required_version = ">= 1.5.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }
  }

  # Remote state in Azure Storage so CD and laptops share one state.
  # Coordinates are supplied at `terraform init` time via -backend-config
  # (see bootstrap.sh / README.md), keeping this file environment-agnostic.
  backend "azurerm" {}
}

provider "azurerm" {
  features {}
}

# Single resource group holds everything — teardown is one `terraform destroy`.
resource "azurerm_resource_group" "tso" {
  name     = var.resource_group_name
  location = var.location
  tags     = var.tags
}
