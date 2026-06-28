# Azure VM Setup — Handoff Checklist

One-time setup so GitHub Actions can deploy **Team Special Ops** to an Azure Virtual Machine running Docker Compose.

Nothing from this list goes into source code — all credentials live in **GitHub Secrets** only.

**Who this is for:** whoever owns the Azure subscription and has admin/maintainer access to the GitHub repo.

**Time:** ~30–45 minutes once.

---

## Goal

Set up Azure and GitHub once so anyone on the team can run:

**Actions → Deploy to Azure VM**

This should deploy the app to an Azure VM, provide a public HTTPS URL, and allow the team to tear the VM down afterward to stop billing.

---



## Prerequisites

Before you start, confirm you have:

- [ ] An Azure subscription. Azure for Students credit works.
- [ ] Contributor or Owner role on that subscription.
- [ ] Azure CLI installed.

```bash
az --version
```

- [ ] GitHub admin/maintainer access on the repo.
  - Secrets
  - Environments
- [ ] The Azure VM code merged to `main`.
  - Workflow: `.github/workflows/cd-azure.yml`

---



## 1. Azure subscription

Log in and select the subscription that will pay for the VM.

```bash
az login
az account list -o table
az account set --subscription "<subscription-name-or-id>"
az account show --query "{subscription:id, tenant:tenantId}" -o table
```

Add these as GitHub repository secrets:

**GitHub → Settings → Secrets and variables → Actions**


| Secret                  | Source                                       |
| ----------------------- | -------------------------------------------- |
| `AZURE_SUBSCRIPTION_ID` | `subscription` column from the command above |
| `AZURE_TENANT_ID`       | `tenant` column from the command above       |


You do **not** create the VM, network, or public IP manually. Terraform creates those on the first deploy.

---



## 2. Terraform remote state

Terraform stores its state in Azure Storage so GitHub Actions and laptops share one locked state file.

Create the backend once before the first deploy.

```bash
git clone <repo-url>
cd team-special-ops/infra/terraform
./bootstrap.sh
```

The script creates:

- A resource group, default: `rg-tso-tfstate`
- A storage account
- A blob container

It prints four values at the end.

Add these as GitHub repository secrets:


| Secret                    | Typical value     | Notes                                 |
| ------------------------- | ----------------- | ------------------------------------- |
| `TFSTATE_RESOURCE_GROUP`  | `rg-tso-tfstate`  | From script output                    |
| `TFSTATE_STORAGE_ACCOUNT` | `tsotfstate12345` | Globally unique name from output      |
| `TFSTATE_CONTAINER`       | `tfstate`         | From script output                    |
| `TFSTATE_KEY`             | `tso.tfstate`     | Suggested blob name; any name is fine |


This requires `az login` from step 1 with permission to create resource groups and storage accounts.

---



## 3. SSH key for the VM

The VM accepts SSH-key login only. There is no password login.

Generate a dedicated key pair for this deployment.

```bash
ssh-keygen -t ed25519 -f ~/.ssh/tso_azure -N "" -C tso-azure
```

Add these directly as GitHub repository secrets.

Do **not** send the private key over chat, email, or commit it to git.


| Secret                     | Source                                                                 |
| -------------------------- | ---------------------------------------------------------------------- |
| `AZURE_VM_SSH_PUBLIC_KEY`  | Full contents of `~/.ssh/tso_azure.pub`, one line                      |
| `AZURE_VM_SSH_PRIVATE_KEY` | Full contents of `~/.ssh/tso_azure`, including `BEGIN` and `END` lines |


Terraform installs the public key on the VM.

The GitHub workflow uses the private key for Ansible over SSH.

---



## 4. Azure App Registration — OIDC

GitHub Actions logs into Azure using OIDC.

No stored Azure client secret is needed.

Register an app in Microsoft Entra ID and trust this repo's GitHub environments.

---



### 4a. Create the app in Azure Portal

Go to:

**Microsoft Entra ID → App registrations → New registration**

Use:


| Field        | Value                 |
| ------------ | --------------------- |
| Name         | `github-tso-azure-cd` |
| Redirect URI | Leave empty           |


Then click **Register**.

Copy:

**Application (client) ID**

Add it as this GitHub repository secret:


| Secret            | Source                                        |
| ----------------- | --------------------------------------------- |
| `AZURE_CLIENT_ID` | Application/client ID of the app registration |


---



### 4b. Add federated credentials in Azure Portal

Go to:

**Certificates & secrets → Federated credentials → Add credential**

Create two credentials.

Entity type must be **Environment**, not Branch.


