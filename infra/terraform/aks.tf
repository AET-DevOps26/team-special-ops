# One resource group is the lifecycle + billing boundary: `terraform destroy`
# (or deleting this group) removes the cluster, disks, and public IP in one shot.
resource "azurerm_resource_group" "tso" {
  name     = var.resource_group_name
  location = var.location
  tags     = var.tags
}

# Managed Kubernetes. Azure runs/patches the control plane for free; we only pay
# for the worker VMs in the default node pool. No ACR is attached on purpose —
# the chart pulls the public GHCR images (ghcr.io/aet-devops26/tso-*), so the
# cluster needs no registry credentials.
resource "azurerm_kubernetes_cluster" "tso" {
  name                = var.cluster_name
  location            = azurerm_resource_group.tso.location
  resource_group_name = azurerm_resource_group.tso.name
  dns_prefix          = var.dns_prefix
  kubernetes_version  = var.kubernetes_version != "" ? var.kubernetes_version : null
  tags                = var.tags

  default_node_pool {
    name       = "default"
    node_count = var.node_count
    vm_size    = var.node_size
  }

  # System-assigned managed identity instead of a service-principal password:
  # nothing to leak, nothing to rotate.
  identity {
    type = "SystemAssigned"
  }
}
