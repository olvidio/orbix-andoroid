package com.orbix.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import android.util.Log
import java.io.IOException

/** Tab / Mayús+Tab mueven el foco como en escritorio; evita insertar tabulador en el texto. */
private fun Modifier.tabToNextFocus(focusManager: FocusManager): Modifier =
    onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown || event.key != Key.Tab) {
            return@onPreviewKeyEvent false
        }
        if (event.isShiftPressed) {
            focusManager.moveFocus(FocusDirection.Previous)
        } else {
            focusManager.moveFocus(FocusDirection.Next)
        }
        true
    }

/**
 * Tras autenticar: shell con menú ☰. Antes: solo pantalla de login (sin cabecera de app).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as OrbixApplication
        val cookieJar = app.cookieJar
        val client = app.client

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OrbixApp(client = client, cookieJar = cookieJar)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrbixApp(client: OkHttpClient, cookieJar: InMemoryCookieJar) {
    val context = LocalContext.current
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var baseUrl by rememberSaveable { mutableStateOf("") }
    var esquema by rememberSaveable { mutableStateOf("") }
    var isAuthenticated by rememberSaveable { mutableStateOf(false) }
    var menuGroups by remember { mutableStateOf<List<GrupMenuItem>>(emptyList()) }
    var menusLoading by remember { mutableStateOf(false) }
    var selectedGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedMenuId by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val selectedGroup = menuGroups.find { it.id == selectedGroupId }
    val selectedMenu = menuGroups.flatMap { it.menus }.find { it.id == selectedMenuId }
    val singleMenuGroup = menuGroups.size == 1

    fun logout() {
        cookieJar.clear()
        isAuthenticated = false
        menuGroups = emptyList()
        selectedGroupId = null
        selectedMenuId = null
        showSettings = false
    }

    fun refreshMenus() {
        val url = baseUrl.trim()
        if (url.isEmpty() || !isAuthenticated) {
            return
        }
        scope.launch {
            menusLoading = true
            try {
                val res = withContext(Dispatchers.IO) {
                    fetchGrupMenuColeccionDetailed(client, url)
                }
                menuGroups = res.items
                if (BuildConfig.DEBUG) {
                    Log.d("OrbixApi", res.debugSummary)
                }
            } finally {
                menusLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        baseUrl = OrbixPrefs.getBaseUrl(context)
        esquema = OrbixPrefs.getEsquema(context)
    }

    // Solo al cambiar URL: restaurar sesión por cookies. No enlazar con isAuthenticated
    // para no pisar el estado justo después de un login correcto.
    LaunchedEffect(baseUrl) {
        if (baseUrl.isEmpty()) {
            isAuthenticated = false
            menuGroups = emptyList()
            selectedGroupId = null
            selectedMenuId = null
            return@LaunchedEffect
        }
        val sessionOk = withContext(Dispatchers.IO) {
            fetchSessionAuthenticated(client, baseUrl.trim())
        }
        isAuthenticated = sessionOk
        if (sessionOk) {
            menusLoading = true
            try {
                val res = withContext(Dispatchers.IO) {
                    fetchGrupMenuColeccionDetailed(client, baseUrl.trim())
                }
                menuGroups = res.items
                if (BuildConfig.DEBUG) {
                    Log.d("OrbixApi", res.debugSummary)
                }
            } finally {
                menusLoading = false
            }
        } else {
            menuGroups = emptyList()
            selectedGroupId = null
            selectedMenuId = null
        }
    }

    LaunchedEffect(menuGroups) {
        if (menuGroups.size == 1) {
            selectedGroupId = menuGroups.first().id
        }
    }

    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(
            initialBaseUrl = baseUrl,
            initialEsquema = esquema,
            onBack = { showSettings = false },
            onSave = { url, esq ->
                OrbixPrefs.save(context, url, esq)
                baseUrl = OrbixPrefs.getBaseUrl(context)
                esquema = OrbixPrefs.getEsquema(context)
                showSettings = false
            },
        )
        return
    }

    if (!isAuthenticated) {
        AuthLoginScreen(
            client = client,
            baseUrl = baseUrl,
            esquema = esquema,
            onOpenSettings = { showSettings = true },
            onLoginSuccess = {
                isAuthenticated = true
                refreshMenus()
            },
        )
        return
    }

    val sectionTitle = selectedGroup?.label
    // Con un solo grupmenu su nombre (p. ej. «exterior») no aporta nada: se muestra «Inicio».
    val shellTitle = selectedMenu?.label
        ?: sectionTitle?.takeIf { !singleMenuGroup }
        ?: "Inicio"

    BackHandler(enabled = selectedMenuId != null) {
        selectedMenuId = null
    }

    OrbixShell(
        title = shellTitle,
        menuGroups = menuGroups,
        menusLoading = menusLoading,
        selectedGroupId = selectedGroupId,
        selectedMenuId = selectedMenuId,
        onSelectHome = {
            if (singleMenuGroup) {
                selectedMenuId = null
            } else {
                selectedGroupId = null
                selectedMenuId = null
            }
        },
        onSelectGroup = { item ->
            selectedGroupId = item.id
            selectedMenuId = null
        },
        onSelectMenu = { item ->
            if (singleMenuGroup) {
                selectedGroupId = menuGroups.first().id
            }
            selectedMenuId = item.id
        },
        onOpenSettings = { showSettings = true },
        onLogout = { logout() },
    ) { padding ->
        when {
            selectedMenu != null && isMisasNativeScreen(nativeScreenFor(selectedMenu)) -> {
                MisasNativeScreen(
                    screen = nativeScreenFor(selectedMenu),
                    client = client,
                    baseUrl = baseUrl,
                    contentPadding = padding,
                )
            }
            selectedMenu != null &&
                nativeScreenFor(selectedMenu) == NativeMenuScreen.AtencionActividades -> {
                AtencionActividadesScreen(
                    client = client,
                    baseUrl = baseUrl,
                    contentPadding = padding,
                )
            }
            selectedMenu != null &&
                nativeScreenFor(selectedMenu) == NativeMenuScreen.PlanningZonas -> {
                PlanningZonasScreen(
                    client = client,
                    baseUrl = baseUrl,
                    contentPadding = padding,
                )
            }
            selectedMenu != null &&
                nativeScreenFor(selectedMenu) == NativeMenuScreen.Ausencias -> {
                AusenciasScreen(
                    client = client,
                    baseUrl = baseUrl,
                    contentPadding = padding,
                    mode = ausenciasModeForMenu(selectedMenu),
                )
            }
            selectedMenu != null &&
                nativeScreenFor(selectedMenu) == NativeMenuScreen.NuevoPlan -> {
                NuevoPlanScreen(
                    client = client,
                    baseUrl = baseUrl,
                    contentPadding = padding,
                    propuestaCalendario = isNuevoPlanPropuesta(selectedMenu),
                )
            }
            selectedMenu != null -> {
                UnimplementedMenuScreen(
                    contentPadding = padding,
                    menu = selectedMenu,
                    onBack = { selectedMenuId = null },
                )
            }
            selectedGroup != null && !singleMenuGroup -> {
                GroupMenuListScreen(
                    contentPadding = padding,
                    group = selectedGroup,
                    onSelectMenu = { selectedMenuId = it.id },
                )
            }
            else -> {
                AuthenticatedHomeScreen(contentPadding = padding)
            }
        }
    }
}

@Composable
private fun AuthLoginScreen(
    client: OkHttpClient,
    baseUrl: String,
    esquema: String,
    onOpenSettings: () -> Unit,
    onLoginSuccess: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var totp by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }
    var errorFields by remember { mutableStateOf(emptySet<LoginField>()) }
    var loggingIn by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Aquinate", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onOpenSettings) {
                Text("Configuración")
            }
        }
        Text(
            "Versión ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(vertical = 56.dp)
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (baseUrl.isEmpty()) {
                Text(
                    "Define la URL y el esquema en Configuración.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    errorFields = errorFields - LoginField.Usuario
                },
                isError = LoginField.Usuario in errorFields,
                label = { Text("Usuario") },
                modifier = Modifier
                    .fillMaxWidth()
                    .tabToNextFocus(focusManager),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) },
                ),
            )
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorFields = errorFields - LoginField.Password
                },
                isError = LoginField.Password in errorFields,
                label = { Text("Contraseña") },
                modifier = Modifier
                    .fillMaxWidth()
                    .tabToNextFocus(focusManager),
                singleLine = true,
                visualTransformation = if (showPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = if (showPassword) {
                                "Ocultar contraseña"
                            } else {
                                "Mostrar contraseña"
                            },
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) },
                ),
            )
            OutlinedTextField(
                value = totp,
                onValueChange = {
                    totp = it
                    errorFields = errorFields - LoginField.Codigo
                },
                isError = LoginField.Codigo in errorFields,
                label = { Text("Código 2FA (si aplica)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .tabToNextFocus(focusManager),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                ),
            )
            Button(
                onClick = {
                    loginError = null
                    errorFields = emptySet()
                    if (baseUrl.isEmpty()) {
                        loginError = "Configura primero la URL en Configuración."
                        return@Button
                    }
                    val missing = buildSet {
                        if (username.isBlank()) add(LoginField.Usuario)
                        if (password.isEmpty()) add(LoginField.Password)
                    }
                    if (missing.isNotEmpty()) {
                        errorFields = missing
                        loginError = when {
                            missing.size == 2 -> "Introduce el usuario y la contraseña."
                            LoginField.Usuario in missing -> "Introduce el usuario."
                            else -> "Introduce la contraseña."
                        }
                        return@Button
                    }
                    scope.launch {
                        loggingIn = true
                        try {
                            val result = withContext(Dispatchers.IO) {
                                runLogin(
                                    client,
                                    baseUrl.trim(),
                                    username,
                                    password,
                                    esquema.trim(),
                                    totp.trim(),
                                )
                            }
                            if (BuildConfig.DEBUG) Log.d("OrbixApi", result.log)
                            if (result.succeeded) {
                                val sessionOk = withContext(Dispatchers.IO) {
                                    fetchSessionAuthenticated(client, baseUrl.trim())
                                }
                                if (sessionOk) {
                                    onLoginSuccess()
                                } else {
                                    loginError = "Has entrado, pero la sesión no se mantiene. " +
                                        "Reinstala la app y vuelve a probar."
                                }
                            } else {
                                loginError = result.message
                                errorFields = result.fields
                            }
                        } finally {
                            loggingIn = false
                        }
                    }
                },
                enabled = !loggingIn,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (loggingIn) "Entrando…" else "Iniciar sesión") }
            loginError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupMenuListScreen(
    contentPadding: PaddingValues,
    group: GrupMenuItem,
    onSelectMenu: (OrbixMenuItem) -> Unit,
) {
    val rows = remember(group.menus) { buildMenuDisplayRows(group.menus) }

    LazyColumn(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Text(
                text = group.label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }

        if (rows.isEmpty()) {
            item {
                Text(
                    text = "Este apartado no tiene entradas de menú visibles.",
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(rows, key = { row ->
                when (row) {
                    is MenuDisplayRow.Section -> "s_${row.depth}_${row.title}"
                    is MenuDisplayRow.Item -> "i_${row.menu.id}"
                }
            }) { row ->
                when (row) {
                    is MenuDisplayRow.Section -> {
                        Text(
                            text = row.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(
                                start = (12 + row.depth * 16).dp,
                                top = 10.dp,
                                end = 4.dp,
                                bottom = 2.dp,
                            ),
                        )
                    }
                    is MenuDisplayRow.Item -> {
                        val native = row.native != NativeMenuScreen.Pending
                        Card(
                            onClick = { onSelectMenu(row.menu) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = (row.depth * 16).dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = row.menu.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Text(
                                        text = if (native) "Pantalla nativa" else "Pendiente de implementar",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (native) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                    Text(
                                        text = row.menu.url,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnimplementedMenuScreen(
    contentPadding: PaddingValues,
    menu: OrbixMenuItem,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(menu.label, style = MaterialTheme.typography.titleMedium)
        Text(
            if (nativeScreenFor(menu) == NativeMenuScreen.Pending) {
                "Esta pantalla aún no está implementada en la app móvil."
            } else {
                "Pantalla nativa"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(menu.url, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Volver")
        }
    }
}

@Composable
private fun AuthenticatedHomeScreen(contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "Toca el icono ☰ de arriba a la izquierda para ver los menús.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    initialBaseUrl: String,
    initialEsquema: String,
    onBack: () -> Unit,
    onSave: (baseUrl: String, esquema: String) -> Unit,
) {
    var baseUrlEdit by remember(initialBaseUrl) { mutableStateOf(initialBaseUrl) }
    var esquemaEdit by remember(initialEsquema) { mutableStateOf(initialEsquema) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Se guardan en el dispositivo (SharedPreferences). La contraseña no se guarda.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = baseUrlEdit,
                onValueChange = { baseUrlEdit = it },
                label = { Text("Base URL (sin barra final)") },
                placeholder = { Text("http://orbix.docker:8003/orbix/index.php") },
                modifier = Modifier
                    .fillMaxWidth()
                    .tabToNextFocus(focusManager),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) },
                ),
            )
            OutlinedTextField(
                value = esquemaEdit,
                onValueChange = { esquemaEdit = it },
                label = { Text("Esquema") },
                placeholder = { Text("H-dlbv") },
                modifier = Modifier
                    .fillMaxWidth()
                    .tabToNextFocus(focusManager),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                ),
            )
            Button(
                onClick = { onSave(baseUrlEdit.trim(), esquemaEdit.trim()) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Guardar") }
        }
    }
}

private enum class LoginField { Usuario, Password, Codigo }

/** `log` es el detalle técnico (solo para Logcat); `message` es lo que ve el usuario. */
private data class LoginHttpResult(
    val log: String,
    val succeeded: Boolean,
    val message: String = "",
    val fields: Set<LoginField> = emptySet(),
)

