# Non-secret defaults for the Azure VM deployment.
# admin_ssh_public_key is intentionally NOT set here — pass it via the
# TF_VAR_admin_ssh_public_key environment variable (CD reads it from a secret),
# e.g.  export TF_VAR_admin_ssh_public_key="$(cat ~/.ssh/tso_azure.pub)"

# Azure-for-Students policy on this subscription only permits 5 regions
# (polandcentral, austriaeast, spaincentral, swedencentral, francecentral) and
# blocks B-series VMs in all of them. swedencentral + D2as_v5 (2 vCPU / 8 GiB,
# AMD, ~€0.09/hr) is the cheapest combo confirmed available + unrestricted here.
location            = "swedencentral"
resource_group_name = "rg-tso"
vm_size             = "Standard_D2as_v5"
admin_username      = "azureuser"
dns_label           = "tso-special-ops"

# Tighten this to your own IP (e.g. "203.0.113.4/32") to restrict SSH.
allowed_ssh_cidr = "*"

tags = {
  project = "team-special-ops"
  managed = "terraform"
}
