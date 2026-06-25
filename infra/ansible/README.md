# Ansible — Azure VM configuration + app deploy

Takes the bare Linux VM from [`../terraform`](../terraform) and makes it serve the
app over HTTPS by installing Docker and running the existing
[`docker-compose`](../docker-compose.yml) stack with the
[Azure overlay](../docker-compose.azure.yml).

The playbook runs **over SSH** against the VM (no Kubernetes, no Helm). The host
is not committed: Terraform's `public_ip` output is passed at run time via
`--inventory "<ip>,"`.

| File | Purpose |
|---|---|
| `requirements.yml` | Galaxy collections (`community.docker`, `community.general`) |
| `inventory.ini` | Placeholder — the VM host is passed dynamically (`--inventory`) |
| `group_vars/all.yml` | Non-secret knobs (app dir, domain, image tag, LE email, LLM) |
| `playbook.yml` | Install Docker → ship compose + observability → template `.env` → compose up |
| `templates/env.j2` | The `.env` rendered onto the VM (secrets + config) |
| `ansible.cfg` | Inventory + sensible defaults |

## What the playbook does (in order)

1. Installs **Docker CE + the compose plugin** from Docker's apt repo.
2. Creates `app_dir` (default `/opt/tso`) and copies `docker-compose.yml`,
   `docker-compose.azure.yml`, and `observability/` to the VM.
3. Templates `.env` onto the VM (Postgres/JWT/LOGOS secrets + `DOMAIN`,
   `IMAGE_TAG`, `LETSENCRYPT_EMAIL`, LLM config) with `0600` perms.
4. Runs `community.docker.docker_compose_v2` to **pull the GHCR images** and bring
   the stack up (`build: never` — no compiling on the VM). Traefik then terminates
   TLS with a Let's Encrypt cert for `DOMAIN`.

## Secrets

Read from the environment (never committed):

- `JWT_SECRET`, `POSTGRES_PASSWORD`, `LOGOS_API_KEY`

In CD these come from GitHub Secrets; locally, export them in your shell. They are
written into the VM's `.env` (`0600`) and consumed by the compose stack.

## Run it locally

```bash
# 1. VM must already exist (terraform apply). Grab its coordinates:
cd ../terraform
IP=$(terraform output -raw public_ip)
USER=$(terraform output -raw admin_username)
FQDN=$(terraform output -raw fqdn)
cd ../ansible

# 2. install collections once:
ansible-galaxy collection install -r requirements.yml
pip install docker        # the community.docker modules need the Python SDK

# 3. deploy (SSH key is the private half of admin_ssh_public_key):
export JWT_SECRET=... POSTGRES_PASSWORD=... LOGOS_API_KEY=...
ansible-playbook playbook.yml \
  --inventory "${IP}," \
  --user "${USER}" \
  --private-key ~/.ssh/tso_azure \
  -e "domain=${FQDN}" \
  -e "image_tag=sha-$(git rev-parse --short HEAD)"
```

In CI this is driven by [`.github/workflows/cd-azure.yml`](../../.github/workflows/cd-azure.yml),
which passes the Terraform outputs in automatically.
