package com.orbix.mobile

import android.util.Log
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Entrada de menú dentro de un grupmenu (submenú horizontal en la web).
 */
data class OrbixMenuItem(
    val id: String,
    val label: String,
    /** Ruta relativa del frontend, p. ej. `frontend/misas/controller/ver_plan_de_misas.php`. */
    val url: String,
    /** Jerarquía del menú (campo `orden` del backend). */
    val order: List<Int> = emptyList(),
)

/**
 * Un apartado de menú lateral (tabla `grupmenu` en el servidor), mismo listado
 * que usa la web vía [fetchGrupMenuColeccion] → `/src/menus/grupmenu_coleccion`.
 */
data class GrupMenuItem(
    val id: String,
    val label: String,
    /** Orden de presentación (campo `orden` del backend). */
    val order: Int = 0,
    val menus: List<OrbixMenuItem> = emptyList(),
)

/**
 * Construye URL directa a un endpoint `/src/...` (como hace la web con PostRequest).
 *
 * - Si la base termina en `index.php`, se quita ese segmento.
 * - Si la base incluye `/public` (p. ej. `…/orbix/public/index.php`), también se quita:
 *   las rutas API son `…/orbix/src/…`, nunca `…/orbix/public/src/…` (404 en nginx).
 */
fun buildSrcUrl(baseUrl: String, route: String): String? {
    val root = baseUrl.trim().toHttpUrlOrNull() ?: return null
    var basePath = root.encodedPath.trimEnd('/')
    when {
        basePath.endsWith("/index.php", ignoreCase = true) ->
            basePath = basePath.dropLast("/index.php".length)
        basePath.equals("index.php", ignoreCase = true) -> basePath = ""
    }
    if (basePath.endsWith("/public", ignoreCase = true)) {
        basePath = basePath.dropLast("/public".length)
    }
    val routePath = route.trim().let { r ->
        when {
            r.startsWith("/src/") -> r
            r.startsWith("src/") -> "/$r"
            r.startsWith('/') -> r
            else -> "/src/$r"
        }
    }
    val fullPath = (basePath + routePath).replace(Regex("/+"), "/")
    return root.newBuilder()
        .encodedPath(fullPath)
        .query(null)
        .build()
        .toString()
}

/** Conservado para llamadas existentes; delega en [buildSrcUrl]. */
fun buildIndexSrcGetUrl(baseUrl: String, route: String): String? = buildSrcUrl(baseUrl, route)

private const val TAG_API = "OrbixApi"

/** Para que `login.php` responda JSON y no el formulario HTML en rutas `/src/` sin sesión. */
private const val ACCEPT_SRC_JSON = "application/json"

internal fun Request.Builder.headerAcceptSrcJson(): Request.Builder =
    header("Accept", ACCEPT_SRC_JSON)

/** Resultado de [fetchGrupMenuColeccionDetailed] con texto listo para Logcat o la UI. */
data class GrupMenuColeccionResult(
    val items: List<GrupMenuItem>,
    /** Resumen multilínea: URL, HTTP, cuerpo recortado y diagnóstico si la lista vino vacía. */
    val debugSummary: String,
)

/**
 * Lista de grupos de menú a los que el usuario tiene acceso: misma ruta `src` que la web
 * (`index.php?r=/src/menus/grupmenu_coleccion`, GET, cookies de sesión).
 * Requiere sesión válida; sin login o sin permisos, suele devolver cuerpo no JSON o lista vacía.
 */
fun fetchGrupMenuColeccion(client: OkHttpClient, baseUrl: String): List<GrupMenuItem> {
    return fetchGrupMenuColeccionDetailed(client, baseUrl).items
}

/**
 * Igual que [fetchGrupMenuColeccion] pero rellena [GrupMenuColeccionResult.debugSummary]
 * (útil en Logcat y en el cajón). Escribe en Log con tag [TAG_API].
 */
