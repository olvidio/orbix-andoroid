package com.orbix.mobile

import android.content.Context

/** URL base y esquema persistidos en el dispositivo (no secretos). */
object OrbixPrefs {
    private const val PREFS = "orbix_prefs"
    private const val K_BASE_URL = "base_url"
    private const val K_ESQUEMA = "esquema"

    fun getBaseUrl(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(K_BASE_URL, null)
            ?.trim()
            .orEmpty()

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
