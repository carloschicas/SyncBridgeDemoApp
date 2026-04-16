# syncbridge-demo-backend

Servidor de demostración para **SyncBridge**. Implementa un API REST con **idempotencia garantizada** para entornos de conectividad intermitente: sin importar cuántas veces un cliente móvil reintente una petición, el servidor la procesa exactamente una vez.

## Requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (incluye Docker Compose)

No se necesita Node.js instalado localmente.

## Arranque

```bash
# Clonar y entrar al directorio
cd syncbridge-demo-backend

# Construir imagen y levantar servicios (primera vez: ~1-2 min)
docker compose up --build

# El servidor está listo cuando aparezca:
# SyncBridge Demo Backend
# Listening on http://0.0.0.0:3000
```

En ejecuciones posteriores (imagen ya construida):

```bash
docker compose up
```

## Endpoints

### `POST /api/orders`

Crea un nuevo pedido. Si el `X-Transaction-Id` ya fue procesado, devuelve la respuesta original cacheada sin volver a ejecutar la lógica de negocio.

**Headers requeridos (inyectados por SyncBridge):**

| Header | Tipo | Descripción |
|--------|------|-------------|
| `X-Transaction-Id` | UUID v4 | Identificador único de la transacción |
| `X-Client-Timestamp` | Epoch ms | Timestamp del encolado en el dispositivo |
| `X-Attempt-Count` | Integer | Número de intento (1, 2, 3...) |

**Body:**
```json
{
  "customerName": "Distribuidora Acme",
  "productName": "Widget Pro X",
  "quantity": 10,
  "totalAmount": 299.90
}
```

**Respuestas:**

| Código | Cuándo | `source` |
|--------|--------|----------|
| `201 Created` | Primera vez que llega el `X-Transaction-Id` | `"processed"` |
| `200 OK` | Reintento con el mismo `X-Transaction-Id` | `"cached"` |
| `400 Bad Request` | Header `X-Transaction-Id` ausente o inválido | — |

---

### `POST /api/orders/force-conflict`

Simula una regla de negocio fallida (stock agotado). Útil para demostrar el flujo de conflicto en las apps móviles.

**Respuesta `409 Conflict`:**
```json
{
  "error": "OUT_OF_STOCK",
  "message": "El producto solicitado ya no está disponible.",
  "offlineRef": "<X-Transaction-Id>",
  "source": "processed"
}
```

Los reintentos con el mismo `X-Transaction-Id` devuelven el mismo 409 cacheado (`source: "cached"`).

---

### `GET /health`

```json
{ "status": "ok", "timestamp": "2026-04-16T12:00:00.000Z" }
```

## Pruebas con curl

### Flujo completo de idempotencia

```bash
# 1. Primera petición — procesa y guarda en DB (201)
curl -s -X POST http://localhost:3000/api/orders \
  -H "Content-Type: application/json" \
  -H "X-Transaction-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "X-Client-Timestamp: 1713264000000" \
  -H "X-Attempt-Count: 1" \
  -d '{"customerName":"Acme","productName":"Widget","quantity":2,"totalAmount":59.90}' | jq .

# 2. Reintento — devuelve respuesta cacheada (200)
curl -s -X POST http://localhost:3000/api/orders \
  -H "Content-Type: application/json" \
  -H "X-Transaction-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "X-Client-Timestamp: 1713264000000" \
  -H "X-Attempt-Count: 2" \
  -d '{"customerName":"Acme","productName":"Widget","quantity":2,"totalAmount":59.90}' | jq .

# 3. Verificar: solo 1 pedido en la DB
docker exec syncbridge_postgres \
  psql -U syncbridge -d syncbridge_demo -c "SELECT id, customer_name, status, created_at FROM orders;"
```

### Flujo de conflicto

```bash
# 1. Forzar 409 (primera vez)
curl -s -X POST http://localhost:3000/api/orders/force-conflict \
  -H "Content-Type: application/json" \
  -H "X-Transaction-Id: aaaa1111-e29b-41d4-a716-446655440000" \
  -H "X-Client-Timestamp: 1713264000000" \
  -H "X-Attempt-Count: 1" \
  -d '{"customerName":"Beta Corp","productName":"Out-of-stock Item","quantity":1,"totalAmount":10.00}' | jq .

# 2. Reintento — mismo 409 cacheado
curl -s -X POST http://localhost:3000/api/orders/force-conflict \
  -H "Content-Type: application/json" \
  -H "X-Transaction-Id: aaaa1111-e29b-41d4-a716-446655440000" \
  -H "X-Client-Timestamp: 1713264000000" \
  -H "X-Attempt-Count: 2" \
  -d '{}' | jq .
```

