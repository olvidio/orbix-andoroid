package com.orbix.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.JavaNetCookieJar
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.CookieManager
import java.net.CookiePolicy

/**
 * Pantalla mínima: login JSON contra `/src/usuarios/app_login` y comprobación de sesión.
 * Las cookies de sesión se guardan en memoria con [OkHttpClient] (CookieJar).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cookieManager = CookieManager(null, CookiePolicy.ACCEPT_ALL)
        val client = OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(cookieManager))
            .build()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OrbixLoginScreen(client)
                }
            }
        }
    }
}

@Composable
private fun OrbixLoginScreen(client: OkHttpClient) {
    var baseUrl by remember { mutableStateOf("https://ejemplo.org/orbix/public") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var esquema by remember { mutableStateOf("") }
    var totp by remember { mutableStateOf("") }
    var log by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Orbix (Plan B — prueba de API)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("Base URL (sin barra final)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuario") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = esquema,
            onValueChange = { esquema = it },
            label = { Text("Esquema (v/f)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = totp,
            onValueChange = { totp = it },
            label = { Text("Código 2FA (si aplica)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                scope.launch {
                    log = "…"
                    log = withContext(Dispatchers.IO) {
                        runLogin(client, baseUrl.trim(), username, password, esquema.trim(), totp.trim())
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Iniciar sesión (app_login)") }
        Button(
            onClick = {
                scope.launch {
                    log = "…"
                    log = withContext(Dispatchers.IO) {
                        runSessionCheck(client, baseUrl.trim())
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Comprobar sesión (app_session)") }
        Text("Respuesta", style = MaterialTheme.typography.labelLarge)
        Text(log, fontFamily = FontFamily.Monospace, modifier = Modifier.fillMaxWidth())
    }
}

private fun runLogin(
    client: OkHttpClient,
    baseUrl: String,
    username: String,
    password: String,
    esquema: String,
    verificationCode: String,
): String {
    val root = baseUrl.toHttpUrlOrNull() ?: return "URL inválida"
    val url = root.newBuilder()
        .encodedPath("${root.encodedPath.trimEnd('/')}/index.php")
        .addQueryParameter("r", "/src/usuarios/app_login")
        .build()
    val json = buildString {
        append("{\"username\":").append(jsonQuote(username))
        append(",\"password\":").append(jsonQuote(password))
        if (esquema.isNotEmpty()) append(",\"esquema\":").append(jsonQuote(esquema))
        if (verificationCode.isNotEmpty()) append(",\"verification_code\":").append(jsonQuote(verificationCode))
        append('}')
    }
    val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
    val req = Request.Builder().url(url).post(body).build()
    return client.newCall(req).execute().use { r ->
        "HTTP ${r.code}\n${r.body?.string().orEmpty()}"
    }
}

private fun runSessionCheck(client: OkHttpClient, baseUrl: String): String {
    val root = baseUrl.toHttpUrlOrNull() ?: return "URL inválida"
    val url = root.newBuilder()
        .encodedPath("${root.encodedPath.trimEnd('/')}/index.php")
        .addQueryParameter("r", "/src/usuarios/app_session")
        .build()
    val req = Request.Builder().url(url).get().build()
    return client.newCall(req).execute().use { r ->
        "HTTP ${r.code}\n${r.body?.string().orEmpty()}"
    }
}

private fun jsonQuote(s: String): String =
    "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
