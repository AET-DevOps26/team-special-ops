output "resource_group_name" {
  description = "Resource group holding the cluster — feed to `az aks get-credentials`."
  value       = azurerm_resource_group.tso.name
}

output "cluster_name" {
  description = "AKS cluster name — feed to `az aks get-credentials`."
  value       = azurerm_kubernetes_cluster.tso.name
}

output "namespace" {
  description = "Kubernetes namespace the app deploys into."
  value       = var.namespace
}

output "dns_label" {
  description = "DNS label Ansible sets on the ingress public IP."
  value       = var.dns_label
}

# Deterministic public host: Azure builds it from the DNS label we attach to the
# ingress-nginx public IP. Computing it here (rather than reading it back after
# the controller is up) lets Ansible/Helm set ingress.host in the same run, fully
# dynamic with no manual copy-paste.
output "ingress_fqdn" {
  description = "Tutor-facing HTTPS host: <dns_label>.<location>.cloudapp.azure.com."
  value       = "${var.dns_label}.${var.location}.cloudapp.azure.com"
}

output "kube_config_raw" {
  description = "Raw kubeconfig for the cluster (sensitive). CI uses `az aks get-credentials` instead."
  value       = azurerm_kubernetes_cluster.tso.kube_config_raw
  sensitive   = true
}
