# Non-secret defaults, committed so a plan/apply is reproducible without flags.
# Override any of these with -var / a *.auto.tfvars file or TF_VAR_* env vars.
# (No secrets live here — app secrets go through GitHub Secrets -> Ansible/Helm.)

location            = "westeurope"
resource_group_name = "rg-tso"
cluster_name        = "aks-tso"
namespace           = "tso"

node_count = 2
node_size  = "Standard_B2s"

# Must be globally unique within the region. Change this if `terraform apply`
# reports the DNS label is already taken.
dns_label = "tso-special-ops"
