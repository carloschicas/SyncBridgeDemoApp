# Implementación Android — SyncBridge Demo ("Fuerza de Ventas Offline")

Checklist maestro de tareas para el desarrollo de la app Android de demostración.
Actualizar `[ ]` → `[x]` conforme se completen las tareas.

---

## 0. Configuración del Proyecto Base

### Estructura y herramientas
- [x] Crear proyecto Android nuevo en Android Studio con soporte Kotlin
- [x] Configurar `minSdk 26`, `targetSdk 35`, `compileSdk 35`
- [x] Activar Jetpack Compose en `build.gradle.kts` (BOM actualizado)
- [ ] Configurar `composeOptions` y `kotlinOptions` (JVM target 17)
- [x] Añadir `google()` y `mavenCentral()` en `settings.gradle.kts`
- [ ] Crear estructura de paquetes: `ui/`, `viewmodel/`, `data/`, `di/`, `mock/`

### Dependencias principales
- [x] Agregar Jetpack Compose (BOM + material3, navigation, lifecycle)
- [x] Agregar Room (`room-runtime`, `room-ktx`, kapt/ksp `room-compiler`)
- [x] Agregar Hilt (`hilt-android`, `hilt-compiler`, `hilt-navigation-compose`)
- [x] ~~Agregar OkHttp + Logging Interceptor~~ *(eliminado — SDK gestiona la red)*
- [x] Agregar `syncbridge-core` y `syncbridge-room` (Maven local o JAR)
- [x] Agregar Coroutines + Flow (`kotlinx-coroutines-android`)
- [ ] Verificar que el build base compila sin errores (`./gradlew assembleDebug`)

### Configuración de código
- [ ] Activar `strict` en `kotlinOptions` (`allWarningsAsErrors = false`, lint rules)
- [ ] Configurar `proguard-rules.pro` con reglas para Room y Hilt
- [ ] Crear `.editorconfig` con reglas de estilo Kotlin (ktlint)
- [ ] Configurar ktlint o Detekt para análisis estático

---

## 1. Fase 1 — Estructura Base y UI (Semana 1)

### 1.1 Capa de datos (Room)
- [x] Definir entidad `OrderEntity` (id UUID, cliente, producto, cantidad, status, createdAt)
- [x] Definir `OrderStatus` enum: `PENDING`, `SYNCING`, `SYNCED`, `CONFLICT`
- [x] Crear `OrderDao` con queries: insert, updateStatus, observeAll (Flow)
- [x] Crear `AppDatabase` con `@Database` y migrations vacías iniciales
- [x] Verificar schema exportado (`room.schemaLocation` en build.gradle)

### 1.2 Capa de red — gestionada por SyncBridge SDK
> **Nota (2026-04-17):** El paquete `data/network` (ApiService, MockServerInterceptor, NetworkModule) fue
> eliminado por completo. El SDK de SyncBridge gestiona su propia capa de red, cola e inyección de cabeceras.
> `syncbridge.json` apunta a `http://10.0.2.2:8080`. El estado de conectividad real se expone via
> `SyncBridge.getInstance().networkState: StateFlow<Boolean>`, observado en `DashboardViewModel`.

- [x] ~~Crear `ApiService`~~ *(eliminado — SDK gestiona la red)*
- [x] ~~Crear `OkHttpClient`~~ *(eliminado — SDK gestiona la red)*
- [x] ~~Configurar Retrofit~~ *(eliminado — SDK gestiona la red)*
- [x] Configurar `syncbridge.json` con `baseUrl = http://10.0.2.2:8080`

### 1.3 Pantalla Dashboard (`DashboardScreen`)
- [x] Crear Composable `DashboardScreen` con Scaffold base
- [x] Añadir badge de estado de red: indicador Verde ("Online") / Naranja ("Offline")
- [x] Añadir contador de "Transacciones Pendientes" (`pendingCount: Int`)
- [x] Añadir Switch/botón "Forzar Offline" visible en la UI
- [x] Añadir lista de pedidos recientes con estado visual por ítem
- [x] Añadir FAB o botón de navegación a `CreateOrderScreen`

### 1.4 Pantalla Crear Pedido (`CreateOrderScreen`)
- [x] Crear Composable `CreateOrderScreen` con formulario
- [x] Campos: Cliente (texto), Producto (texto o dropdown), Cantidad (numérico)
- [x] Botón "Guardar" que llama al ViewModel (sin spinner bloqueante)
- [x] Feedback inmediato al usuario (Snackbar o Toast "Pedido guardado localmente")
- [x] Validación de campos no vacíos

### 1.5 Navegación
- [x] Configurar `NavHost` con rutas `dashboard` y `create_order`
- [x] Pasar `NavController` a los Composables que lo necesiten

