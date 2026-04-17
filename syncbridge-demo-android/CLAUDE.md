# CLAUDE.md — syncbridge-demo-android

Instrucciones para Claude Code al trabajar en la app Android de SyncBridge.

## Regla crítica: UI

**Antes de crear o modificar cualquier elemento de UI, leer `docs/DESIGN.md`.**

Este documento define el sistema de diseño completo de la app ("The Authoritative Anchor"): paleta de colores, tipografía, superficies, sombras y componentes. Toda decisión visual debe seguirlo estrictamente. No improvisar colores, bordes ni estilos.

## Stack tecnológico

- **Lenguaje:** Kotlin
- **Arquitectura:** MVVM
- **UI:** Jetpack Compose
- **Persistencia local:** Room Database
- **Inyección de dependencias:** Hilt
- **Red:** SyncBridge SDK (gestiona cola, reintentos e inyección de cabeceras — no hay OkHttp/Retrofit en la app)

## Estructura del proyecto

```
app/src/main/java/com/syncbridge/demo/
├── data/
│   └── local/          # Room: OrderEntity, OrderDao, AppDatabase
├── di/                 # Módulos Hilt: DatabaseModule, SyncBridgeModule, ConflictManager
├── presentation/
│   ├── dashboard/      # DashboardScreen.kt, DashboardViewModel.kt
│   └── create_order/   # CreateOrderScreen.kt (usa OrderViewModel)
├── App.kt              # Application class — inicializa SyncBridge con conflictListener
└── MainActivity.kt
```

## Pantallas principales

| Pantalla | Archivo | Responsabilidad |
|----------|---------|-----------------|
| Dashboard | `presentation/dashboard/DashboardScreen.kt` | Estado de red, cola de pedidos, lista de transacciones |
| Crear pedido | `presentation/create_order/CreateOrderScreen.kt` | Formulario: Cliente, Producto, Cantidad |

## Convenciones de código

- No usar `any` ni tipos sin tipado explícito en Kotlin.
- Los ViewModels exponen estado via `StateFlow`, nunca `LiveData`.
- Los Composables no acceden directamente a Room ni al SDK — solo consumen estado del ViewModel.
- El SDK SyncBridge maneja la red; la app no instancia OkHttpClient ni Retrofit.
- Los conflictos (409) se propagan via `ConflictManager` → `DashboardViewModel` → `DashboardScreen`.

## Casos de uso a demostrar

1. **Patrón Outbox:** guardar pedido localmente → sincronizar en background.
2. **Simulador offline:** switch en UI que fuerza modo sin red.
3. **Cola visual:** contador en tiempo real de transacciones pendientes.
4. **Idempotencia:** reintento no duplica el pedido (header `X-Transaction-Id`).
5. **Conflicto 409:** dialog de resolución cuando el inventario se agotó.

## Referencia

Ver `docs/planificacion_demo_android.md` para el plan completo de fases y el guion de la demo de ventas.