/** Traduce el `code` de `app_login` (AppMobileLogin) a un aviso claro y los campos afectados. */
private fun loginFailure(log: String, httpCode: Int, bodyStr: String): LoginHttpResult {
    val env = parseSrcEnvelope(bodyStr)
    val code = env?.dataObject?.optStr("code", "").orEmpty()
    val (msg, fields) = when (code) {
        "missing_credentials" -> "Introduce el usuario y la contraseña." to
            setOf(LoginField.Usuario, LoginField.Password)
        "invalid_credentials" -> "Usuario o contraseña incorrectos." to
            setOf(LoginField.Usuario, LoginField.Password)
        "need_2fa" -> "Introduce el código 2FA." to setOf(LoginField.Codigo)
        "invalid_2fa" -> "El código 2FA no es correcto." to setOf(LoginField.Codigo)
        "need_2fa_setup" -> "Debes completar la configuración 2FA en la web antes de entrar." to
            emptySet()
        "dmz_denied" -> "Este usuario no tiene acceso permitido." to emptySet()
        "invalid_schema", "invalid_schema_env", "missing_schema" ->
            "El esquema no es válido. Revísalo en Configuración." to emptySet()
        "server_error" -> "Error del servidor. Inténtalo más tarde." to emptySet()
        else -> (env?.mensaje?.takeIf { it.isNotBlank() }
            ?: "Respuesta inesperada del servidor (HTTP $httpCode).") to emptySet()
    }
    return LoginHttpResult(log, false, msg, fields)
}

