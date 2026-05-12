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
  && pnpm dlx openapi-typescript@latest /build/api/openapi.yaml -o src/api/types.ts
RUN pnpm build

# --- Runtime: nginx + static assets only ---
FROM nginx:1.27-alpine AS runtime
COPY web-client/nginx.default.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /build/web-client/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
