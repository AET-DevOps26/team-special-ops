variable "location" {
  description = "Azure region for all resources."
  type        = string
  default     = "swedencentral"
}

variable "resource_group_name" {
  description = "Name of the resource group that holds the whole deployment."
  type        = string
  default     = "rg-tso"
}

variable "vm_size" {
  description = "VM size. Standard_D2as_v5 (2 vCPU / 8 GiB) — cheapest size available + unrestricted on this Azure-for-Students subscription in the permitted regions."
  type        = string
  default     = "Standard_D2as_v5"
}

variable "admin_username" {
  description = "Admin (SSH) username on the VM."
  type        = string
  default     = "azureuser"
}

variable "admin_ssh_public_key" {
  description = "SSH public key for the admin user. Supplied via TF_VAR_admin_ssh_public_key (no default — never committed)."
  type        = string
}

variable "allowed_ssh_cidr" {
  description = "CIDR allowed to reach SSH (22). Defaults to the whole internet for convenience; tighten to your IP (e.g. 203.0.113.4/32) in production."
  type        = string
  default     = "*"
}

variable "dns_label" {
  description = "DNS label for the public IP, yielding <dns_label>.<location>.cloudapp.azure.com."
  type        = string
  default     = "tso-special-ops"
}

variable "tags" {
  description = "Tags applied to every resource."
  type        = map(string)
  default = {
    project = "team-special-ops"
    managed = "terraform"
  }
}
