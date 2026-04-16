# Planificación de la Aplicación Demo Android: \"Fuerza de Ventas Offline\"

Este documento detalla la planificación, arquitectura y fases de
desarrollo para la aplicación de demostración de **SyncBridge** en
Android. El objetivo de esta app es servir como prueba de concepto (PoC)
y herramienta de ventas para demostrar el valor de un enfoque
*offline-first* real.

## 1. Objetivo de la Aplicación

Crear una aplicación móvil simulada de \"Toma de Pedidos\" (Fuerza de
Ventas) que permita a los vendedores seguir creando registros sin
interrupciones, independientemente de si tienen conexión a internet o
no. La app demostrará visualmente cómo SyncBridge gestiona la cola, los
reintentos y los conflictos de forma transparente.

## 2. Stack Tecnológico (Android)

- **Lenguaje:** Kotlin.

- **Arquitectura:** MVVM (Model-View-ViewModel).

- **UI:** Jetpack Compose (recomendado para desarrollo rápido) o XML
  clásico.

- **Persistencia Local (App y Librería):** Room Database.

- **Inyección de Dependencias:** Hilt (opcional, pero recomendado).

- **Librería Principal:** syncbridge-core y syncbridge-room (integración
  local).

## 3. Funcionalidades Clave a Demostrar (Casos de Uso)

### A. Creación Transparente de Pedidos (Patrón Outbox)

- El usuario llena un formulario de pedido y presiona \"Guardar\".

- **El Efecto Wow:** La acción es inmediata. No hay *spinners* de carga
  infinitos ni errores de red. El pedido se guarda localmente y
  SyncBridge se encarga del resto.

### B. Simulador de Modo Avión (Dashboard Visual)

- Un *switch* o botón en la interfaz principal para forzar a la app a
  creer que está offline (Mock del NetworkMonitor).

- **Indicadores Visuales:** \* Un *badge* que cambia de Verde
  (\"Online\") a Naranja (\"Offline - Guardando en cola\").

  - Un contador en tiempo real de \"Transacciones Pendientes\"
    (syncBridge.observeQueueSize()).

### C. Sincronización y Prueba de Idempotencia

- Al desactivar el \"Modo Avión\", la app detecta la red y vacía la cola
  automáticamente.

- Visualización de los registros pasando de estado PENDING a SYNCED.

- **Demo de Idempotencia:** Simular un corte de red justo después de
  enviar el payload, forzando un reintento. El servidor mock responderá
  200 sin duplicar el pedido (gracias al X-Transaction-Id).

### D. Resolución de Conflictos (Error 409)

- Simular un escenario donde un pedido entra en conflicto (ej. el
  inventario del producto se agotó mientras el usuario estaba offline).

- Mostrar un cuadro de diálogo (UI) utilizando el Conflict Listener para
  que el usuario decida cómo proceder.

## 4. Arquitectura de la Demo

La app se dividirá en 3 capas principales:

1.  **Capa de UI (Presentación):** \* DashboardScreen: Muestra el estado
    de la red, tamaño de la cola y lista de pedidos.

    - CreateOrderScreen: Formulario simple (Cliente, Producto,
      Cantidad).

2.  **Capa de Lógica (ViewModels):** \* Conecta la UI con SyncBridge.
    Utiliza syncBridge.enqueue() y recolecta flujos (StateFlow) del
    estado de la transacción.

3.  **Capa de Mocking (Servidor Falso):**

    - Para evitar depender de un backend real durante la demo de ventas,
      se implementará un Interceptor de OkHttp que simule las respuestas
      del servidor (200 OK, 409 Conflict, 500 Error, y latencia
      artificial).

## 5. Fases de Desarrollo

### Fase 1: Estructura Base y UI (Semana 1)

- Configurar el proyecto Android con Jetpack Compose y Room.

- Diseñar las pantallas principales (Dashboard y Formulario de Pedidos).

- Implementar el OkHttp Interceptor para simular un servidor con
  latencia.

### Fase 2: Integración de SyncBridge (Semana 2)

- Inicializar SyncBridge en Application.kt pasando el RoomSyncAdapter.

- Conectar el formulario de pedidos al método syncBridge.enqueue().

- Mostrar el estado de la red usando syncBridge.networkState en el
  Dashboard.

### Fase 3: Casos de Uso Avanzados y Pulido (Semana 3)

- Implementar el simulador de caídas de red (botón \"Forzar Offline\").

- Implementar el Conflict Listener para capturar respuestas 409 del
  interceptor y mostrar un *Dialog* en Compose.

- Añadir logs visuales en pantalla para que los *stakeholders* vean las
  peticiones HTTP por debajo de la interfaz.

## 6. Guion de Demostración para Ventas

1.  **Inicio:** Abre la app. Muestra el estado \"Online\". Crea un
    pedido normal y muestra cómo se sincroniza al instante.

2.  **Corte de Red:** Activa el \"Modo Offline\" en la app.

3.  **Trabajo Fluido:** Crea 3 pedidos rápidamente. Haz énfasis en que
    la UI no se bloquea. Muestra el contador de \"Transacciones en Cola:
    3\".

4.  **Reconexión:** Desactiva el \"Modo Offline\". Muestra la consola de
    logs (o UI) viendo cómo las 3 transacciones se envían en *batch* de
    forma segura (cambiando a SYNCED).

5.  **Manejo de Errores:** Simula un error forzado 409 y muestra cómo la
    app no crashea, sino que presenta al usuario un diálogo amigable
    para resolver el conflicto.
