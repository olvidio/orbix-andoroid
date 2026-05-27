# Guía para agentes — Orbix Mobile

Repositorio de la app Android (`orbix-android`). El backend y la documentación API viven en el repo hermano **`orbix`** (p. ej. `../orbix`).

## Contexto útil

- Pantallas nativas: Kotlin + Compose bajo `app/src/main/java/com/orbix/mobile/`.
- Enrutado menú: `MenuNavigation.kt`, `MisasNavigation.kt` (u homólogo del módulo), `MainActivity.kt`, `OrbixShell.kt`.
- URLs API: `buildSrcUrl()` en `OrbixApi.kt` — base sin `/index.php` ni `/public`, rutas `/src/...`.
- Convenciones cliente: `orbix/docs/catalogo/_clientes_nativos.md`.
- Índice de endpoints ya usados en app: `orbix/docs/catalogo/_endpoints_cliente_movil.md`.

## Tras cablear un módulo con pantallas nativas

Cuando implementes un menú completo en móvil (como **Plan de misas**), **no des por cerrada la tarea solo con el código**. Completa también la documentación API en el repo `orbix`.

### 1. Inventariar endpoints usados

Recorre el código del módulo (`*Api.kt`, pantallas Compose) y lista cada llamada a `/src/<modulo>/...` (GET/POST, parámetros, parseo de `data`).

### 2. Revisar fichas API

Para **cada endpoint** que la app consume:

| Qué hacer | Dónde |
|-----------|--------|
| Revisar o crear ficha | `orbix/docs/catalogo/<modulo>/api/<endpoint>.md` |
| Validar contra PHP (`src/`) y contra la app | Objetivo, entrada, `respuesta_data`, errores, permisos, HashB |
| Marcar ficha revisada | Front matter: `estado_revision: "revisado"` |
| Ejemplos verificados | Request/response acordes a lo que hace `orbix-android` |

Prioriza lo que la app usa; no hace falta revisar todo el módulo backend si solo cableaste un subconjunto.

### 3. Actualizar índice móvil

Añade o actualiza la sección del módulo en:

`orbix/docs/catalogo/_endpoints_cliente_movil.md`

Incluye: ruta, enlace a la ficha, pantalla nativa o flujo en app (p. ej. «Plan misas → Ver plan zona»).

### 4. Regenerar OpenAPI

Desde la raíz de `orbix`:

```bash
php docs/scripts/generar_openapi_desde_catalogo.php <modulo> --force
docs/scripts/validar_openapi.sh <modulo>
```

Regenera **cada módulo** cuyas fichas `api/*.md` hayas tocado. Si no hay Node/npx, al menos ejecuta el generador PHP; la validación puede quedar pendiente.

### 5. README de la app (si aplica)

Si el módulo es visible para quien clona el repo, resume en `README.md` qué menús van nativos y enlaza a `_endpoints_cliente_movil.md`.

## Checklist rápida (copiar por módulo)

```markdown
- [ ] Pantallas nativas cableadas (drawer + índice del módulo si existe)
- [ ] *Api.kt alineado con fichas (parámetros y parseo de envelope)
- [ ] Fichas `docs/catalogo/<mod>/api/*.md` usadas → `estado_revision: revisado`
- [ ] `_endpoints_cliente_movil.md` actualizado
- [ ] `openapi.yaml` del módulo regenerado (y validado si el entorno lo permite)
- [ ] Compilación Kotlin sin errores
```

## Referencia: Plan de misas

Módulo `misas` — pantallas en `PlanMisasScreen.kt`, `PlanDeMisasCuadriculaScreen.kt`, `PlanMisasExtraScreens.kt`, `MisasApi.kt`; documentación revisada inicialmente para `plan_de_misas_pantalla_data` y `ver_cuadricula_zona_data`, ampliada al resto de submenús según endpoints consumidos.
