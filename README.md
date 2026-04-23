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

- **Login JSON (sin HTML):** `POST` con cuerpo JSON a  
`{baseUrl}/index.php?r=/src/usuarios/app_login`  
Campos: `username`, `password`, `esquema` (si no viene del entorno del servidor), `verification_code` (si el usuario tiene 2FA).
- **Estado de sesión:** `GET` `{baseUrl}/index.php?r=/src/usuarios/app_session`  
Respuesta `ContestarJson`: `data` puede ser string JSON; si `authenticated` es true, la cookie `PHPSESSID` enviada por OkHttp es válida para el resto de `/src/...`.

La app usa **OkHttp** con `JavaNetCookieJar` para guardar cookies en memoria.

### URL base en la app

Introduce la base hasta `public` **sin barra final**, por ejemplo:

- `https://tu-servidor/org/public`
- Emulador contra tu PC: `http://10.0.2.2:8080/ruta/public` (el alias `10.0.2.2` es el host del equipo desde el emulador).

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