private fun runLogin(
    client: OkHttpClient,
    baseUrl: String,
    username: String,
    password: String,
    esquema: String,
    verificationCode: String,
): LoginHttpResult {
    val url = buildIndexSrcGetUrl(baseUrl.trim(), "/src/usuarios/app_login")
        ?: return LoginHttpResult("URL inválida", false, "La URL del servidor no es válida. Revísala en Configuración.")
    val json = buildString {
        append("{\"username\":").append(jsonQuote(username))
        append(",\"password\":").append(jsonQuote(password))
        if (esquema.isNotEmpty()) append(",\"esquema\":").append(jsonQuote(esquema))
        if (verificationCode.isNotEmpty()) append(",\"verification_code\":").append(jsonQuote(verificationCode))
        append('}')
    }
    val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
    val req = Request.Builder().url(url).headerAcceptSrcJson().post(body).build()
    return try {
        client.newCall(req).execute().use { r ->
            val bodyStr = r.body?.string().orEmpty()
            val text = "POST $url\nHTTP ${r.code}\n$bodyStr"
            val ok = r.isSuccessful && isContestarJsonSuccess(bodyStr)
            if (ok) LoginHttpResult(text, true) else loginFailure(text, r.code, bodyStr)
        }
    } catch (e: IOException) {
        val hint =
            if (e.message?.contains("127.0.0.1") == true || e.message?.contains("ECONNREFUSED") == true) {
                "\n\nEn emulador, 127.0.0.1 es el propio emulador. Usa la IP especial del PC: " +
                    "http://10.0.2.2:PUERTO/…/public (mismo path que en el navegador). " +
                    "O ejecuta: adb reverse tcp:PUERTO tcp:PUERTO y usa http://127.0.0.1:PUERTO/…"
            } else {
                ""
            }
        LoginHttpResult(
            log = "${e.javaClass.simpleName}: ${e.message}$hint",
            succeeded = false,
            message = "No se pudo conectar con el servidor. Revisa la conexión y la URL en Configuración.",
        )
    }
}

private fun jsonQuote(s: String): String =
    "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