| Name                       | GitHub org | GitHub repo        | Environment name |
| -------------------------- | ---------- | ------------------ | ---------------- |
| `github-env-azure`         | `<ORG>`    | `team-special-ops` | `azure`          |
| `github-env-azure-destroy` | `<ORG>`    | `team-special-ops` | `azure-destroy`  |


The resulting subject must look like this:

```text
repo:<ORG>/team-special-ops:environment:azure
repo:<ORG>/team-special-ops:environment:azure-destroy
```

Example:

```text
repo:aet-devops26/team-special-ops:environment:azure
```

---



### 4c. Grant permissions

Go to:

**Subscriptions → your subscription → Access control (IAM) → Add role assignment**

Use:


| Field     | Value                     |
| --------- | ------------------------- |
| Role      | Contributor               |
| Assign to | `github-tso-azure-cd` app |


For a course project, Contributor on the whole subscription is simplest.

---



### 4d. Alternative — Azure CLI

Replace `YOUR_ORG` and `YOUR_REPO`.

```bash
APP_NAME="github-tso-azure-cd"
SUB_ID="$(az account show --query id -o tsv)"

APP_ID="$(az ad app create --display-name "$APP_NAME" --query appId -o tsv)"

echo "AZURE_CLIENT_ID = $APP_ID"

az ad app federated-credential create --id "$APP_ID" --parameters '{
  "name": "github-env-azure",
  "issuer": "https://token.actions.githubusercontent.com",
  "subject": "repo:YOUR_ORG/YOUR_REPO:environment:azure",
  "audiences": ["api://AzureADTokenExchange"]
}'

az ad app federated-credential create --id "$APP_ID" --parameters '{
  "name": "github-env-azure-destroy",
  "issuer": "https://token.actions.githubusercontent.com",
  "subject": "repo:YOUR_ORG/YOUR_REPO:environment:azure-destroy",
  "audiences": ["api://AzureADTokenExchange"]
}'

az role assignment create \
  --assignee "$APP_ID" \
  --role Contributor \
  --scope "/subscriptions/$SUB_ID"
```

No client secret is created or needed.

---



## 5. GitHub Environments

Go to:

**GitHub → Settings → Environments**

Create two environments with these exact names because the OIDC federated credentials reference them.


| Environment     | Purpose  | Recommended setting                   |
| --------------- | -------- | ------------------------------------- |
| `azure`         | Deploy   | No approval required for faster demos |
| `azure-destroy` | Teardown | Required reviewers enabled            |


---



## 6. GitHub Secrets

Go to:

**GitHub → Settings → Secrets and variables → Actions → New repository secret**

Add every secret below.

If Rancher CD, `deploy.yml`, already works, you may already have the application secrets. Reuse the same values.

---



### Azure — OIDC


| Secret                  | Set in step |
| ----------------------- | ----------- |
| `AZURE_CLIENT_ID`       | 4           |
| `AZURE_TENANT_ID`       | 1           |
| `AZURE_SUBSCRIPTION_ID` | 1           |


---



### Terraform state


| Secret                    | Set in step |
| ------------------------- | ----------- |
| `TFSTATE_RESOURCE_GROUP`  | 2           |
| `TFSTATE_STORAGE_ACCOUNT` | 2           |
| `TFSTATE_CONTAINER`       | 2           |
| `TFSTATE_KEY`             | 2           |


---



### SSH


| Secret                     | Set in step |
| -------------------------- | ----------- |
| `AZURE_VM_SSH_PUBLIC_KEY`  | 3           |
| `AZURE_VM_SSH_PRIVATE_KEY` | 3           |


---



### Application


| Secret              | Notes                                 |
| ------------------- | ------------------------------------- |
| `JWT_SECRET`        | Minimum 32 characters; team generates |
| `POSTGRES_PASSWORD` | Database password; team chooses       |
| `LOGOS_API_KEY`     | Optional; enables GenAI via TUM Logos |


Total: **12 repository secrets**.

---



## 7. Docker images — GHCR

The VM pulls prebuilt images from GitHub Container Registry.

The current workflow does not pass a registry login token, so images must be public.

Before deploying:

- [ ] Ensure CI on `main` has passed.
- [ ] Confirm CI publishes `latest` and `sha-<commit>` tags on every green merge.
- [ ] Go to **GitHub → Packages**.
- [ ] For each `tso-*` package:
  - Package settings
  - Change visibility
  - Public

Images used:

```text
ghcr.io/aet-devops26/tso-user-progress
ghcr.io/aet-devops26/tso-catalog
ghcr.io/aet-devops26/tso-chat
ghcr.io/aet-devops26/tso-genai
ghcr.io/aet-devops26/tso-web-client
```

---



## 8. Test deployment

