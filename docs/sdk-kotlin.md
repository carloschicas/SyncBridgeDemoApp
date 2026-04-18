# SyncBridge Android SDK

**Package**: `io.syncbridge`  
**Min SDK**: API 23 (Android 6.0)  
**Language**: Kotlin 1.9+

---

## Dependencias

### Requeridas

Agrega en el `build.gradle.kts` de tu módulo app:

```kotlin
dependencies {
    // Coroutines — requerido por la API pública (Flow, StateFlow, suspend)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // OkHttp — cliente HTTP por defecto del SDK
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // WorkManager — requerido para sync en background
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
```

### Opcionales — Adapter Room (recomendado)

Solo si usas el `RoomSyncAdapter` incluido como referencia:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
}

dependencies {
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
}
```

> Si implementas tu propio `StorageAdapter` con otra BD (SQLDelight, Realm, etc.), Room **no es necesario**.

### Versiones mínimas compatibles

| Dependencia | Versión mínima | Notas |
|---|---|---|
| `kotlinx-coroutines` | `1.7.x` | Core + Android |
| `com.squareup.okhttp3:okhttp` | `4.12.0` | Cliente HTTP por defecto |
| `androidx.work:work-runtime-ktx` | `2.9.0` | Para `SyncWorker` |
| `androidx.room:room-runtime` | `2.6.1` | Solo con `RoomSyncAdapter` |
| Kotlin | `1.9+` | |
| Android `compileSdk` | `34` | |
| Android `minSdk` | `23` | |
| Java | `17` | `jvmTarget = "17"` |

---

## Setup

### 1. Configuración — `assets/syncbridge.json`

Coloca este archivo en `src/main/assets/syncbridge.json`:

```json
{
  "baseUrl": "https://api.example.com",
  "environment": "production",
  "maxRetries": 3,
  "backoffStrategy": "exponential",
  "initialDelayMs": 1000,
  "maxDelayMs": 30000,
  "timeoutMs": 15000,
  "requiresUnmetered": false,
  "maxPayloadSizeKb": 512,
  "batchSize": 10,
  "defaultTtlSeconds": 86400,
  "defaultPriority": "NORMAL",
  "compressPayloads": false,
  "logLevel": "WARN"
}
```

| Campo | Tipo | Default | Descripción |
|---|---|---|---|
| `baseUrl` | String | — | **Requerido.** URL base del servidor |
| `maxRetries` | Int | `3` | Intentos máximos por transacción |
| `initialDelayMs` | Long | `1000` | Delay inicial de backoff (ms) |
| `maxDelayMs` | Long | `30000` | Delay máximo de backoff (ms) |
| `timeoutMs` | Long | `15000` | Timeout por request (ms) |
| `requiresUnmetered` | Boolean | `false` | Si `true`, solo sincroniza en WiFi |
| `maxPayloadSizeKb` | Int | `512` | Tamaño máximo de payload |
| `batchSize` | Int | `10` | Transacciones por ciclo de sync |
| `defaultTtlSeconds` | Int | `86400` | TTL por defecto (24h) |
| `defaultPriority` | String | `"NORMAL"` | Prioridad por defecto (`LOW`/`NORMAL`/`HIGH`) |
| `compressPayloads` | Boolean | `false` | Comprime payloads con gzip+base64 |

---

### 2. Inicialización — `Application.onCreate()`

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SyncBridge.initialize(this) {
            storageAdapter(RoomSyncAdapter(SyncBridgeDatabase.getInstance(this@MyApp).syncDao()))
            authTokenProvider { myTokenManager.getAccessToken() }
            conflictListener { event ->
                Log.w("Sync", "Conflict on ${event.transaction.id}: ${event.serverResponse}")
            }
        }
    }
}
```

---

## SyncBridge

Punto de entrada único. Singleton inicializado una sola vez.

```kotlin
object SyncBridge {
    fun initialize(context: Context, block: SyncBridgeBuilder.() -> Unit)
    fun getInstance(): SyncBridge

    val networkState: StateFlow<Boolean>

    suspend fun enqueue(
        endpoint: String,
        payload: String,
        metadata: Map<String, String>? = null,
        httpMethod: HttpMethod = HttpMethod.POST,
        priority: Priority = Priority.NORMAL,
        groupId: String? = null,
        ttlSeconds: Int? = null,
        headers: Map<String, String>? = null
    ): String

    fun observeTransaction(txnId: String): Flow<TxnState>
    suspend fun exportQueue(): String
}
```

### `enqueue()`

Encola una operación para sincronización. Devuelve el `txnId` (UUID).

```kotlin
val txnId = SyncBridge.getInstance().enqueue(
    endpoint = "/orders",
    payload = """{"item":"coffee","qty":2}""",
    httpMethod = HttpMethod.POST,
    priority = Priority.HIGH,
    metadata = mapOf("source" to "checkout"),
    ttlSeconds = 3600
)
```

**Lanza**: `SyncBridgeEnqueueException` si el payload excede `maxPayloadSizeKb` o falla la BD.

### `observeTransaction()`