fun fetchGrupMenuColeccionDetailed(client: OkHttpClient, baseUrl: String): GrupMenuColeccionResult {
    val url = buildIndexSrcGetUrl(baseUrl.trim(), "/src/menus/grupmenu_coleccion")
    if (url == null) {
        val msg = "grupmenu_coleccion: baseUrl inválida (${baseUrl.trim()})"
        Log.w(TAG_API, msg)
        return GrupMenuColeccionResult(emptyList(), msg)
    }
    val out = StringBuilder()
    out.append("GET ").append(url).append('\n')
    return try {
        val req = Request.Builder().url(url).headerAcceptSrcJson().get().build()
        client.newCall(req).execute().use { response ->
            val code = response.code
            val body = response.body?.string().orEmpty()
            out.append("HTTP ").append(code).append('\n')
            val max = 2500
            out.append("Cuerpo (").append(body.length).append(" bytes), primeros ").append(max).append(":\n")
            out.append(body.take(max))
            if (body.length > max) out.append("\n…")
            out.append('\n')
            val items = if (response.isSuccessful) {
                parseGrupMenuColeccionBody(body)
            } else {
                out.append("→ No se parsea: respuesta no exitosa.\n")
                emptyList()
            }
            if (items.isEmpty() && response.isSuccessful) {
                out.append(diagnoseGrupMenuEmptyResponse(body))
            } else {
                out.append("→ Parseados ").append(items.size).append(" apartados.\n")
            }
            val summary = out.toString()
            Log.d(TAG_API, summary)
            GrupMenuColeccionResult(items, summary)
        }
    } catch (e: Exception) {
        out.append("Excepción: ").append(e.javaClass.simpleName).append(": ").append(e.message)
        val summary = out.toString()
        Log.e(TAG_API, summary, e)
        GrupMenuColeccionResult(emptyList(), summary)
    }
}

private fun diagnoseGrupMenuEmptyResponse(body: String): String {
    return try {
        val root = JSONObject(body)
        if (!root.optBoolean("success", false)) {
            val m = root.optString("mensaje", "")
            val dataStr = root.optString("data", "")
            val authHint = if (dataStr.contains("auth_required")) {
                " [code auth_required: el servidor no ve tu sesión en este GET; suele ser cookie PHPSESSID. Cierra sesión en la app y vuelve a entrar.]"
            } else {
                ""
            }
            "→ Diagnóstico: success=false. mensaje=$m$authHint\n"
        } else {
            when (val raw = root.opt("data")) {
                null, JSONObject.NULL -> "→ Diagnóstico: success=true pero `data` vacío o null.\n"
                is String -> {
                    if (raw.isEmpty()) {
                        "→ Diagnóstico: `data` es string vacío.\n"
                    } else {
                        val dataObj = try {
                            JSONObject(raw)
                        } catch (_: JSONException) {
                            return "→ Diagnóstico: `data` no es JSON parseable. Primeros 120 chars: " +
                                raw.take(120) + "\n"
                        }
                        diagnoseDataValores(dataObj)
                    }
                }
                is JSONObject -> diagnoseDataValores(raw)
                else -> "→ Diagnóstico: `data` tipo inesperado: ${raw.javaClass.name}\n"
            }
        }
    } catch (e: JSONException) {
        "→ Diagnóstico: cuerpo no es JSON. ${e.message}\n" +
            "Primeros 200 chars: ${body.take(200).replace("\n", " ")}"
    }
}

private fun diagnoseDataValores(dataObj: JSONObject): String {
    val valores = dataObj.optJSONObject("a_valores")
    if (valores == null) {
        val keys = buildList {
            val it = dataObj.keys()
            while (it.hasNext()) {
                add(it.next())
            }
        }
        return "→ Diagnóstico: falta `a_valores` en data. Claves: $keys\n"
    }
    if (valores.length() == 0) {
        return "→ Diagnóstico: `a_valores` es objeto vacío (0 grupos en BD o sin permisos).\n"
    }
    return "→ Diagnóstico: `a_valores` tiene entradas pero ninguna con `grupmenu` no vacío.\n"
}

