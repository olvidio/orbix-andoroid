# Orbix Mobile (Android, Plan B)

Repositorio **Git independiente** del backend PHP. Suele vivir junto al proyecto Orbix, por ejemplo:

- `.../orbix_local/orbix` — aplicación web (otro clon / repo)
- `.../orbix_local/orbix-android` — esta app (este repo)

Cliente mínimo en **Kotlin + Jetpack Compose** para probar el login JSON y la sesión por cookies contra el backend Orbix.

## Herramientas

1. **[Android Studio](https://developer.android.com/studio)** (recomendado): abre esta carpeta como proyecto; el IDE descarga el SDK y puede generar el **Gradle Wrapper** (`gradlew`) si falta.
2. **JDK 17** (Android Studio suele incluir uno).
3. **Emulador (AVD)** o **móvil con depuración USB** para ejecutar la app.

## Backend Orbix

Documentación API revisada para esta app: en el repo `orbix`, carpeta `docs/catalogo/` — guía [`_clientes_nativos.md`](../orbix/docs/catalogo/_clientes_nativos.md) e índice [`_endpoints_cliente_movil.md`](../orbix/docs/catalogo/_endpoints_cliente_movil.md).

### URLs (normalizar base)

La base en ajustes puede ser `http://host/orbix/public/index.php`. Las llamadas API usan la ruta directa **`…/orbix/src/…`** (se quitan `/index.php` y `/public` del path). Ver `buildSrcUrl()` en `OrbixApi.kt`.

- **Login JSON:** `POST` con cuerpo JSON a `{base normalizada}/src/usuarios/app_login`  
  Campos: `username`, `password`, `esquema` (si no viene del entorno del servidor), `verification_code` (si el usuario tiene 2FA).
- **Estado de sesión:** `GET` `{base}/src/usuarios/app_session`  
  Respuesta `ContestarJson`: `data` puede ser string JSON; si `authenticated` es true, la cookie `PHPSESSID` es válida para el resto de `/src/...`.
- **Menú:** `GET` `{base}/src/menus/grupmenu_coleccion` (`data` anidado como objeto, no string).

La app usa **OkHttp** con `InMemoryCookieJar` para guardar cookies en memoria.

### URL base en la app

Introduce la base hasta `index.php` **sin barra final**, por ejemplo:

- `http://orbix.docker:8003/orbix/public/index.php`
- Emulador contra tu PC: `http://10.0.2.2:8003/orbix/public/index.php`

`network_security_config` permite HTTP claro solo hacia `10.0.2.2` y `localhost` para depuración; en producción usa HTTPS y reduce ese permiso.

## Autenticación en dos pasos (TOTP)

- El servidor sigue validando **TOTP** como la web; la app debe enviar `verification_code` cuando el usuario tiene 2FA activo y configurado.
- Si la respuesta indica `need_2fa`, vuelve a llamar a `app_login` con el mismo usuario, contraseña, esquema y el código de seis dígitos.
- **Mismo móvil que Google Authenticator:** sigue siendo 2FA lógico (contraseña + código), pero los dos factores comparten dispositivo; para máxima separación física valorar llave FIDO2 u otro terminal (ver documentación del plan de arquitectura).

## Biometría

Esta demo no implementa `BiometricPrompt`; el plan reserva la huella para **desbloquear tokens o credenciales locales** tras un login ya validado por el servidor.

## Abrir el proyecto

**File → Open** en Android Studio y selecciona la carpeta `orbix-android`. Ejecuta en un AVD o dispositivo con **Run**.

Si Gradle pide wrapper: **File → Settings → Build → Gradle** o deja que el asistente “Sync Project” cree `gradlew`.