variable "location" {
  description = "Azure region for all resources."
  type        = string
  default     = "westeurope"
}

variable "resource_group_name" {
  description = "Name of the resource group that holds everything (single teardown boundary)."
  type        = string
  default     = "rg-tso"
}

variable "cluster_name" {
  description = "AKS cluster name."
  type        = string
  default     = "aks-tso"
}

variable "dns_prefix" {
  description = "DNS prefix for the AKS API server FQDN."
  type        = string
  default     = "tso"
}

variable "kubernetes_version" {
  description = "Kubernetes version for the cluster. Empty string lets AKS pick the default for the region."
  type        = string
  default     = ""
}

variable "node_count" {
  description = "Number of worker nodes in the default pool. 2 gives headroom and lets self-healing be demoed."
  type        = number
  default     = 2
}

variable "node_size" {
  description = "VM size for worker nodes. Standard_B2s is the cheap burstable baseline for a course demo."
  type        = string
  default     = "Standard_B2s"
}

variable "namespace" {
  description = "Kubernetes namespace the app is deployed into (passed through to Ansible/Helm)."
  type        = string
  default     = "tso"
}

variable "dns_label" {
  description = <<-EOT
    DNS label applied to the ingress public IP, yielding the tutor-facing host
    <dns_label>.<location>.cloudapp.azure.com. Must be globally unique within the
    region, so include something distinctive. Ansible sets the matching
    `service.beta.kubernetes.io/azure-dns-label-name` annotation on ingress-nginx.
  EOT
  type        = string
  default     = "tso-special-ops"
}

variable "tags" {
  description = "Tags applied to all resources for cost tracking / cleanup."
  type        = map(string)
  default = {
    project   = "team-special-ops"
    managedBy = "terraform"
  }
}
