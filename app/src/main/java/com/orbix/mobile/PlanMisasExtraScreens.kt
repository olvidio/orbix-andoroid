package com.orbix.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

@Composable
fun VerPlanSacdScreen(
    client: OkHttpClient,
    baseUrl: String,
    contentPadding: PaddingValues,
) {
    var loadingPage by remember { mutableStateOf(true) }
    var loadingResult by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf<BuscarPlanSacdPage?>(null) }
    var rows by remember { mutableStateOf<List<PlanSacdRow>?>(null) }

    var selectedSacd by remember { mutableStateOf<String?>(null) }
    var selectedPeriodo by remember { mutableStateOf("esta_semana") }
    var sacdExpanded by remember { mutableStateOf(false) }
    var periodoExpanded by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val periodoOptions = periodoLabelsVer()

    LaunchedEffect(baseUrl) {
        loadingPage = true
        error = null
        page = null
        rows = null
        showFilters = true
        try {
            val data = withContext(Dispatchers.IO) { fetchBuscarPlanSacdPage(client, baseUrl) }
            if (data == null || data.sacdOpciones.isEmpty()) {
                error = "No hay sacerdotes disponibles."
            } else {
                page = data
                selectedSacd = data.sacdSelected.ifEmpty { data.sacdOpciones.keys.firstOrNull() }
            }
        } finally {
            loadingPage = false
        }
    }

    fun buscar() {
        val sacd = selectedSacd ?: return
        scope.launch {
            loadingResult = true
            error = null
            try {
                val result = withContext(Dispatchers.IO) {
                    fetchVerPlanSacd(client, baseUrl, idSacd = sacd, periodo = selectedPeriodo)
                }
                if (result == null) {
                    error = "No se pudo cargar (ver_plan_sacd_data)."
                    rows = null
                } else {
                    rows = result
                    showFilters = false
                    if (result.isEmpty()) error = "Sin datos para el periodo."
                }
            } finally {
                loadingResult = false
            }
        }
    }

    Column(
        modifier = Modifier.padding(contentPadding).padding(horizontal = 12.dp, vertical = 8.dp).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Plan de un sacerdote", style = MaterialTheme.typography.titleMedium)
        if (loadingPage) {
            MisasLoadingBox()
            return@Column
        }
        val sacds = page?.sacdOpciones.orEmpty()
        if (sacds.isEmpty()) {
            Text(error ?: "Sin datos", color = MaterialTheme.colorScheme.error)
            return@Column
        }

        if (!showFilters && rows != null) {
            MisasFilterSummaryBar(
                summary = "${sacds[selectedSacd].orEmpty()} · ${periodoOptions[selectedPeriodo].orEmpty()}",
                onShowFilters = { showFilters = true },
            )
        }
        if (showFilters) {
            MisasDropdown(
                label = "Sacerdote",
                value = sacds[selectedSacd].orEmpty(),
                expanded = sacdExpanded,
                onExpandedChange = { sacdExpanded = it },
                options = sacds.map { (id, label) -> id to label },
                onSelect = { selectedSacd = it },
            )
            MisasDropdown(
                label = "Periodo",
                value = periodoOptions[selectedPeriodo].orEmpty(),
                expanded = periodoExpanded,
                onExpandedChange = { periodoExpanded = it },
                options = periodoOptions.map { (id, label) -> id to label },
                onSelect = { selectedPeriodo = it },
            )
            Button(onClick = { buscar() }, modifier = Modifier.fillMaxWidth(), enabled = !loadingResult) {
                Text("Ver plan")
            }
        }
        if (loadingResult) MisasLoadingBox()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        rows?.let { if (it.isNotEmpty()) SacdPlanList(rows = it) }
    }
}