Devuelve un `Flow<TxnState>` que completa al llegar a un estado terminal.

```kotlin
SyncBridge.getInstance().observeTransaction(txnId).collect { state ->
    when (state) {
        is TxnState.Pending   -> showSpinner()
        is TxnState.Sending   -> showSending()
        is TxnState.Synced    -> showSuccess()
        is TxnState.Conflict  -> showConflict(state.serverMessage)
        is TxnState.Failed    -> showRetrying(state.reason)
        is TxnState.Dead      -> showError()
    }
}
```

### `networkState`

`StateFlow<Boolean>` — `true` si hay internet validado, `false` si no.

```kotlin
SyncBridge.getInstance().networkState.collect { online ->
    updateUiConnectivity(online)
}
```

### `exportQueue()`

Devuelve JSON con las transacciones pendientes. Nunca lanza excepción (devuelve JSON de error en caso de fallo).

```kotlin
val json = SyncBridge.getInstance().exportQueue()
```

---

## SyncBridgeBuilder

Receptor del bloque de configuración en `initialize()`.

| Método | Requerido | Descripción |
|---|---|---|
| `storageAdapter(adapter: StorageAdapter)` | **Sí** | Implementación de persistencia |
| `authTokenProvider(provider: AuthTokenProvider)` | **Sí** | Proveedor de token de autenticación |
| `authTokenProvider { suspend () -> String }` | **Sí** | Overload lambda |
| `conflictListener(listener: ConflictListener)` | No | Listener global de conflictos 409 |
| `conflictListener { suspend (ConflictEvent) -> Unit }` | No | Overload lambda |
| `httpClient(client: HttpClient)` | No | Reemplaza el cliente HTTP (OkHttp por defecto). Útil para tests o interceptores personalizados |

**Lanza**: `SyncBridgeInitException` si faltan `storageAdapter` o `authTokenProvider`.

---

## StorageAdapter

Interfaz que debes implementar para conectar tu base de datos.

```kotlin
interface StorageAdapter {
    suspend fun save(transaction: SyncTransaction)
    suspend fun update(id: String, status: TxnStatus, serverResponse: String? = null)
    suspend fun updateRetry(id: String, attemptCount: Int, nextRetryAt: Long)
    suspend fun getPending(): List<SyncTransaction>
    suspend fun getById(id: String): SyncTransaction?
    suspend fun delete(id: String)
}
```

El adaptador de referencia con **Room** está disponible en el paquete `io.syncbridge.adapters.room`:

```kotlin
val db = SyncBridgeDatabase.getInstance(context)
val adapter = RoomSyncAdapter(db.syncDao(), batchSize = 10)
```

---

## AuthTokenProvider

SAM interface para proveer el token de autenticación en cada request.

```kotlin
fun interface AuthTokenProvider {
    suspend fun getToken(): String  // puede renovar internamente; lanza AuthException si no disponible
}
```

```kotlin
// Como lambda
authTokenProvider { tokenStore.getValidToken() }

// Como clase
class MyTokenProvider : AuthTokenProvider {
    override suspend fun getToken(): String = tokenStore.getValidToken()
}
```

**Lanza**: `AuthException` si el token no puede obtenerse o renovarse.

---

## ConflictListener / ConflictEvent

Listener opcional invocado cuando el servidor responde **409**.

```kotlin
fun interface ConflictListener {
    suspend fun onConflict(event: ConflictEvent)
}

data class ConflictEvent(
    val transaction: SyncTransaction,
    val serverResponse: String,
    val conflictedAt: Long           // epoch ms
)
```

---

## Modelos

### TxnState

Estado observable de una transacción (para UI).

```kotlin
sealed class TxnState {
    object Pending                          : TxnState()  // encolada, no intentada
    object Sending                          : TxnState()  // enviando
    object Synced                           : TxnState()  // 2xx — terminal OK
    data class Conflict(val serverMessage: String) : TxnState()  // 409
    data class Failed(val reason: String)   : TxnState()  // error recuperable, en retry
    object Dead                             : TxnState()  // sin más reintentos — terminal error
}
```

### Priority

```kotlin
enum class Priority { LOW, NORMAL, HIGH }
```

### HttpMethod

```kotlin
enum class HttpMethod { GET, POST, PUT, PATCH, DELETE }
```

### TxnStatus

Estado interno almacenado en BD (no para UI directa — usa `TxnState`).

```kotlin
enum class TxnStatus { PENDING, SENDING, SYNCED, FAILED, CONFLICT, DEAD }
```

---

## Excepciones

| Excepción | Lanzada por | Causa |
|---|---|---|
| `SyncBridgeInitException` | `initialize()` | Falta `storageAdapter` o `authTokenProvider` |
| `SyncBridgeConfigException` | `initialize()` | `syncbridge.json` no encontrado o inválido |
| `SyncBridgeEnqueueException` | `enqueue()` | Payload muy grande o error de BD |
| `AuthException` | `AuthTokenProvider.getToken()` | Token no disponible o renovación fallida |
| `StorageException` | Operaciones de `StorageAdapter` | Error de capa de persistencia |
