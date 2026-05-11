#!/usr/bin/env bash
set -euo pipefail

# Usage: gen-all.sh [java|python|ts|all]   (default: all)
#
# Each CI job runs only its own target; local dev runs `all`.

target="${1:-all}"

cd "$(dirname "$0")/../.."

have() { command -v "$1" >/dev/null 2>&1; }

# Lint always runs first — it's a fast safety net (~5s) and the spec is the
# input to every generator, so a broken spec should fail loud before codegen.
echo "→ Validating OpenAPI spec..."
pnpm --package=@redocly/cli@latest dlx redocly lint api/openapi.yaml

if [[ "$target" == "java" || "$target" == "all" ]]; then
  if have java; then
    echo "→ Generating Java stubs for Spring services..."
    java_pkg_for() {
      case "$1" in
        user-progress) echo "userprogress" ;;
        catalog)       echo "catalog" ;;
        chat)          echo "chat" ;;
        *) echo "ERROR: unknown service $1" >&2; exit 1 ;;
      esac
    }
    for svc in user-progress catalog chat; do
      pkg="$(java_pkg_for "$svc")"
      rm -rf "services/$svc/generated"
      pnpm dlx @openapitools/openapi-generator-cli@latest generate \
        -i api/openapi.yaml \
        -g spring \
        -o "services/$svc/generated" \
        --additional-properties="interfaceOnly=true,useTags=true,apiPackage=com.tso.${pkg}.api,modelPackage=com.tso.${pkg}.model,useSpringBoot3=true"
    done
  else
    echo "⚠ skipping Java codegen (java not installed)"
  fi
fi

if [[ "$target" == "python" || "$target" == "all" ]]; then
  if have uvx; then
    echo "→ Generating Python client for GenAI service..."
    rm -rf services/genai/generated
    uvx --from openapi-python-client openapi-python-client generate \
      --path api/openapi.yaml \
      --output-path services/genai/generated \
      --config api/scripts/py-config.json \
      --overwrite
  else
    echo "⚠ skipping Python codegen (uvx not installed)"
  fi
fi

if [[ "$target" == "ts" || "$target" == "all" ]]; then
  if have pnpm; then
    echo "→ Generating TypeScript types for web-client..."
    mkdir -p web-client/src/api
    pnpm dlx openapi-typescript@latest api/openapi.yaml \
      -o web-client/src/api/types.ts
  else
    echo "⚠ skipping TS codegen (pnpm not installed)"
  fi
fi

echo "✓ Codegen complete."
