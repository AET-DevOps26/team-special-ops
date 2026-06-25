# Non-secret defaults, committed so a plan/apply is reproducible without flags.
# Override any of these with -var / a *.auto.tfvars file or TF_VAR_* env vars.
# (No secrets live here — app secrets go through GitHub Secrets -> Ansible/.env.)
#
# NOTE: admin_ssh_public_key has no default — it must be supplied via
# TF_VAR_admin_ssh_public_key (CD) or a *.auto.tfvars file (local).

location            = "westeurope"
resource_group_name = "rg-tso"

vm_size        = "Standard_B2s"
admin_username = "azureuser"

# Tighten this to your own IP/CIDR (e.g. "203.0.113.4/32") for anything beyond a
# throwaway demo. "*" leaves SSH open to the internet (key-only auth still applies).
allowed_ssh_cidr = "*"

# Must be globally unique within the region. Change this if `terraform apply`
# reports the DNS label is already taken.
dns_label = "tso-special-ops"