Before running the workflow:

- [ ] Confirm the Azure VM PR is merged to `main`.
- [ ] Confirm CI on `main` finished successfully.
- [ ] Confirm Docker images were published.

Then go to:

**GitHub → Actions → Deploy to Azure VM → Run workflow**

Use:


| Input       | Value                                               |
| ----------- | --------------------------------------------------- |
| `action`    | `deploy`                                            |
| `image_tag` | `latest`, or a specific `sha-abc1234` from a CI run |


The workflow will:

1. Log into Azure via OIDC.
2. Run `terraform apply`.
3. Create:
  - VM
  - Network
  - Static public IP
  - DNS
4. Wait for SSH.
5. Run Ansible.
6. Install Docker.
7. Start the Docker Compose stack.
8. Run an HTTPS smoke test.
9. Print the live URL in the job summary.

First deploy takes around **10–15 minutes**.

---



### Verify

Open the URL from the job summary.

Do not use bare HTTP on the IP.

```text
https://tso-special-ops.westeurope.cloudapp.azure.com
```

Expected result:

- [ ] Valid Let's Encrypt certificate.
  - It may take 1–2 minutes on the first request.
- [ ] Login works.

```text
demo@example.com / password123
```

- [ ] Core app flow works.

If the DNS label is already taken globally, change `dns_label` in:

```text
infra/terraform/terraform.tfvars
```

Then redeploy.

---



## 9. Cleanup — stop billing

The VM and public IP bill while they run.

Tear down after the demo.

Go to:

**GitHub → Actions → Deploy to Azure VM → Run workflow**

Use:


| Input     | Value     |
| --------- | --------- |
| `action`  | `destroy` |
| `confirm` | `DESTROY` |


A required reviewer on the `azure-destroy` environment must approve before destroy runs.

This deletes resource group:

```text
rg-tso
```

That includes:

- VM
- Public IP
- Network

The Terraform state storage, `rg-tso-tfstate`, is left intact so the next deploy still has its state.

Delete that resource group manually only if you want zero Azure cost.

---



## Completion checklist

Hand back to the team when all boxes are checked:

- [ ] `az login` works; subscription and tenant IDs added as secrets.
- [ ] `bootstrap.sh` ran; four `TFSTATE_*` secrets added.
- [ ] SSH key pair added as `AZURE_VM_SSH_*` secrets.
- [ ] Private key was not shared in chat or committed to git.
- [ ] OIDC app `github-tso-azure-cd` created with federated credentials for:
  - `azure`
  - `azure-destroy`
- [ ] App has Contributor role on the subscription.
- [ ] `AZURE_CLIENT_ID` secret added.
- [ ] GitHub Environments created:
  - `azure`
  - `azure-destroy`
- [ ] Required reviewer enabled on `azure-destroy`.
- [ ] `JWT_SECRET`, `POSTGRES_PASSWORD`, and `LOGOS_API_KEY` secrets added.
- [ ] GHCR `tso-*` packages are public.
- [ ] CI on `main` is green.
- [ ] Test deploy succeeded.
- [ ] HTTPS URL works.
- [ ] VM destroyed after demo, or team notified that it is still running.

---



## Cost and safety


| Area    | Notes                                                                                                                                   |
| ------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| Cost    | Around €10–15/month while the VM runs. Use Azure for Students credit and set a budget alert in Azure Cost Management.                   |
| SSH     | Defaults to open worldwide: `allowed_ssh_cidr = "*"`. Tighten to your IP in `infra/terraform/terraform.tfvars` for anything long-lived. |
| Secrets | Never commit `.env`, SSH keys, or Azure credentials to git.                                                                             |


---



## Troubleshooting


| Problem                            | Likely fix                                                                        |
| ---------------------------------- | --------------------------------------------------------------------------------- |
| OIDC / `AADSTS700213` login failed | Federated credential subject must match `repo:ORG/REPO:environment:azure` exactly |
| Terraform permission denied        | App is missing Contributor role, or wrong subscription is selected                |
| SSH timeout                        | VM may still be booting. Retry. Check SSH key secrets are complete                |
| Docker image pull failed           | GHCR packages are not public                                                      |
| Smoke test failed                  | Stack may still be starting, or `image_tag` does not exist on GHCR                |
| DNS name unavailable               | Change `dns_label` in `infra/terraform/terraform.tfvars`                          |


---



## Further reading

- Full runbook: `docs/project-guidelines/azure-deployment-plan.md`
- Terraform: `infra/terraform/README.md`
- Ansible: `infra/ansible/README.md`

---



## Suggested repo path

```text
docs/project-guidelines/azure-handoff.md
```

