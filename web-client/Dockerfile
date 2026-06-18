# syntax=docker/dockerfile:1
#
# Multi-stage: Node + pnpm build → nginx serves static files only (no Node in final image).
# Build from repository root:
#   docker build -f web-client/Dockerfile -t tso-web-client .

# --- Build: Node toolchain + production bundle (discarded after this stage) ---
FROM node:20-bookworm-slim AS builder
WORKDIR /build/web-client
ENV CI=true
RUN corepack enable && corepack prepare pnpm@9 --activate

COPY web-client/package.json web-client/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY web-client .
COPY api/openapi.yaml /build/api/openapi.yaml
RUN mkdir -p src/api \
  && pnpm exec openapi-typescript /build/api/openapi.yaml -o src/api/types.ts
RUN pnpm build

# --- Runtime: nginx + static assets only ---
FROM nginx:1.27-alpine AS runtime
# Ship the config as a template so nginx's stock entrypoint runs envsubst on it.
# 15-local-resolvers.envsh exports $NGINX_LOCAL_RESOLVERS from /etc/resolv.conf,
# which the template uses for `resolver` — so the same image gets Docker's
# embedded DNS (127.0.0.11) under compose and CoreDNS under Kubernetes.
# Opt in to nginx's 15-local-resolvers.envsh (it no-ops unless this is set); it
# then exports $NGINX_LOCAL_RESOLVERS from /etc/resolv.conf for the template below.
ENV NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1
# Empty by default so compose resolves bare service names (`catalog`) via Docker
# DNS. Kubernetes overrides this with `.<namespace>.svc.cluster.local` so CoreDNS
# can resolve the FQDN. Must be defined (even empty) for envsubst to substitute it.
ENV UPSTREAM_DNS_SUFFIX=""
COPY web-client/nginx.default.conf /etc/nginx/templates/default.conf.template
COPY --from=builder /build/web-client/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