internal fun parseGrupMenuColeccionBody(body: String): List<GrupMenuItem> {
    return try {
        val root = JSONObject(body)
        if (!root.optBoolean("success", false)) {
            emptyList()
        } else {
            val dataObj = when (val raw = root.opt("data")) {
                is String -> JSONObject(raw)
                is JSONObject -> raw
                else -> null
            }
            if (dataObj == null) {
                emptyList()
            } else {
                val valores = dataObj.optJSONObject("a_valores")
                if (valores == null) {
                    emptyList()
                } else {
                    val out = mutableListOf<GrupMenuItem>()
                    val keys = valores.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val row = valores.optJSONObject(k) ?: continue
                        val idRaw = row.opt("sel")
                        val id = if (idRaw == null || idRaw == JSONObject.NULL) {
                            k
                        } else {
                            idRaw.toString()
                        }
                        val label = row.optString("grupmenu", "")
                        if (label.isNotEmpty()) {
                            val order = when (val o = row.opt("orden")) {
                                is Number -> o.toInt()
                                is String -> o.toIntOrNull() ?: 0
                                else -> 0
                            }
                            out.add(
                                GrupMenuItem(
                                    id = id,
                                    label = label,
                                    order = order,
                                    menus = parseGrupMenuEntries(row.optJSONArray("menus")),
                                ),
                            )
                        }
                    }
                    out.sortedWith(
                        compareBy<GrupMenuItem> { it.order }.thenBy { it.label.lowercase() },
                    )
                }
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

/** Respuesta estándar `{ success, data }` de ContestarJson. */
fun isContestarJsonSuccess(body: String): Boolean {
    return try {
        JSONObject(body).optBoolean("success", false)
    } catch (_: Exception) {
        false
    }
}

/** Cuerpo de `app_session`: `data.authenticated`. */
fun parseSessionAuthenticated(body: String): Boolean {
    return try {
        val root = JSONObject(body)
        if (!root.optBoolean("success", false)) {
            false
        } else {
            when (val raw = root.opt("data")) {
                is String -> JSONObject(raw).optBoolean("authenticated", false)
                is JSONObject -> raw.optBoolean("authenticated", false)
                else -> false
            }
        }
    } catch (_: Exception) {
        false
    }
}

private fun parseGrupMenuEntries(menus: JSONArray?): List<OrbixMenuItem> {
    if (menus == null) return emptyList()
    val out = mutableListOf<OrbixMenuItem>()
    for (i in 0 until menus.length()) {
        val row = menus.optJSONObject(i) ?: continue
        val label = row.optString("menu", "")
        if (label.isEmpty()) continue
        val id = row.opt("id_menu")?.toString()?.takeIf { it.isNotEmpty() && it != "null" }
            ?: "${i}_${label.hashCode()}"
        out.add(
            OrbixMenuItem(
                id = id,
                label = label,
                url = row.optString("url", ""),
                order = parseMenuOrder(row.optJSONArray("orden")),
            ),
        )
    }
    return out
}

private fun parseMenuOrder(arr: JSONArray?): List<Int> {
    if (arr == null) return emptyList()
    val out = mutableListOf<Int>()
    for (i in 0 until arr.length()) {
        out.add(arr.optInt(i))
    }
    return out
}

/** Objeto `data` del envelope ContestarJson (`data` puede ser string JSON u objeto). */
internal fun parseContestarDataObject(body: String): JSONObject? {
    return try {
        val root = JSONObject(body)
        if (!root.optBoolean("success", false)) {
            null
        } else {
            when (val raw = root.opt("data")) {
                is String -> if (raw.isEmpty()) null else JSONObject(raw)
                is JSONObject -> raw
                else -> null
            }
        }
    } catch (_: Exception) {
        null
    }
}

internal fun postSrcForm(
    client: OkHttpClient,
    baseUrl: String,
    route: String,
    fields: Map<String, String>,
): String? {
    val url = buildSrcUrl(baseUrl.trim(), route) ?: return null
    val body = FormBody.Builder().apply {
        fields.forEach { (k, v) -> add(k, v) }
    }.build()
    val req = Request.Builder().url(url).headerAcceptSrcJson().post(body).build()
    return client.newCall(req).execute().use { response ->
        if (!response.isSuccessful) null else response.body?.string()
    }
}

fun fetchSessionAuthenticated(client: OkHttpClient, baseUrl: String): Boolean {
    return try {
        val url = buildIndexSrcGetUrl(baseUrl.trim(), "/src/usuarios/app_session")
        if (url == null) {
            false
        } else {
            val req = Request.Builder().url(url).headerAcceptSrcJson().get().build()
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use false
                }
                parseSessionAuthenticated(response.body?.string().orEmpty())
            }
        }
    } catch (_: Exception) {
        false
    }
}
