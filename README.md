# ApiMovil — App Android del Sistema de Órdenes de Combustible

Consume la API Laravel del repo `ProgramacionMovil`. Sigue el mismo patrón que
usa el ingeniero en sus ejemplos de clase: Retrofit + OkHttp + Gson +
`HttpLoggingInterceptor`, ViewModel con `StateFlow<UiState<T>>`, Fragment con
`viewModels()` + `repeatOnLifecycle(STARTED)`, RecyclerView con `ListAdapter`.

## Cómo abrirlo

1. Abre la carpeta `ApiMovil/` directo en Android Studio (`Open` → selecciona
   esta carpeta, no un archivo).
2. Android Studio va a detectar que falta el `.jar` del Gradle Wrapper (no se
   puede generar fuera de un entorno con Gradle instalado) y te va a ofrecer
   descargarlo automáticamente al sincronizar. Acepta. Si no te lo ofrece,
   corre `gradle wrapper --gradle-version 9.9.1` una vez, con Gradle instalado
   en tu máquina, parado en esta carpeta.
3. Deja que sincronice (va a descargar las dependencias de Retrofit, OkHttp,
   Navigation, ZXing, etc. automáticamente).

## URL del backend

Está configurada en `app/src/main/java/com/example/apimovil/data/ApiClient.kt`:

```kotlin
private const val BASE_URL = "https://geocom-production.up.railway.app/api/"
```

Actualmente apunta al servidor de producción en Railway, por lo que funciona tanto en emulador como en dispositivos físicos sin necesidad de levantar un servidor local (`php artisan serve`).

## Qué cubre cada rol

- **Conductor**: login → nueva solicitud → escanear QR de la gasolinera →
  ver monto y contador de 15 minutos → subir evidencia por cada línea →
  orden completada. Las órdenes vencidas no se muestran (el backend ya las
  filtra para este rol).
- **Administrador**: login → solicitudes pendientes → asignar monto y
  gasolinera (genera la orden) → ver todas las órdenes con filtro de
  estado → catálogos de solo lectura → registrar y consultar caja chica →
  reportes por conductor y por gasolinera.
- **Contabilidad (hace de "supervisor" hasta que renombren el rol en el
  backend)**: mismo panel que el administrador, pero sin el botón de
  registrar caja chica, y sin poder tocar una solicitud para asignarle
  monto (el backend igual lo rechazaría con 403 si se intentara).

## Decisiones de alcance (para no inflar el proyecto sin necesidad)

- La gestión completa de catálogos (crear/editar/desactivar conductores,
  vehículos y gasolineras) **no está en el celular**, solo lectura. Esa
  gestión vive en el panel web, que es donde tiene sentido hacerla con
  calma. La API ya soporta esas operaciones (`ApiService.kt` las tiene
  todas) por si luego quieren agregarles pantalla.
- No hay una pantalla de "historial" separada para el conductor: el
  endpoint `GET /ordenes` ya excluye las vencidas para ese rol, así que una
  pantalla de historial no mostraría nada distinto a la de inicio.
- El código QR se lee con ZXing embebido (`journeyapps:zxing-android-embedded`),
  no con CameraX + ML Kit, para mantener la dependencia simple. Si prefieren
  ML Kit, el único archivo que cambia es `EscanearQrFragment.kt`.
- La foto de evidencia se toma con la cámara nativa del sistema
  (`ActivityResultContracts.TakePicture()` + `FileProvider`), no con CameraX.
  Más simple y suficiente para lo que pide el flujo.

## Pendientes que dependen del backend, no de esta app

- Si cambian el nombre del rol `contabilidad` a `supervisor` en el backend
  (como quedó pendiente en el ERS), solo hay que cambiar las comparaciones
  de texto en `SessionManager.kt` (`esContabilidad`), nada más en esta app
  depende del nombre literal del rol.
- Todo lo que el ERS deja pendiente de definir con el cliente (mecanismo de
  validación para trabajos foráneos, kilometraje, etc.) tampoco está
  implementado aquí, a propósito, por la misma razón que no está en el
  backend.
