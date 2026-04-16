# Changelog

All notable changes to **syncbridge-demo-backend** are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> **Commit tracking convention:** notable entries reference the commit that introduced them  
> as `` [`abc1234`](https://github.com/WhatDaCode/SyncBridgeDemoApp/commit/<full-sha>) ``.  
> The GitHub Release page always appends the **full commit log** for the tagged range automatically.

---

## [Unreleased]

## [1.0.0] - 2026-04-16

### Added
- Idempotent order creation endpoint (`POST /api/orders`) — returns `201` on first call, cached `200` on retries [`feat: add POST /api/orders with idempotency middleware`]
- Force-conflict simulation endpoint (`POST /api/orders/force-conflict`) — returns `409 OUT_OF_STOCK`, also cached on retry [`feat: add force-conflict endpoint`]
- SyncBridge middleware (`src/middleware/idempotency.ts`) validating `X-Transaction-Id`, `X-Client-Timestamp`, and `X-Attempt-Count` headers [`feat: idempotency middleware with header validation`]
- Atomic idempotency claim via `INSERT ... ON CONFLICT DO NOTHING` — no distributed locks required [`feat: atomic claim in idempotencyLog service`]
- TTL-based cleanup job (`src/jobs/ttlCleanup.ts`) that purges expired records from `idempotency_log` [`feat: TTL cleanup job`]
- PostgreSQL 16 schema: `orders` table and `idempotency_log` table with index on `expires_at` [`feat: db init.sql schema`]
- Health check endpoint (`GET /health`) returning `status`, `version`, and `timestamp` [`feat: health endpoint with version`]
- Multi-stage Docker build (builder + runner on `node:22-alpine`) [`chore: Dockerfile multi-stage build`]
- Docker Compose setup with `postgres` + `app` services, health-checked dependency, and persistent volume [`chore: docker-compose setup`]
- Environment variable validation at startup via `src/config/env.ts` [`chore: env config validation`]
- Console log format with `✅ [PROCESSED]` / `♻️ [CACHED]` icons for demo visibility [`chore: demo log format`]

---

[Unreleased]: https://github.com/WhatDaCode/SyncBridgeDemoApp/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/WhatDaCode/SyncBridgeDemoApp/releases/tag/v1.0.0
