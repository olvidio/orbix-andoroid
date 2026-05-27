package com.orbix.mobile

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Almacén de cookies para OkHttp. Suele comportarse mejor que [okhttp3.JavaNetCookieJar]
 * con respuestas PHP (Set-Cookie + SameSite) en Android.
 */
class InMemoryCookieJar : CookieJar {
    private val storage = mutableListOf<Cookie>()
    private val lock = Any()

    fun clear() {
        synchronized(lock) {
            storage.clear()
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        synchronized(lock) {
            for (cookie in cookies) {
                storage.removeAll { existing ->
                    existing.name == cookie.name &&
                        existing.domain == cookie.domain &&
                        existing.path == cookie.path
                }
                if (cookie.expiresAt <= System.currentTimeMillis() && cookie.persistent) {
                    continue
                }
                storage.add(cookie)
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            storage.removeAll { it.persistent && it.expiresAt < now }
            return storage.filter { it.matches(url) }
        }
    }
}
