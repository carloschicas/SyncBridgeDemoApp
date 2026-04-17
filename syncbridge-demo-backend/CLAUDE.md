# CLAUDE.md — syncbridge-demo-backend

Instrucciones para Claude Code al trabajar en el backend de SyncBridge.

## Stack

- Node.js 22 + Express + TypeScript
- PostgreSQL 16 (sin Redis — idempotencia via SQL puro)
- Docker Compose (dos servicios: `postgres` + `app`)

## Comandos principales

```bash
# Arrancar todo (primera vez: construye la imagen y crea las tablas)
docker compose up --build

# Arrancar en background
docker compose up -d

# Ver logs en tiempo real
docker compose logs -f app

# Parar y eliminar contenedores (conserva el volumen de datos)
docker compose down

# Parar y eliminar TODO (incluye datos de PostgreSQL)
docker compose down -v

# Acceder a la DB directamente
docker exec -it syncbridge_postgres psql -U syncbridge -d syncbridge_demo
```

## Variables de entorno

El archivo `.env` ya existe con valores listos para desarrollo local. No editar `.env.example` — ese es solo la plantilla.

## Estructura del proyecto

```
src/
├── config/env.ts               # Variables de entorno tipadas
├── db/pool.ts                  # Pool de conexión PostgreSQL (único punto de acceso a pg)
├── middleware/idempotency.ts   # Middleware central de idempotencia
├── controllers/
│   ├── createOrder.ts
│   └── forceConflict.ts
├── routes/orders.ts
├── services/idempotencyLog.ts  # Operaciones sobre la tabla idempotency_log
├── jobs/ttlCleanup.ts          # Limpieza periódica de registros expirados
├── types/index.ts
├── app.ts
└── index.ts
```

## Arquitectura de idempotencia

El middleware en `src/middleware/idempotency.ts` implementa el patrón central:

1. Lee `X-Transaction-Id` del header (UUID v4 requerido).
2. Intenta `INSERT ... ON CONFLICT DO NOTHING` en `idempotency_log` con status `PROCESSING`.
3. Si el INSERT retorna 1 fila → transacción nueva: ejecuta el controller, guarda resultado.
4. Si el INSERT retorna 0 filas → duplicado: devuelve la respuesta cacheada del log.

**No modificar este flujo** sin revisar `docs/planificacion_demo_backend.md` sección 4.

## Endpoints

| Método | Ruta | Comportamiento |
|--------|------|----------------|
| POST | `/api/orders` | Crea pedido (201). Reintento → 200 cacheado. |
| POST | `/api/orders/force-conflict` | Simula stock agotado (409). Reintento → 409 cacheado. |
| GET | `/health` | Health check del servidor. |

## Logs de consola esperados

```
✅ [PROCESSED]   POST /api/orders                  txn=550e8400  attempt=1  status=201
♻️  [CACHED]     POST /api/orders                  txn=550e8400  attempt=2  status=200
✅ [PROCESSED]   POST /api/orders/force-conflict   txn=aaaa1111  attempt=1  status=409
♻️  [CACHED]     POST /api/orders/force-conflict   txn=aaaa1111  attempt=2  status=409
```

Mantener el formato de iconos `✅` / `♻️` / `⏳` — es parte de la presentación.

## Convenciones de código

- TypeScript estricto (`strict: true`). No usar `any`.
- Queries SQL con parámetros posicionales (`$1`, `$2`), nunca interpolación de strings.
- Los controllers no importan `pg` directamente — solo via `src/db/pool.ts`.

## Documento de progreso

`docs/implementacion_backend.md` es el checklist de tareas. Actualizar `[ ]` → `[x]` conforme se completen y verificar las tareas de la **Fase 8** (tests con curl) antes de dar por finalizado el backend.