## Arquitectura

```
src/
├── index.ts                  # Entry point — arranca HTTP server + TTL job
├── app.ts                    # Express factory (middlewares globales, rutas)
├── config/
│   └── env.ts                # Validación de variables de entorno al arranque
├── db/
│   └── pool.ts               # pg.Pool singleton (max 10 conexiones)
├── middleware/
│   └── idempotency.ts        # Middleware core — claim / replay / pass-through
├── services/
│   └── idempotencyLog.ts     # Queries a idempotency_log (claimTransaction, updateTransaction...)
├── controllers/
│   ├── createOrder.ts        # Lógica de negocio: INSERT en tabla orders
│   └── forceConflict.ts      # Retorna 409 fijo (sin lógica de negocio)
├── routes/
│   └── orders.ts             # Router Express con middleware aplicado
├── jobs/
│   └── ttlCleanup.ts         # setInterval que elimina registros expirados del log
└── types/
    └── index.ts              # Interfaces TypeScript compartidas
```

### Algoritmo del Middleware de Idempotencia

```
1. Leer X-Transaction-Id → validar UUID v4 (400 si falta)
2. INSERT idempotency_log con status=PROCESSING (ON CONFLICT DO NOTHING)
   ├─ Éxito (1 fila) → nueva transacción:
   │    ├─ Monkeypatch de res.json() para capturar el body de respuesta
   │    ├─ next() → controller ejecuta lógica de negocio
   │    └─ UPDATE log: status=SUCCESS/CONFLICT + response guardada
   └─ Sin fila (conflicto) → transacción ya conocida:
        ├─ PROCESSING → 409 + Retry-After: 2 (request concurrente)
        └─ SUCCESS/CONFLICT → replay de status + body cacheado (♻️)
```

### Race Conditions

La atomicidad de `INSERT ... ON CONFLICT DO NOTHING` en PostgreSQL garantiza que, bajo cualquier nivel de concurrencia, exactamente **un** request gana la carrera y procesa la transacción. No se usan mutexes, locks de aplicación ni Redis.

### TTL del log de idempotencia

Los registros en `idempotency_log` expiran a los **30 días** (configurable via `IDEMPOTENCY_TTL_DAYS`). Un job interno en Node.js ejecuta el cleanup cada hora (configurable via `TTL_CLEANUP_INTERVAL_MS`).

## Variables de Entorno

| Variable | Valor por defecto | Descripción |
|----------|-------------------|-------------|
| `PORT` | `3000` | Puerto HTTP del servidor |
| `NODE_ENV` | `production` | Entorno de ejecución |
| `DATABASE_URL` | *(construido por docker-compose)* | Connection string de PostgreSQL |
| `POSTGRES_USER` | `syncbridge` | Usuario de la DB |
| `POSTGRES_PASSWORD` | `syncbridge_secret` | Contraseña de la DB |
| `POSTGRES_DB` | `syncbridge_demo` | Nombre de la DB |
| `IDEMPOTENCY_TTL_DAYS` | `30` | Días que vive un registro en el log |
| `TTL_CLEANUP_INTERVAL_MS` | `3600000` | Intervalo del job de limpieza (ms) |

## Comandos de Docker

```bash
# Ver logs del servidor en tiempo real
docker compose logs -f app

# Ver logs de PostgreSQL
docker compose logs -f postgres

# Acceder a la DB con psql
docker exec -it syncbridge_postgres psql -U syncbridge -d syncbridge_demo

# Parar servicios (conserva datos)
docker compose down

# Parar y limpiar TODO (datos incluidos)
docker compose down -v

# Reconstruir solo la imagen de la app
docker compose build app
```

## Consultas útiles en psql

```sql
-- Ver todos los pedidos
SELECT id, customer_name, product_name, quantity, status, created_at FROM orders;

-- Ver el log de idempotencia
SELECT transaction_id, status, response_status, endpoint, attempt_count, expires_at
FROM idempotency_log
ORDER BY created_at DESC;

-- Contar pedidos (debe ser igual al número de transacciones únicas, no al de reintentos)
SELECT COUNT(*) FROM orders;
```
