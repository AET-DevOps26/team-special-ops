# One resource group is the lifecycle + billing boundary: `terraform destroy`
# (or deleting this group) removes the VM, disk, NIC, and public IP in one shot.
resource "azurerm_resource_group" "tso" {
  name     = var.resource_group_name
  location = var.location
  tags     = var.tags
}

# A minimal VNet/subnet for the single VM. We don't need multiple subnets or
# peering for a one-box demo; this just gives the NIC somewhere to live.
resource "azurerm_virtual_network" "tso" {
  name                = "vnet-tso"
  location            = azurerm_resource_group.tso.location
  resource_group_name = azurerm_resource_group.tso.name
  address_space       = ["10.0.0.0/16"]
  tags                = var.tags
}

resource "azurerm_subnet" "tso" {
  name                 = "subnet-tso"
  resource_group_name  = azurerm_resource_group.tso.name
  virtual_network_name = azurerm_virtual_network.tso.name
  address_prefixes     = ["10.0.1.0/24"]
}

# Static public IP with a DNS label, giving the stable tutor-facing host
# <dns_label>.<location>.cloudapp.azure.com. Static (not Dynamic) so the FQDN and
# address survive VM restarts — required for Let's Encrypt to keep validating.
resource "azurerm_public_ip" "tso" {
  name                = "pip-tso"
  location            = azurerm_resource_group.tso.location
  resource_group_name = azurerm_resource_group.tso.name
  allocation_method   = "Static"
  sku                 = "Standard"
  domain_name_label   = var.dns_label
  tags                = var.tags
}

# Firewall: SSH locked to an admin CIDR (tighten var.allowed_ssh_cidr from the
# demo default of *), HTTP/HTTPS open to the world so the app + ACME work.
# Prometheus/Grafana (9090/3001) are deliberately NOT opened — they stay
# reachable only over SSH tunnels.
resource "azurerm_network_security_group" "tso" {
  name                = "nsg-tso"
  location            = azurerm_resource_group.tso.location
  resource_group_name = azurerm_resource_group.tso.name
  tags                = var.tags

  security_rule {
    name                       = "allow-ssh"
    priority                   = 100
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "22"
    source_address_prefix      = var.allowed_ssh_cidr
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "allow-http"
    priority                   = 110
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "80"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "allow-https"
    priority                   = 120
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "443"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }
}

resource "azurerm_network_interface" "tso" {
  name                = "nic-tso"
  location            = azurerm_resource_group.tso.location
  resource_group_name = azurerm_resource_group.tso.name
  tags                = var.tags

  ip_configuration {
    name                          = "internal"
    subnet_id                     = azurerm_subnet.tso.id
    private_ip_address_allocation = "Dynamic"
    public_ip_address_id          = azurerm_public_ip.tso.id
  }
}

# Apply the NSG to the VM's NIC so the rules above actually filter its traffic.
resource "azurerm_network_interface_security_group_association" "tso" {
  network_interface_id      = azurerm_network_interface.tso.id
  network_security_group_id = azurerm_network_security_group.tso.id
}
