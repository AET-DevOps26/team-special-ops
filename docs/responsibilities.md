# Team Responsibilities

Who owns what in **team-special-ops** (SceneIt). This doc satisfies the course
requirement that student responsibilities are documented and traceable in the
README. Contributions are also visible in GitHub (commits, PR authorship, reviews)
and Artemis team channels.

**RACI legend:** **R** = Responsible (does the work) · **A** = Accountable (owns
the outcome) · **C** = Consulted · **I** = Informed

## Team

| Student | GitHub | Primary subsystem | Main artefacts (oral-exam candidates) |
|---|---|---|---|
| Atharva Mathapati | [`atharvamp`](https://github.com/atharvamp) | Server + Rancher/K8s deployment + observability | Spring services (`user-progress`, `catalog`), Helm chart (`infra/k8s/`), Rancher CD (`.github/workflows/deploy.yml`), Prometheus/Grafana/alerts (`infra/observability/`) |
| Tejash Varsani | [`tejash123`](https://github.com/tejash123) | GenAI + chat Q&A + Azure deployment | GenAI service (`services/genai/`), spoiler-safe chat orchestration (`services/chat/`), Azure IaC (`infra/terraform/`, `infra/ansible/`, `.github/workflows/cd-azure.yml`) |
| Leo Pahl | [`leopahl`](https://github.com/leopahl) | Client + API gateway + end-to-end testing | React web-client (auth, routing), Traefik gateway (`infra/docker-compose.yml`), Playwright e2e (`web-client/e2e/`), user authentication (`services/user-progress`) |

Each student owns one primary subsystem per
[`docs/project-guidelines/project-details.md`](project-guidelines/project-details.md),
but all three collaborate on integration, deployment, and debugging. Shared work
(e.g. OpenAPI changes, docker-compose, CI) is listed in the RACI matrix below.

> Registration details (TUMonline login, matriculation number) are submitted via
> the course registration form and tracked in Artemis — not duplicated here.

## RACI matrix

| Area | Atharva | Tejash | Leo |
|---|---|---|---|
| OpenAPI contract (`api/openapi.yaml`) | A/R | C | C |
| `user-progress` (auth, JWT, progress) | A/R | I | C |
| `catalog` (series, episodes, summaries) | A/R | I | I |
| `chat` (Q&A orchestration, spoiler filter) | C | A/R | I |
| `genai` (LLM `/genai/ask`, prompts) | I | A/R | I |
| `web-client` (React SPA) | R | C | A/R |
| Traefik API gateway (local routing) | C | I | A/R |
| Local stack (`infra/docker-compose.yml`) | R | R | R |
| CI — build, test, lint (`.github/workflows/ci.yml`) | A/R | R | R |
| CD — Rancher/Kubernetes (`.github/workflows/deploy.yml`, `infra/k8s/`) | A/R | C | I |
| CD — Azure VM (`.github/workflows/cd-azure.yml`, Terraform, Ansible) | C | A/R | R |
| Observability (Prometheus, Grafana, alerts) | A/R | C | I |
| Unit & integration tests (per service) | R | R | R |
| E2E tests (Playwright in CI) | C | I | A/R |
| Architecture & engineering docs (`docs/`) | A/R | R | R |

## How to trace ownership in GitHub

| What to look at | Where |
|---|---|
| Who wrote the code | `git log -- path/` or GitHub **Blame** / **History** |
| Who reviewed changes | Merged PRs → **Files changed** → review comments |
| Who integrated a feature | PR author + approvers on the merge commit |
| Branch naming | e.g. `tejash/azure-vm`, `fix/azure-jwt-secret-user-progress` |

Example — GenAI ownership:

```bash
git log --oneline -- services/genai services/chat/src/main/java/com/tso/chat/client
```

## Collaboration expectations

- **API changes** start in `api/openapi.yaml`, then `./api/scripts/gen-all.sh`; the
  accountable owner for that area coordinates the PR.
- **Integration** (chat → genai, Traefik routes, JWT across services) is a shared
  responsibility — the primary owner opens the PR; others review and help debug.
- **Deployment** paths are split: Rancher/K8s (Atharva) and Azure VM (Tejash), with
  Leo supporting gateway/CORS/e2e validation on both.
- **Communication** for planning and tutor feedback stays in the official Artemis
  team channel (course requirement).
