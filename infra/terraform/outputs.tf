output "public_ip" {
  description = "Static public IP of the VM — SSH target and the A record behind the FQDN."
  value       = azurerm_public_ip.tso.ip_address
}

# Deterministic public host: Azure builds it from the DNS label we attach to the
# VM's public IP. Computing it here lets Ansible/CD pass it straight to Traefik as
# the DOMAIN (for routing + the Let's Encrypt cert) with no manual copy-paste.
output "fqdn" {
  description = "Tutor-facing HTTPS host: <dns_label>.<location>.cloudapp.azure.com."
  value       = "${var.dns_label}.${var.location}.cloudapp.azure.com"
}

output "admin_username" {
  description = "Admin (SSH) user to connect to the VM as."
  value       = var.admin_username
}
