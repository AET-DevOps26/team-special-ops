# Non-secret defaults for the Azure VM deployment.
# admin_ssh_public_key is intentionally NOT set here — pass it via the
# TF_VAR_admin_ssh_public_key environment variable (CD reads it from a secret),
# e.g.  export TF_VAR_admin_ssh_public_key="$(cat ~/.ssh/tso_azure.pub)"

location            = "westeurope"
resource_group_name = "rg-tso"
vm_size             = "Standard_B2s"
admin_username      = "azureuser"
dns_label           = "tso-special-ops"

# Tighten this to your own IP (e.g. "203.0.113.4/32") to restrict SSH.
allowed_ssh_cidr = "*"

tags = {
  project = "team-special-ops"
  managed = "terraform"
}
