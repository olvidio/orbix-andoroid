package com.orbix.mobile

import android.content.Context

/** URL base y esquema persistidos en el dispositivo (no secretos). */
object OrbixPrefs {
    private const val PREFS = "orbix_prefs"
    private const val K_BASE_URL = "base_url"
    private const val K_ESQUEMA = "esquema"

    /** URL base guardada, con el esquema añadido a la ruta si falta (ver [withEsquemaInPath]). */
    fun getBaseUrl(context: Context): String =
        withEsquemaInPath(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(K_BASE_URL, null)
                ?.trim()
                .orEmpty(),
            getEsquema(context),
        )

    /**
     * Algunas instalaciones (p. ej. `/orbixsf/`) exigen el esquema en la ruta:
     * `…/orbixsf/H-dlbf/index.php`. Si la URL no lo lleva, se inserta antes de `index.php`
     * (o al final si no hay `index.php`).
     */
    internal fun withEsquemaInPath(baseUrl: String, esquema: String): String {
        val esq = esquema.trim().trim('/')
        if (baseUrl.isEmpty() || esq.isEmpty()) return baseUrl
        val qIdx = baseUrl.indexOfAny(charArrayOf('?', '#')).let { if (it < 0) baseUrl.length else it }
        val head = baseUrl.substring(0, qIdx)
        val tail = baseUrl.substring(qIdx)
        val schemeEnd = head.indexOf("://").let { if (it < 0) 0 else it + 3 }
        val pathStart = head.indexOf('/', schemeEnd).let { if (it < 0) head.length else it }
        val origin = head.substring(0, pathStart)
        val segments = head.substring(pathStart).split('/').filter { it.isNotEmpty() }.toMutableList()
        if (segments.any { it.equals(esq, ignoreCase = true) }) return baseUrl
        val last = segments.lastOrNull()
        if (last != null && last.equals("index.php", ignoreCase = true)) {
            segments.add(segments.size - 1, esq)
        } else {
            segments.add(esq)
        }
        return origin + "/" + segments.joinToString("/") + tail
    }

    fun getEsquema(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(K_ESQUEMA, null)
            ?.trim()
            .orEmpty()

    fun save(context: Context, baseUrl: String, esquema: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(K_BASE_URL, baseUrl.trim())
            .putString(K_ESQUEMA, esquema.trim())
            .apply()
    }
}
