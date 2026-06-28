output "public_ip" {
  description = "Static public IP of the VM."
  value       = azurerm_public_ip.tso.ip_address
}

output "fqdn" {
  description = "Public DNS name of the VM (<dns_label>.<location>.cloudapp.azure.com)."
  value       = azurerm_public_ip.tso.fqdn
}

output "admin_username" {
  description = "SSH username for the VM."
  value       = azurerm_linux_virtual_machine.tso.admin_username
}
