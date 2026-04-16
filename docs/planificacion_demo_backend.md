# Planificación del Backend: Servidor de Sincronización e Idempotencia

Este documento detalla la arquitectura, el contrato de API y las fases
de desarrollo para el Backend que dará soporte a las aplicaciones de
demostración (\"Fuerza de Ventas Offline\") de **SyncBridge**.

A diferencia de un backend CRUD tradicional, el objetivo principal aquí
es garantizar la **idempotencia**. En entornos de conectividad
intermitente, los clientes enviarán la misma petición varias veces
(reintentos) si no reciben confirmación. El servidor es la última línea
de defensa contra la duplicación de datos.

## 1. Objetivo del Backend

Proveer una API robusta que procese los pedidos (payloads JSON) enviados
por los clientes móviles (Android/iOS) y que implemente un registro de
transacciones para ignorar los reintentos de peticiones ya procesadas
con éxito, cumpliendo así con el patrón *Outbox* y las políticas de
SyncBridge.

## 2. Stack Tecnológico Sugerido

Para la PoC (Proof of Concept) y la demo, se sugiere un stack ligero y
de alta concurrencia:

- **Entorno:** Node.js con Express o NestJS (Alternativas:
  Python/FastAPI, Java/Spring Boot).

- **Base de Datos Principal:** PostgreSQL o MongoDB (para guardar los
  pedidos).

- **Caché/Log de Idempotencia:** Redis (ideal por su manejo nativo de
  TTL) o una tabla específica en PostgreSQL (idempotency_log).

## 3. El Contrato de Idempotencia (Core)

SyncBridge inyectará automáticamente tres cabeceras HTTP en cada
petición enviada al servidor. El backend **debe** leerlas
obligatoriamente:

1.  X-Transaction-Id: (UUID v4) Identificador único de la transacción
    generado por el cliente. **Es la clave principal.**

2.  X-Client-Timestamp: (Epoch ms) Fecha y hora en que se encoló la
    transacción en el dispositivo móvil.

3.  X-Attempt-Count: (Integer) Número de intento actual (1, 2, 3\...).
    Útil para logs y métricas.

## 4. Flujo de Trabajo del Servidor (Algoritmo)

Para cada endpoint que reciba peticiones mutables (POST, PUT, PATCH,
DELETE), se debe implementar un *Middleware* o interceptor con la
siguiente lógica exacta:

1.  **Interceptar:** Leer X-Transaction-Id de las cabeceras.

2.  **Verificar:** Buscar este ID en la tabla/colección idempotency_log.

3.  **Manejar Duplicados (Hit):**

    - Si el ID **existe** y su estado fue SUCCESS: Devolver código HTTP
      200/201 con el body de la respuesta original guardado en el log
      (NO volver a procesar el negocio).

    - Si el ID **existe** y su estado fue CONFLICT: Devolver código HTTP
      409 con la respuesta original.

4.  **Procesar (Miss):** \* Si el ID **no existe**, ejecutar la lógica
    de negocio real (ej. guardar el pedido, descontar stock).

5.  **Registrar:** Guardar el resultado en idempotency_log (ID, Status,
    Response Body, Timestamp).

6.  **Responder:** Enviar la respuesta HTTP al cliente móvil.

> **Mantenimiento:** Configurar un TTL (Time-To-Live) de 7 a 30 días
> para los registros del idempotency_log para evitar que la base de
> datos crezca infinitamente.

## 5. APIs a Implementar (Para la Demo)

### A. Crear Pedido

- **Endpoint:** POST /api/orders

- **Comportamiento Normal:** Recibe el JSON del pedido, guarda en BD,
  registra en idempotencia, responde 201 Created.

- **Comportamiento Reintento:** Detecta el X-Transaction-Id ya
  procesado, responde 200 OK (simulando que se acaba de crear, aunque es
  un caché).

### B. Generador de Conflictos (Mock 409)

- **Endpoint:** POST /api/orders/force-conflict (o simulado por un flag
  en el payload).

- **Comportamiento:** Simula una regla de negocio fallida (ej. \"Stock
  insuficiente\").

- **Respuesta:** Responde HTTP 409 Conflict con un body detallado:
  {\"error\": \"OUT_OF_STOCK\", \"message\": \"El producto X ya no está
  disponible\", \"offlineRef\": \"\...\"}. Esto disparará el Conflict
  Listener en las apps móviles.

## 6. Fases de Desarrollo

### Fase 1: Estructura y Middleware (Semana 1)

- Configuración del servidor y la base de datos.

- Creación de la tabla/esquema idempotency_log.

- **Entregable Crítico:** Desarrollo del *Middleware* de Idempotencia
  que intercepta y valida el X-Transaction-Id antes de llegar a los
  controladores.

### Fase 2: Endpoints de Negocio (Semana 2)

- Creación del endpoint POST /api/orders integrando el middleware.

- Desarrollo de lógica de validación de negocio.

- Creación de respuestas de prueba (Mocks) para conflictos lógicos (409
  Conflict) y errores internos (500 Internal Server Error).

### Fase 3: Pruebas de Estrés y End-to-End (Semana 3)

- Simulaciones locales: Enviar peticiones simultáneas con el mismo
  X-Transaction-Id para asegurar que no hay *Race Conditions* en la
  inserción del log de idempotencia.

- Pruebas de integración reales con las aplicaciones Android e iOS de
  demostración.

- Ajuste de *rate limits* para mitigar el efecto *Thundering Herd*
  (avalancha de peticiones) cuando muchos clientes se reconectan a
  internet simultáneamente.

## 7. Criterios de Aceptación para la Venta (Demo)

Para que la demo técnica sea un éxito frente a los *stakeholders*, el
backend debe poder demostrar a través de los *logs* de la consola:

1.  Petición inicial recibida (Procesada: ✅).

2.  Petición idéntica recibida un minuto después simulando timeout
    (Ignorada por idempotencia, respuesta cacheada devuelta: ♻️).

3.  Base de datos con exactamente **1** registro del pedido, demostrando
    que SyncBridge previene activamente la duplicidad.
