# Implementación: syncbridge-demo-backend

Documento de seguimiento del progreso de implementación del backend de demostración para SyncBridge.

**Inicio:** 2026-04-16  
**Stack:** Node.js 22 + Express + TypeScript + PostgreSQL 16 + Docker

---

## Fase 1 — Estructura y Configuración del Proyecto

- [x] Crear `docs/implementacion_backend.md` (este documento)
- [x] Crear `syncbridge-demo-backend/package.json`
- [x] Crear `syncbridge-demo-backend/tsconfig.json`
- [x] Crear `syncbridge-demo-backend/Dockerfile` (multi-stage: builder + runner)
- [x] Crear `syncbridge-demo-backend/docker-compose.yml` (servicios: postgres + app)
- [x] Crear `syncbridge-demo-backend/.env.example`
- [x] Crear `syncbridge-demo-backend/.env` (listo para usar)
- [x] Crear `syncbridge-demo-backend/.dockerignore`

## Fase 2 — Base de Datos

- [x] Crear `syncbridge-demo-backend/db/init.sql`
  - Tabla `orders` (con UNIQUE en transaction_id)
  - Tabla `idempotency_log` (con PRIMARY KEY en transaction_id, status PROCESSING/SUCCESS/CONFLICT)
  - Índice en `expires_at` para limpieza eficiente por TTL

## Fase 3 — Infraestructura de la App

- [x] Crear `src/config/env.ts` — validación de variables de entorno al arranque
- [x] Crear `src/db/pool.ts` — singleton de `pg.Pool`
- [x] Crear `src/types/index.ts` — interfaces TypeScript (Order, IdempotencyRecord, etc.)

## Fase 4 — Servicio de Idempotencia

- [x] Crear `src/services/idempotencyLog.ts`
  - `claimTransaction()` — INSERT atómico con `ON CONFLICT DO NOTHING`
  - `findTransaction()` — buscar registro existente
  - `updateTransaction()` — actualizar status y response tras procesar
  - `deleteExpired()` — eliminar registros con `expires_at < NOW()`

## Fase 5 — Middleware Core

- [x] Crear `src/middleware/idempotency.ts`
  - Validar cabeceras `X-Transaction-Id`, `X-Client-Timestamp`, `X-Attempt-Count`
  - Flujo: claim → (nuevo) interceptar res.json() + next() → (existente) replay o PROCESSING
  - Logs de consola: `✅ [PROCESSED]` y `♻️ [CACHED]`

## Fase 6 — Endpoints de Negocio

- [x] Crear `src/controllers/createOrder.ts` — lógica POST /api/orders (guarda en DB, retorna 201)
- [x] Crear `src/controllers/forceConflict.ts` — retorna 409 OUT_OF_STOCK directo
- [x] Crear `src/routes/orders.ts` — monta ambos endpoints con el middleware

## Fase 7 — Ensamblaje y Arranque

- [x] Crear `src/app.ts` — Express factory (JSON parser, request logger, rutas)
- [x] Crear `src/jobs/ttlCleanup.ts` — limpieza periódica de idempotency_log expirados
- [x] Crear `src/index.ts` — entry point: arranca server + inicia TTL job

## Fase 8 — Verificación

- [ ] Ejecutar `docker compose up --build` sin errores
- [ ] Test 1: POST /api/orders con UUID nuevo → respuesta 201 (source: "processed")
- [ ] Test 2: POST /api/orders con mismo UUID → respuesta 200 (source: "cached")
- [ ] Test 3: POST /api/orders/force-conflict → respuesta 409
- [ ] Test 4: Reintento de force-conflict con mismo UUID → respuesta 409 (source: "cached")
- [ ] Test 5: Verificar que DB tiene exactamente 1 registro en `orders` tras reintentos

---

## Contrato de API

### Headers SyncBridge (requeridos en todas las mutaciones)
```
X-Transaction-Id:    <UUID v4>    — clave de idempotencia
X-Client-Timestamp: <epoch ms>   — timestamp del cliente
X-Attempt-Count:    <integer>    — número de intento (1, 2, 3...)
```

### POST /api/orders
- **Primera vez:** `201 Created` con `source: "processed"`
- **Reintento:**   `200 OK` con body cacheado y `source: "cached"`

### POST /api/orders/force-conflict
- **Primera vez:** `409 Conflict` con `error: "OUT_OF_STOCK"`
- **Reintento:**   `409 Conflict` con body cacheado y `source: "cached"`

---

## Logs de Consola (Demo)
```
✅ [PROCESSED]  POST /api/orders                 txn=550e8400  attempt=1  status=201
♻️  [CACHED]    POST /api/orders                 txn=550e8400  attempt=2  status=200
✅ [PROCESSED]  POST /api/orders/force-conflict  txn=aaaa1111  attempt=1  status=409
♻️  [CACHED]    POST /api/orders/force-conflict  txn=aaaa1111  attempt=2  status=409
[TTL-CLEANUP] Deleted 3 expired idempotency records
```
