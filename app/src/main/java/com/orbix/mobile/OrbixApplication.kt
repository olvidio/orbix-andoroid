package com.orbix.mobile

import android.app.Application
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Clase Application que mantiene [cookieJar] y [client] vivos durante todo el
 * ciclo de vida del proceso de la app, permitiendo que la sesión por cookies
 * y las peticiones HTTP sobrevivan a cambios de configuración (como la rotación de pantalla).
 */
class OrbixApplication : Application() {
    val cookieJar = InMemoryCookieJar()

    val client: OkHttpClient by lazy {
        val http = OkHttpClient.Builder().cookieJar(cookieJar)
        if (BuildConfig.DEBUG) {
            val logInterceptor = HttpLoggingInterceptor { line -> Log.d("OrbixHttp", line) }
            logInterceptor.level = HttpLoggingInterceptor.Level.BODY
            http.addInterceptor(logInterceptor)
        }
        http.build()
    }
}
