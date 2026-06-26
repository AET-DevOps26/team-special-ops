#!/usr/bin/env bash
#
# One-command Helm deploy for the tso stack to the course Rancher cluster.
#
#   ./infra/k8s/deploy.sh rancher        # deploy to the course Rancher cluster
#
# NOTE: Azure does NOT use this script / Helm. The Azure target is a single VM
# running Docker Compose, provisioned with Terraform + Ansible. See
# docs/project-guidelines/azure-deployment-plan.md (and infra/terraform/,
# infra/ansible/, infra/docker-compose.azure.yml).
#
# Idempotent: run it on a fresh/wiped namespace or to redeploy — `helm upgrade
# --install` converges either way. Image tag defaults to `latest` (the moving tag
# published only by main builds), so a plain run always deploys the newest main.
# Override with IMAGE_TAG=sha-<sha> to pin/rollback to a specific commit.
#
# Secrets are read from the environment so nothing sensitive lives in git. Set
# them before running (or source a gitignored infra/k8s/.env):
#   JWT_SECRET, POSTGRES_PASSWORD, LOGOS_API_KEY (optional)

set -euo pipefail

ENV="${1:-}"
if [[ "$ENV" != "rancher" ]]; then
  echo "usage: $0 rancher" >&2
  echo "(Azure uses Terraform + Ansible + Docker Compose, not Helm — see azure-deployment-plan.md)" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHART_DIR="$SCRIPT_DIR/chart"
VALUES_FILE="$CHART_DIR/values-$ENV.yaml"
RELEASE="tso"
NAMESPACE="${NAMESPACE:-team-special-ops}"

# Optional local secrets file (gitignored). Never commit real values.
if [[ -f "$SCRIPT_DIR/.env" ]]; then
  # shellcheck disable=SC1091
  source "$SCRIPT_DIR/.env"
fi

IMAGE_TAG="${IMAGE_TAG:-latest}"

echo ">> Deploying release '$RELEASE' to namespace '$NAMESPACE' ($ENV), image tag: $IMAGE_TAG"

# Build --set args only for secrets that are actually present, so we never
# overwrite a value with an empty string.
SECRET_ARGS=()
[[ -n "${JWT_SECRET:-}" ]]       && SECRET_ARGS+=(--set "secrets.jwtSecret=$JWT_SECRET")
[[ -n "${POSTGRES_PASSWORD:-}" ]] && SECRET_ARGS+=(--set "secrets.postgresPassword=$POSTGRES_PASSWORD")
[[ -n "${LOGOS_API_KEY:-}" ]]   && SECRET_ARGS+=(--set "secrets.logosApiKey=$LOGOS_API_KEY")

helm upgrade --install "$RELEASE" "$CHART_DIR" \
  --namespace "$NAMESPACE" --create-namespace \
  -f "$VALUES_FILE" \
  --set image.tag="$IMAGE_TAG" \
  "${SECRET_ARGS[@]}" \
  --wait --timeout 5m

# Verify, don't assume: fail loudly if anything didn't roll out.
echo ">> Rollout status:"
kubectl -n "$NAMESPACE" rollout status statefulset/"$RELEASE"-postgres --timeout=3m
for dep in $(kubectl -n "$NAMESPACE" get deploy -o name); do
  kubectl -n "$NAMESPACE" rollout status "$dep" --timeout=3m
done

echo ">> Done. Pods:"
kubectl -n "$NAMESPACE" get pods
