# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM node:22-alpine AS builder

WORKDIR /app

COPY package.json tsconfig.json ./
RUN npm install

COPY src ./src
RUN npm run build

# ── Stage 2: Run ──────────────────────────────────────────────────────────────
FROM node:22-alpine AS runner

WORKDIR /app

ARG APP_VERSION=dev
ENV NODE_ENV=production \
    APP_VERSION=${APP_VERSION}

LABEL org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.title="syncbridge-demo-backend" \
      org.opencontainers.image.description="Demo backend for SyncBridge — idempotent order sync API"

COPY package.json ./
RUN npm install --omit=dev

COPY --from=builder /app/dist ./dist

EXPOSE 3000

CMD ["node", "dist/index.js"]
