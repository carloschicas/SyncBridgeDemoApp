# SyncBridge Demo App

Aplicaciones de demostración para **SyncBridge**, una librería que resuelve el problema de sincronización de datos en aplicaciones móviles con conectividad intermitente.

## El Problema que Demuestra

Los vendedores de campo trabajan en zonas con señal inestable. Al recuperar conexión, sus dispositivos reintentan las peticiones pendientes. Sin la protección adecuada, el servidor procesa el mismo pedido múltiples veces, generando duplicados en la base de datos.

**SyncBridge + este backend** demuestran que eso no ocurre: sin importar cuántos reintentos lleguen, la base de datos siempre tiene exactamente **1** registro por transacción.

## Proyectos en este Repositorio

| Proyecto | Estado | Descripción |
|----------|--------|-------------|
| `syncbridge-demo-backend` | ✅ Implementado | Servidor Node.js con API de idempotencia |
| `syncbridge-demo-android` | Planificado | App Android "Fuerza de Ventas" (Kotlin + MVVM) |
| `syncbridge-demo-ios` | Planificado | App iOS "Fuerza de Ventas" (Swift + SwiftUI) |

## Inicio Rápido (Backend)

**Requisitos:** Docker Desktop instalado y corriendo.

```bash
cd syncbridge-demo-backend
docker compose up --build
```

El servidor arranca en `http://localhost:3000`.

## Demostración del Patrón de Idempotencia

### 1. Primera petición — se procesa normalmente (201)

```bash
curl -X POST http://localhost:3000/api/orders \
  -H "Content-Type: application/json" \
  -H "X-Transaction-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "X-Client-Timestamp: 1713264000000" \
  -H "X-Attempt-Count: 1" \
  -d '{
    "customerName": "Distribuidora Acme",
    "productName": "Widget Pro X",
    "quantity": 10,
    "totalAmount": 299.90
  }'
```

Respuesta `201 Created`:
```json
{
  "orderId": "a1b2c3d4-...",
  "transactionId": "550e8400-...",
  "status": "CONFIRMED",
  "source": "processed"
}
```

### 2. Reintento con el mismo ID — respuesta cacheada (200)

```bash
# Mismo X-Transaction-Id, X-Attempt-Count: 2
curl -X POST http://localhost:3000/api/orders \
  -H "Content-Type: application/json" \
  -H "X-Transaction-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "X-Client-Timestamp: 1713264000000" \
  -H "X-Attempt-Count: 2" \
  -d '{ "customerName": "Distribuidora Acme", "productName": "Widget Pro X", "quantity": 10, "totalAmount": 299.90 }'
```

Respuesta `200 OK` — misma respuesta, sin re-procesar:
```json
{
  "orderId": "a1b2c3d4-...",
  "source": "cached"
}
```

### 3. Verificar que solo existe 1 pedido en la base de datos

```bash
docker exec syncbridge_postgres \
  psql -U syncbridge -d syncbridge_demo -c "SELECT COUNT(*) FROM orders;"
```

```
 count
-------
     1
```

### Logs del servidor durante la demo

```
✅ [PROCESSED]   POST /api/orders   txn=550e8400  attempt=1  status=201
♻️  [CACHED]     POST /api/orders   txn=550e8400  attempt=2  status=200
♻️  [CACHED]     POST /api/orders   txn=550e8400  attempt=3  status=200
```

## Documentación

| Archivo | Descripción |
|---------|-------------|
| [docs/planificacion_demo_backend.md](docs/planificacion_demo_backend.md) | Arquitectura y contrato de API del backend |
| [docs/planificacion_demo_android.md](docs/planificacion_demo_android.md) | Plan de la app Android |
| [docs/planificacion_demo_ios.md](docs/planificacion_demo_ios.md) | Plan de la app iOS |
| [docs/implementacion_backend.md](docs/implementacion_backend.md) | Checklist de progreso del backend |
| [syncbridge-demo-backend/README.md](syncbridge-demo-backend/README.md) | Documentación técnica del backend |