@Composable
fun VerPlanCtrScreen(
    client: OkHttpClient,
    baseUrl: String,
    contentPadding: PaddingValues,
) {
    var loading by remember { mutableStateOf(false) }
    var loadingGrid by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf<BuscarPlanCtrPage?>(null) }
    var grid by remember { mutableStateOf<PlanCtrGrid?>(null) }

    var selectedZona by remember { mutableStateOf("0") }
    var selectedCentro by remember { mutableStateOf<String?>(null) }
    var selectedPeriodo by remember { mutableStateOf("esta_semana") }
    var zonaExpanded by remember { mutableStateOf(false) }
    var centroExpanded by remember { mutableStateOf(false) }
    var periodoExpanded by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val periodoOptions = periodoLabelsVer()

    fun loadPage(zonaId: String) {
        scope.launch {
            loading = true
            error = null
            try {
                val data = withContext(Dispatchers.IO) {
                    fetchBuscarPlanCtrPage(client, baseUrl, idZona = zonaId)
                }
                if (data == null) {
                    error = "No se pudo cargar (buscar_plan_ctr_data)."
                    page = null
                } else if (data.error != null) {
                    error = data.error
                    page = data
                } else {
                    page = data
                    selectedZona = data.zonasSelected.ifEmpty { zonaId }
                    selectedCentro = data.centrosSelected.ifEmpty {
                        data.centrosOpciones.keys.firstOrNull()
                    }
                }
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(baseUrl) {
        grid = null
        showFilters = true
        loadPage("0")
    }

    fun buscar() {
        val centro = selectedCentro ?: return
        scope.launch {
            loadingGrid = true
            error = null
            try {
                val result = withContext(Dispatchers.IO) {
                    fetchVerPlanCtr(client, baseUrl, idUbi = centro, periodo = selectedPeriodo)
                }
                if (result == null) {
                    error = "No se pudo cargar (ver_plan_ctr_data)."
                    grid = null
                } else {
                    grid = result
                    showFilters = false
                    if (result.rows.isEmpty()) error = "Sin datos para el periodo."
                }
            } finally {
                loadingGrid = false
            }
        }
    }

    Column(
        modifier = Modifier.padding(contentPadding).padding(horizontal = 12.dp, vertical = 8.dp).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Plan de un centro", style = MaterialTheme.typography.titleMedium)
        if (loading) MisasLoadingBox()

        val p = page
        if (p != null && p.error == null) {
            if (!showFilters && grid != null) {
                MisasFilterSummaryBar(
                    summary = buildString {
                        append(p.centrosOpciones[selectedCentro].orEmpty())
                        append(" · ")
                        append(periodoOptions[selectedPeriodo].orEmpty())
                    },
                    onShowFilters = { showFilters = true },
                )
            }
            if (showFilters) {
                if (p.zonasOpciones.isNotEmpty()) {
                    MisasDropdown(
                        label = "Zona",
                        value = p.zonasOpciones[selectedZona].orEmpty(),
                        expanded = zonaExpanded,
                        onExpandedChange = { zonaExpanded = it },
                        options = p.zonasOpciones.map { (id, label) -> id to label },
                        onSelect = {
                            selectedZona = it
                            loadPage(it)
                        },
                    )
                }
                MisasDropdown(
                    label = "Centro",
                    value = p.centrosOpciones[selectedCentro].orEmpty(),
                    expanded = centroExpanded,
                    onExpandedChange = { centroExpanded = it },
                    options = p.centrosOpciones.map { (id, label) -> id to label },
                    onSelect = { selectedCentro = it },
                )
                MisasDropdown(
                    label = "Periodo",
                    value = periodoOptions[selectedPeriodo].orEmpty(),
                    expanded = periodoExpanded,
                    onExpandedChange = { periodoExpanded = it },
                    options = periodoOptions.map { (id, label) -> id to label },
                    onSelect = { selectedPeriodo = it },
                )
                Button(
                    onClick = { buscar() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loadingGrid && selectedCentro != null,
                ) {
                    Text("Ver plan")
                }
            }
        }
        if (loadingGrid) MisasLoadingBox()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        grid?.let { if (it.rows.isNotEmpty()) PlanCtrGridTable(it.columns, it.rows, it.legend) }
    }
}

@Composable
fun InicialesZonaScreen(
    client: OkHttpClient,
    baseUrl: String,
    contentPadding: PaddingValues,
) {
    var loadingPage by remember { mutableStateOf(true) }
    var loadingGrid by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf<ZonaOpcionesPage?>(null) }
    var rows by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }

    var selectedZona by remember { mutableStateOf<String?>(null) }
    var zonaExpanded by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(true) }

    var editingRow by remember { mutableStateOf<Map<String, String>?>(null) }
    var editIniciales by remember { mutableStateOf("") }
    var editColor by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    fun buscar() {
        val zona = selectedZona ?: return
        scope.launch {
            loadingGrid = true
            error = null
            statusMsg = null
            try {
                val result = withContext(Dispatchers.IO) {
                    fetchVerInicialesZona(client, baseUrl, idZona = zona)
                }
                if (result == null) {
                    error = "No se pudo cargar los datos."
                    rows = emptyList()
                } else {
                    rows = result.rows
                    showFilters = false
                    if (result.rows.isEmpty()) error = "Sin registros."
                }
            } finally {
                loadingGrid = false
            }
        }
    }

    fun guardarIniciales() {
        val row = editingRow ?: return
        val idSacd = row["id_sacd"].orEmpty()
        if (idSacd.isEmpty()) return
        scope.launch {
            saving = true
            error = null
            statusMsg = null
            try {
                val result = withContext(Dispatchers.IO) {
                    postUpdateIniciales(
                        client,
                        baseUrl,
                        idSacd = idSacd,
                        iniciales = editIniciales.trim(),
                        color = editColor.trim(),
                    )
                }
                if (result.ok) {
                    val colorStored = normalizeInicialesColor(editColor)
                    rows = rows.map {
                        if (it["id_sacd"] == idSacd) {
                            it.toMutableMap().apply {
                                put("iniciales", editIniciales.trim())
                                put("color", colorStored)
                            }
                        } else {
                            it
                        }
                    }
                    statusMsg = "Iniciales actualizadas."
                    editingRow = null
                } else {
                    error = result.message
                }
            } finally {
                saving = false
            }
        }
    }

    LaunchedEffect(baseUrl) {
        loadingPage = true
        rows = emptyList()
        showFilters = true
        editingRow = null
        statusMsg = null
        try {
            val data = withContext(Dispatchers.IO) {
                fetchModificarInicialesPage(client, baseUrl)
            }
            if (data == null) {
                error = "No se pudo cargar la pantalla."
            } else if (data.zonasOpciones.isEmpty()) {
                error = "No hay zonas disponibles."
                page = data
            } else {
                page = data
                selectedZona = data.zonasOpciones.keys.firstOrNull()
            }
        } finally {
            loadingPage = false
        }
    }

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Iniciales de sacerdotes", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Toca una fila para editar iniciales y color.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (loadingPage) {
            MisasLoadingBox()
            return@Column
        }
        val zonas = page?.zonasOpciones.orEmpty()
        if (zonas.isEmpty()) {
            Text(error ?: "Sin datos", color = MaterialTheme.colorScheme.error)
            return@Column
        }
        if (!showFilters && rows.isNotEmpty()) {
            MisasFilterSummaryBar(
                summary = zonas[selectedZona].orEmpty(),
                onShowFilters = {
                    showFilters = true
                    rows = emptyList()
                    error = null
                    statusMsg = null
                },
            )
        }
        if (showFilters) {
            MisasDropdown(
                label = "Zona",
                value = zonas[selectedZona].orEmpty(),
                expanded = zonaExpanded,
                onExpandedChange = { zonaExpanded = it },
                options = zonas.map { (id, label) -> id to label },
                onSelect = { selectedZona = it },
            )
            Button(
                onClick = { buscar() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loadingGrid,
            ) {
                Text("Ver listado")
            }
        }
        if (loadingGrid) MisasLoadingBox()
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        statusMsg?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
        if (rows.isNotEmpty()) {
            InicialesEditableList(
                rows = rows,
                onEdit = { row ->
                    editingRow = row
                    editIniciales = row["iniciales"].orEmpty()
                    editColor = row["color"].orEmpty().let { c ->
                        if (c.isNotEmpty() && !c.startsWith("#")) "#$c" else c
                    }
                    error = null
                    statusMsg = null
                },
            )
        }
    }

    editingRow?.let { row ->
        AlertDialog(
            onDismissRequest = { if (!saving) editingRow = null },
            title = { Text(row["nombre_sacd"].orEmpty().ifEmpty { "Editar iniciales" }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editIniciales,
                        onValueChange = { editIniciales = it },
                        label = { Text("Iniciales") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !saving,
                    )
                    OutlinedTextField(
                        value = editColor,
                        onValueChange = { editColor = it },
                        label = { Text("Color (#rrggbb)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !saving,
                    )
                    Text(
                        "Paleta rápida",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        INICIALES_COLOR_PRESETS.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        color = runCatching {
                                            Color(android.graphics.Color.parseColor(hex))
                                        }.getOrDefault(Color.Gray),
                                        shape = MaterialTheme.shapes.small,
                                    )
                                    .clickable(enabled = !saving) { editColor = hex },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { guardarIniciales() }, enabled = !saving) {
                    Text(if (saving) "Guardando…" else "Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingRow = null }, enabled = !saving) {
                    Text("Cancelar")
                }
            },
        )
    }
}

private val INICIALES_COLOR_PRESETS = listOf(
    "#000000", "#980000", "#ff0000", "#ff9900", "#00ff00",
    "#4a86e8", "#0000ff", "#9900ff",
)

@Composable
private fun InicialesEditableList(
    rows: List<Map<String, String>>,
    onEdit: (Map<String, String>) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(rows.size) { index ->
            val row = rows[index]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEdit(row) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val colorHex = row["color"].orEmpty()
                    val swatch = normalizeInicialesColor(colorHex)
                    if (swatch.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = runCatching {
                                        Color(android.graphics.Color.parseColor("#$swatch"))
                                    }.getOrDefault(Color.LightGray),
                                    shape = MaterialTheme.shapes.small,
                                ),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            row["nombre_sacd"].orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            "Iniciales: ${row["iniciales"].orEmpty().ifEmpty { "—" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
