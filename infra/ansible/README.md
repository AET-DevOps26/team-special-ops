# Ansible — configure the Azure VM and run the stack

Takes the bare Ubuntu VM that `infra/terraform/` provisions and turns it into a
running deployment: installs Docker CE + the compose plugin, ships the compose
files and observability config, renders `/opt/tso/.env`, and brings the stack up
from **GHCR images** (no build on the VM).

## Prerequisites

```bash
ansible-galaxy collection install -r requirements.yml   # community.docker
```

You also need the SSH private key whose public half Terraform installed on the
VM, and the VM's public IP / FQDN (from `terraform output`).

## Run

Secrets are read from the environment (never committed, never in `group_vars`):

```bash
export JWT_SECRET="…"
export POSTGRES_PASSWORD="…"
export LOGOS_API_KEY="…"          # optional, enables GenAI

ansible-playbook -i "$(terraform -chdir=../terraform output -raw public_ip)," playbook.yml \
  -u azureuser \
  --private-key ~/.ssh/tso_azure \
  -e domain="$(terraform -chdir=../terraform output -raw fqdn)" \
  -e image_tag=latest
```

> The trailing comma in `-i '<ip>,'` is Ansible's shorthand for an inline,
> one-host inventory — no inventory file needed.

## What it does (`playbook.yml`)

1. Asserts `JWT_SECRET` + `POSTGRES_PASSWORD` are set.
2. Installs Docker CE + compose plugin from Docker's official apt repo.
3. Copies `docker-compose.yml`, `docker-compose.azure.yml`, and `observability/`
   into `{{ app_dir }}` (`/opt/tso`).
4. Renders `.env` from `templates/env.j2` (secrets via `lookup('env', ...)`).
5. Optionally `docker login ghcr.io` (only if `ghcr_username` is set and
   `GHCR_TOKEN` is exported — for private packages).
6. Runs `docker compose -f docker-compose.yml -f docker-compose.azure.yml up`
   with `pull: always`, `build: never`.

## Configuration

Non-secret knobs live in `group_vars/all.yml` (`app_dir`, `domain`, `image_tag`,
`image_registry`, `letsencrypt_email`, `postgres_user/db`, `llm_model`,
`llm_base_url`). Override any per run with `-e key=value`.

## Validate without a VM

```bash
ansible-playbook --syntax-check playbook.yml
ansible-lint playbook.yml
```
