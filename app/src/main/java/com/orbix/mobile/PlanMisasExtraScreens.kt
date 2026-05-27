package com.orbix.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
fun EncargosZonaScreen(
    client: OkHttpClient,
    baseUrl: String,
    contentPadding: PaddingValues,
) {
    ZonaOrdenListScreen(
        client = client,
        baseUrl = baseUrl,
        contentPadding = contentPadding,
        title = "Encargos de la zona",
        loadPage = { fetchModificarEncargosPage(it.first, it.second) },
        loadGrid = { c, u, z, o ->
            fetchVerEncargosZona(c, u, idZona = z, orden = o)
        },
        columns = listOf("Encargo", "Tipo", "Lugar", "Orden"),
        columnKeys = listOf("encargo", "tipo_encargo", "lugar", "orden"),
        readOnlyNote = "Vista de consulta. Crear/editar encargos solo en la web.",
    )
}

@Composable
fun EncargosCentrosScreen(
    client: OkHttpClient,
    baseUrl: String,
    contentPadding: PaddingValues,
) {
    ZonaOrdenListScreen(
        client = client,
        baseUrl = baseUrl,
        contentPadding = contentPadding,
        title = "Encargos visibles por centro",
        loadPage = { fetchModificarEncargosCentrosPage(it.first, it.second) },
        loadGrid = { c, u, z, _ -> fetchVerEncargosCentros(c, u, idZona = z) },
        columns = listOf("Centro", "Encargo"),
        columnKeys = listOf("centro", "encargo"),
        showOrden = false,
        readOnlyNote = "Vista de consulta. Edición solo en la web.",
    )
}

@Composable
fun InicialesZonaScreen(
    client: OkHttpClient,
    baseUrl: String,
    contentPadding: PaddingValues,
) {
    ZonaOrdenListScreen(
        client = client,
        baseUrl = baseUrl,
        contentPadding = contentPadding,
        title = "Iniciales de sacerdotes",
        loadPage = { fetchModificarInicialesPage(it.first, it.second) },
        loadGrid = { c, u, z, _ -> fetchVerInicialesZona(c, u, idZona = z) },
        columns = listOf("Nombre", "Iniciales", "Color"),
        columnKeys = listOf("nombre_sacd", "iniciales", "color"),
        showOrden = false,
        readOnlyNote = "Vista de consulta. Editar iniciales solo en la web.",
    )
}

@Composable
private fun ZonaOrdenListScreen(
    client: OkHttpClient,
    baseUrl: String,
    contentPadding: PaddingValues,
    title: String,
    loadPage: suspend (Pair<OkHttpClient, String>) -> ZonaOpcionesPage?,
    loadGrid: suspend (OkHttpClient, String, String, String) -> Any?,
    columns: List<String>,
    columnKeys: List<String>,
    showOrden: Boolean = true,
    readOnlyNote: String? = null,
) {
    var loadingPage by remember { mutableStateOf(true) }
    var loadingGrid by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf<ZonaOpcionesPage?>(null) }
    var rows by remember { mutableStateOf<List<Map<String, String>>?>(null) }

    var selectedZona by remember { mutableStateOf<String?>(null) }
    var selectedOrden by remember { mutableStateOf("desc_enc") }
    var zonaExpanded by remember { mutableStateOf(false) }
    var ordenExpanded by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(baseUrl) {
        loadingPage = true
        rows = null
        showFilters = true
        try {
            val data = withContext(Dispatchers.IO) { loadPage(client to baseUrl) }
            if (data == null) {
                error = "No se pudo cargar la pantalla."
            } else if (data.error != null) {
                error = data.error
                page = data
            } else if (data.zonasOpciones.isEmpty()) {
                error = "No hay zonas disponibles."
            } else {
                page = data
                selectedZona = data.zonasOpciones.keys.firstOrNull()
                if (data.ordenOpciones.isNotEmpty()) {
                    selectedOrden = data.ordenOpciones.keys.first()
                }
            }
        } finally {
            loadingPage = false
        }
    }

    fun buscar() {
        val zona = selectedZona ?: return
        scope.launch {
            loadingGrid = true
            error = null
            try {
                val result = withContext(Dispatchers.IO) {
                    loadGrid(client, baseUrl, zona, selectedOrden)
                }
                when (result) {
                    is EncargosZonaGrid -> rows = result.rows
                    is EncargosCentrosGrid -> rows = result.rows
                    is InicialesZonaGrid -> rows = result.rows
                    else -> rows = null
                }
                if (rows == null) {
                    error = "No se pudo cargar los datos."
                } else {
                    showFilters = false
                    if (rows!!.isEmpty()) error = "Sin registros."
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
        Text(title, style = MaterialTheme.typography.titleMedium)
        readOnlyNote?.let { MisasReadOnlyNote(it) }
        if (loadingPage) {
            MisasLoadingBox()
            return@Column
        }
        val zonas = page?.zonasOpciones.orEmpty()
        if (zonas.isEmpty()) {
            Text(error ?: "Sin datos", color = MaterialTheme.colorScheme.error)
            return@Column
        }
        if (!showFilters && rows != null) {
            MisasFilterSummaryBar(
                summary = zonas[selectedZona].orEmpty(),
                onShowFilters = { showFilters = true },
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
            if (showOrden && page?.ordenOpciones?.isNotEmpty() == true) {
                MisasDropdown(
                    label = "Orden",
                    value = page!!.ordenOpciones[selectedOrden].orEmpty(),
                    expanded = ordenExpanded,
                    onExpandedChange = { ordenExpanded = it },
                    options = page!!.ordenOpciones.map { (id, label) -> id to label },
                    onSelect = { selectedOrden = it },
                )
            }
            Button(onClick = { buscar() }, modifier = Modifier.fillMaxWidth(), enabled = !loadingGrid) {
                Text("Ver listado")
            }
        }
        if (loadingGrid) MisasLoadingBox()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        rows?.let {
            if (it.isNotEmpty()) {
                SimpleRowsTable(columns = columns, rows = it, columnKeys = columnKeys)
            }
        }
    }
}