---

## 2. Fase 2 — Integración de SyncBridge (Semana 2)

### 2.1 Inicialización de SyncBridge
- [x] Crear `DemoApplication : Application()` y registrarla en `AndroidManifest.xml`
- [x] Instanciar `SyncBridge` en `DemoApplication.onCreate()` con `RoomSyncAdapter`
- [x] Pasar `AppDatabase` al adaptador correctamente
- [x] Configurar `SyncBridge` con la URL base del servidor (o del interceptor mock)
- [x] Exponer instancia de `SyncBridge` via Hilt (`@Singleton`)

### 2.2 ViewModel del Dashboard (`DashboardViewModel`)
- [x] Crear `DashboardViewModel` con inyección Hilt
- [x] Colectar `syncBridge.networkState` → exponer como `StateFlow<Boolean>` (`isOnline`)
- [x] Colectar `orderDao.observeAll()` → exponer como `StateFlow<List<OrderEntity>>`
- [x] Exponer `queueSize` (conteo PENDING en OrderDao) como `StateFlow<Int>`
- [x] Inyectar `ConflictManager` → exponer `conflictEvent: StateFlow<ConflictEvent?>` para UI

### 2.3 ViewModel de Crear Pedido (`OrderViewModel`)
- [x] Inyección Hilt con `OrderDao` y `SyncBridge`
- [x] Implementar `insertOrder(cliente, producto, cantidad)` que llama `syncBridge.enqueue(endpoint="/api/orders", payload=json)`
- [x] Generar UUID v4 para `X-Transaction-Id` por cada pedido nuevo
- [x] Observar `syncBridge.observeTransaction(txnId)` para actualizar Room (SYNCED / CONFLICT / FAILED)

### 2.4 Conectar UI con ViewModels
- [x] Conectar `DashboardScreen` a `DashboardViewModel` (collectAsStateWithLifecycle)
- [x] Conectar `CreateOrderScreen` a `OrderViewModel`
- [x] Badge de red refleja `SyncBridge.networkState` real (CONECTADO / DESCONECTADO)
- [x] Contador de pedidos se actualiza en tiempo real desde Room

---

## 3. Fase 3 — Casos de Uso Avanzados y Pulido (Semana 3)

### 3.1 Simulador de caídas de red
- [x] El toggle "Forzar Offline" del Dashboard actualiza `isOffline: StateFlow<Boolean>` en el ViewModel
- [ ] Al reactivar online, verificar que `SyncBridge` drena la cola automáticamente
- [ ] Verificar que los ítems de la lista pasan de `PENDING` → `SYNCING` → `SYNCED`

### 3.2 Conflict Listener (Error 409)
- [x] Implementar `ConflictListener` en `App.onCreate()` — emite al `ConflictManager` Singleton (Hilt)
- [x] `ConflictManager` expone `SharedFlow<ConflictEvent>` consumido por `DashboardViewModel`
- [x] Al recibir 409, mostrar `AlertDialog` en `DashboardScreen` con "Conflicto en transacción: [ID]"
- [x] Actualizar estado del pedido en Room a `CONFLICT` (via `observeTransaction()` en `OrderViewModel`)

### 3.3 Log visual en pantalla
- [x] Crear Composable `LiveLogPanel` (lista scrollable de eventos)
- [ ] Capturar logs de SyncBridge (callbacks o interceptor de logs) y emitirlos a un `StateFlow<List<LogEntry>>`
- [ ] Mostrar formato: `✅ SYNCED | txn=550e8400 | POST /api/orders | 201`
- [ ] Mostrar formato: `♻️ CACHED  | txn=550e8400 | reintento | 200`
- [ ] Mostrar formato: `⚠️ CONFLICT| txn=aaaa1111 | 409 stock agotado`
- [x] Añadir opción de colapsar/expandir el panel de logs

### 3.4 Pulido de UI para la demo de ventas
- [ ] Revisar colores y tipografía (Material3 theme personalizado)
- [ ] Animación suave en el badge Online/Offline (color transition)
- [ ] Animación en el contador de pendientes al incrementar/decrementar
- [ ] Asegurar que ninguna acción del usuario bloquea el hilo principal
- [ ] Probar el guion de demostración completo de principio a fin

---

## 4. CI/CD — GitHub Actions (Android)

### 4.1 Workflow de Build y Lint
- [ ] Crear `.github/workflows/android-ci.yml`
- [ ] Trigger: `push` a `main` y `pull_request` a `main`
- [ ] Job `build`: `actions/checkout` → `setup-java` (JDK 17) → `./gradlew assembleDebug`
- [ ] Job `lint`: `./gradlew lint` con `--continue` y upload de reporte HTML como artefacto
- [ ] Job `detekt` (o ktlint): `./gradlew detekt` para análisis estático
- [ ] Cachear Gradle dependencies con `actions/cache` (`.gradle/`, `~/.gradle/`)

