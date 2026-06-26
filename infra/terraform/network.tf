resource "azurerm_virtual_network" "tso" {
  name                = "vnet-tso"
  address_space       = ["10.0.0.0/16"]
  location            = azurerm_resource_group.tso.location
  resource_group_name = azurerm_resource_group.tso.name
  tags                = var.tags
}

resource "azurerm_subnet" "tso" {
  name                 = "snet-tso"
  resource_group_name  = azurerm_resource_group.tso.name
  virtual_network_name = azurerm_virtual_network.tso.name
  address_prefixes     = ["10.0.1.0/24"]
}

# Static public IP with a DNS label so the VM keeps a stable, human-readable
# FQDN (<dns_label>.<location>.cloudapp.azure.com) across reboots/redeploys.
resource "azurerm_public_ip" "tso" {
  name                = "pip-tso"
  location            = azurerm_resource_group.tso.location
  resource_group_name = azurerm_resource_group.tso.name
  allocation_method   = "Static"
  sku                 = "Standard"
  domain_name_label   = var.dns_label
  tags                = var.tags
}

# Only SSH (locked down via var.allowed_ssh_cidr), HTTP and HTTPS are public.
# Prometheus (9090) and Grafana (3001) stay closed — reach them via SSH tunnel.
resource "azurerm_network_security_group" "tso" {
  name                = "nsg-tso"
  location            = azurerm_resource_group.tso.location
  resource_group_name = azurerm_resource_group.tso.name
  tags                = var.tags

  security_rule {
    name                       = "AllowSSH"
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
    name                       = "AllowHTTP"
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
    name                       = "AllowHTTPS"
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
    name                          = "ipconfig1"
    subnet_id                     = azurerm_subnet.tso.id
    private_ip_address_allocation = "Dynamic"
    public_ip_address_id          = azurerm_public_ip.tso.id
  }
}

resource "azurerm_network_interface_security_group_association" "tso" {
  network_interface_id      = azurerm_network_interface.tso.id
  network_security_group_id = azurerm_network_security_group.tso.id
}
