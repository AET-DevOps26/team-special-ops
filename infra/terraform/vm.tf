# The single Linux VM that runs the whole docker-compose stack. Ansible installs
# Docker and brings the compose up over SSH after this is created.
#
# Auth is SSH-key only (no password): admin_ssh_public_key is the operator's key,
# and its matching private key is what CD/Ansible connect with. Standard_B2s is
# the cheap burstable baseline; bump var.vm_size if the JVM services need more RAM.
resource "azurerm_linux_virtual_machine" "tso" {
  name                = "vm-tso"
  location            = azurerm_resource_group.tso.location
  resource_group_name = azurerm_resource_group.tso.name
  size                = var.vm_size
  admin_username      = var.admin_username

  network_interface_ids = [
    azurerm_network_interface.tso.id,
  ]

  admin_ssh_key {
    username   = var.admin_username
    public_key = var.admin_ssh_public_key
  }

  os_disk {
    caching              = "ReadWrite"
    storage_account_type = "Standard_LRS"
    disk_size_gb         = 64
  }

  # Ubuntu 22.04 LTS (Jammy). Long-term support release with current Docker CE
  # packages available from Docker's apt repo (Ansible adds that repo).
  source_image_reference {
    publisher = "Canonical"
    offer     = "0001-com-ubuntu-server-jammy"
    sku       = "22_04-lts-gen2"
    version   = "latest"
  }

  tags = var.tags
}
