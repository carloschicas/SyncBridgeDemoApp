# Planificación de la Aplicación Demo iOS: \"Fuerza de Ventas Offline\"

Este documento detalla la planificación, arquitectura y fases de
desarrollo para la aplicación de demostración de **SyncBridge** en iOS.
Al igual que su contraparte en Android, esta app sirve como prueba de
concepto (PoC) y herramienta visual para que los *stakeholders*
experimenten de primera mano el valor de una arquitectura
*offline-first*.

## 1. Objetivo de la Aplicación

Crear una aplicación nativa para iOS de \"Toma de Pedidos\" (Fuerza de
Ventas) que permita a los usuarios seguir operando de forma continua sin
depender de la conectividad a internet. La aplicación ilustrará cómo
SyncBridge gestiona silenciosamente la persistencia, la cola de envíos y
la resolución de conflictos cuando la red fluctúa.

## 2. Stack Tecnológico (iOS)

- **Lenguaje:** Swift 5.9+

- **Arquitectura:** MVVM (Model-View-ViewModel) con concurrencia moderna
  (async/await).

- **UI:** SwiftUI (ideal para iteraciones rápidas y estado reactivo).

- **Persistencia Local:** Core Data (mediante el adaptador
  CoreDataSyncAdapter proveído por la librería).

- **Gestión de Paquetes:** Swift Package Manager (SPM).

- **Librería Principal:** SyncBridge (importado vía SPM).

## 3. Funcionalidades Clave a Demostrar (Casos de Uso)

### A. Creación Inmediata de Pedidos (Patrón Outbox)

- El usuario completa los datos de un cliente y producto, y pulsa
  \"Guardar\".

- **El Efecto Wow:** La interfaz no se bloquea esperando una respuesta
  HTTP. La escritura es local e instantánea, delegando el envío a la
  librería (uso de syncBridge.enqueue()).

### B. Simulador de Modo Avión (Dashboard Visual)

- Un *Toggle* en la vista principal para desconectar la app lógicamente
  (Mock del NWPathMonitor).

- **Indicadores Reactivos en SwiftUI:**

  - Un *banner* o *badge* de estado (\"🟢 Conectado\" vs \"🟠 Modo
    Offline - Guardando en local\").

  - Un contador animado mostrando el número de transacciones en cola.

### C. Sincronización y Prueba de Idempotencia

- Al restablecer la conexión desde el simulador, el Sync Engine se
  activa inmediatamente en *foreground* (prioridad alta en iOS).

- Visualización de las transacciones pasando de PENDING a SYNCED.

- **Demo de Idempotencia:** Simulación de un *timeout* de red. El
  servidor interceptado responderá exitosamente al reintento sin crear
  registros duplicados, evaluando el UUID en X-Transaction-Id.

### D. Resolución de Conflictos (Error 409)

- Forzar un error lógico de negocio (ej. límite de crédito excedido o
  falta de stock) mientras se estaba offline.

- Presentar un .alert() o un *Sheet* en SwiftUI gestionado por el
  Conflict Listener global o el observador específico de la transacción.

## 4. Arquitectura de la Demo

La app se organiza en 3 capas, usando patrones idiomáticos de Swift:

1.  **Capa de UI (Vistas SwiftUI):**

    - DashboardView: Pantalla resumen con el Toggle de conexión, listado
      de transacciones pendientes e historial de pedidos.

    - OrderFormView: Formulario nativo para capturar la transacción.

2.  **Capa de Lógica (ViewModels):**

    - Controladores de vista marcados con \@MainActor y conforme a
      ObservableObject o usando la macro \@Observable (iOS 17+).

    - Transforman el AsyncStream de estados de SyncBridge en propiedades
      publicadas para que SwiftUI se redibuje automáticamente.

3.  **Capa de Mocking (Red Simulada):**

    - A diferencia de Android (donde se usa OkHttp), en iOS se
      implementará un URLProtocol custom.

    - Este MockURLProtocol interceptará las peticiones HTTP de
      URLSession que hace SyncBridge, simulando tiempos de respuesta
      (latencia), caídas de red y devolviendo códigos de estado variados
      (200, 409, 500) según la configuración de la demo.

## 5. Fases de Desarrollo

### Fase 1: Setup, Core Data y Vistas (Semana 1)

- Configuración del proyecto en Xcode y diseño del NSPersistentContainer
  de Core Data.

- Desarrollo de las vistas en SwiftUI (DashboardView y OrderFormView).

- Implementación de la capa de Mock de red mediante URLProtocol.

### Fase 2: Integración de SyncBridge (Semana 2)

- Importar SyncBridge mediante SPM y configurar el archivo
  SyncBridge.plist.

- Inicializar SyncBridge en el ciclo de vida de la app (@main /
  AppDelegate).

- Conectar las acciones de guardar en el ViewModel con la librería (try
  await syncBridge.enqueue(\...)).

### Fase 3: Casos Edge y Refinamiento (Semana 3)

- Implementar el observador de estado de red simulado para que SwiftUI
  reaccione a los cortes.

- Procesar notificaciones de conflicto
  (NotificationCenter.default.publisher) para disparar las alertas de la
  interfaz.

- Asegurar que el comportamiento asíncrono y los *refreshes* de UI se
  ejecuten de manera fluida y sin saltos visuales.

## 6. Guion de Demostración para Ventas (Estandarizado)

*Para asegurar un mensaje coherente, el flujo de ventas es idéntico al
de Android.*

1.  **Inicio Seguro:** Mostrar la app en estado normal (\"Online\").
    Enviar un pedido y demostrar la inmediatez.

2.  **Caída de Red:** Activar el \"Modo Offline\" mediante el *Toggle*
    en la UI.

3.  **Flujo de Trabajo Ininterrumpido:** Crear 3 a 5 pedidos rápidamente
    en el formulario. Subrayar la ausencia de interrupciones o mensajes
    de error por falta de internet.

4.  **Reconexión Automática:** Desactivar el \"Modo Offline\". La UI
    debe mostrar el procesamiento de los lotes (batch) vaciando la cola
    de PENDING a SYNCED progresivamente.

5.  **Robustez ante Errores:** Demostrar cómo un error forzado
    (Simulación de un 409 Conflict vía nuestro URLProtocol) no detiene
    la aplicación, sino que notifica al vendedor para que decida qué
    hacer, demostrando que no se pierden datos.
