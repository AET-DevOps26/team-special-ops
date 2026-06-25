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

variable "vm_size" {
  description = "VM size for the app host. Standard_B2s is the cheap burstable baseline for a course demo; bump to B2ms/B4ms if the JVM services need more RAM."
  type        = string
  default     = "Standard_B2s"
}

variable "admin_username" {
  description = "Admin (SSH) user created on the VM."
  type        = string
  default     = "azureuser"
}

variable "admin_ssh_public_key" {
  description = <<-EOT
    OpenSSH public key authorised on the VM for the admin user (e.g. the contents
    of ~/.ssh/id_ed25519.pub). The matching PRIVATE key is what Ansible/CD connect
    with. Pass via TF_VAR_admin_ssh_public_key or a *.auto.tfvars file — never
    commit a real key here.
  EOT
  type        = string
}

variable "allowed_ssh_cidr" {
  description = "Source CIDR allowed to reach SSH (port 22). Defaults to * for the demo — tighten to your IP/range for anything real."
  type        = string
  default     = "*"
}

variable "dns_label" {
  description = <<-EOT
    DNS label applied to the VM's public IP, yielding the tutor-facing host
    <dns_label>.<location>.cloudapp.azure.com. Must be globally unique within the
    region, so include something distinctive. The compose Traefik config requests
    a Let's Encrypt cert for this exact host.
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