### 4.2 Workflow de Tests
- [ ] Añadir job `unit-test`: `./gradlew testDebugUnitTest`
- [ ] Upload de reporte JUnit como artefacto
- [ ] Considerar matriz de JDK si se requiere compatibilidad amplia

### 4.3 Workflow de Release (APK de Demo)
- [ ] Crear `.github/workflows/android-release.yml`
- [ ] Trigger: tag `v*.*.*` (ej. `v1.0.0`)
- [ ] Build APK release: `./gradlew assembleRelease`
- [ ] Firmar APK con keystore almacenado en GitHub Secrets (`KEYSTORE_BASE64`, `KEY_ALIAS`, `KEY_PASSWORD`, `STORE_PASSWORD`)
- [ ] Subir APK firmado como Release Asset via `softprops/action-gh-release`
- [ ] Generar `CHANGELOG.md` automáticamente desde commits (ver sección 6)

---

## 5. Configuración de Tests

### 5.1 Tests unitarios (JVM)
- [ ] Configurar dependencias de test: JUnit 5, MockK, Coroutines Test
- [ ] Test unitario: `CreateOrderViewModelTest` — verifica que `saveOrder()` llama a `syncBridge.enqueue()`
- [ ] Test unitario: `DashboardViewModelTest` — verifica que el StateFlow de pendientes se actualiza
- [ ] Test unitario: `MockServerInterceptorTest` — verifica respuestas 201, 409 y IOException en offline
- [ ] Test unitario: generación correcta de UUID v4 por pedido

### 5.2 Tests de Room (JVM con in-memory DB)
- [ ] Configurar `TestCoroutineDispatcher` y `InstantTaskExecutorRule`
- [ ] Test de `OrderDao`: insert → query → verificar campos
- [ ] Test de `OrderDao`: actualización de status `PENDING` → `SYNCED`
- [ ] Test de `OrderDao`: `observeAll()` emite actualizaciones en tiempo real

### 5.3 Tests de integración / instrumentados (opcional para PoC)
- [ ] Configurar `hiltRule` en tests instrumentados si se usa Hilt
- [ ] Test end-to-end: crear pedido en offline → activar online → verificar status SYNCED en Room

### 5.4 Configuración de cobertura
- [ ] Activar JaCoCo en `build.gradle.kts` para métricas de cobertura
- [ ] Generar reporte HTML: `./gradlew jacocoTestReport`
- [ ] Umbral mínimo de cobertura: 60% (ajustable, es una PoC)

---

## 6. Manejo de Versiones y Changelog

### 6.1 Versionado semántico
- [x] Definir `versionName` y `versionCode` en `build.gradle.kts` del módulo `app`
- [ ] Convención: `versionName = "MAJOR.MINOR.PATCH"` (ej. `1.0.0`)
- [ ] Convención: `versionCode` = entero autoincremental (puede ser el número de build de CI)
- [ ] Documentar en `README.md` del módulo cómo incrementar versiones

### 6.2 Conventional Commits
- [ ] Adoptar formato `type(scope): descripción` para todos los commits del módulo Android
  - Tipos válidos: `feat`, `fix`, `refactor`, `test`, `chore`, `docs`, `ci`
  - Scopes sugeridos: `ui`, `viewmodel`, `data`, `mock`, `ci`, `deps`
- [ ] Configurar commitlint (opcional) o documentar la convención en `CONTRIBUTING.md`

### 6.3 CHANGELOG.md
- [x] Crear `syncbridge-demo-android/CHANGELOG.md` con estructura Keep a Changelog
- [ ] Secciones por versión: `[Unreleased]`, `[1.0.0]`, etc.
- [ ] Subsecciones: `Added`, `Changed`, `Fixed`, `Removed`
- [ ] Actualizar `[Unreleased]` antes de cada Release y renombrar a la versión al tagear
- [ ] Automatizar generación en el workflow de release (ej. `git-cliff` o `conventional-changelog`)

---

## Estado General

| Fase | Descripción | Estado |
|------|-------------|--------|
| 0 | Configuración del proyecto base | ⬜ Pendiente |
| 1 | Estructura Base y UI | ✅ Completada |
| 2 | Integración de SyncBridge SDK completa (red custom eliminada) | ✅ Completada |
| 3 | Casos de Uso Avanzados y Pulido | 🔄 En progreso |
| 4 | CI/CD GitHub Actions | ⬜ Pendiente |
| 5 | Configuración de Tests | ⬜ Pendiente |
| 6 | Versiones y Changelog | ⬜ Pendiente |
