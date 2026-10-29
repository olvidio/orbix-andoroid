package com.orbix.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

enum class CuadriculaPlanMode {
    Ver,
    Modificar,
    Preparar,
    ModificarPlantilla,
    CambiarStatus,
}

data class CuadriculaPlanConfig(
    val title: String,
    val pantallaApi: String? = null,
    val useStatusApi: Boolean = false,
    val periodoOptions: Map<String, String> = emptyMap(),
    val defaultPeriodo: String = "este_mes",
    val showPeriodo: Boolean = true,
    val showOrden: Boolean = true,
    val showTipoPlantilla: Boolean = false,
    val showEstado: Boolean = false,
    val fixedTipoPlantilla: String? = null,
    val readOnlyNote: String? = null,
    /** Si true, muestra botón para POST `/src/misas/nuevo_status`. */
    val applyStatus: Boolean = false,
    /** Permite tocar celdas y guardar con `cuadricula_update`. */
    val editableCells: Boolean = false,
    /** Botón preparar periodo (`crear_nuevo_periodo_data`). */
    val canPreparar: Boolean = false,
    /** Importar plantilla origen → destino. */
    val canImportarPlantilla: Boolean = false,
    val defaultOrden: String = "desc_enc",
    val showVerCuadricula: Boolean = true,
)

fun cuadriculaPlanConfig(mode: CuadriculaPlanMode): CuadriculaPlanConfig {
    return when (mode) {
        CuadriculaPlanMode.Ver -> CuadriculaPlanConfig(
            title = "Ver plan de misas",
            pantallaApi = "ver",
            periodoOptions = periodoLabelsVer(),
            defaultPeriodo = "este_mes",
        )
        CuadriculaPlanMode.Modificar -> CuadriculaPlanConfig(
            title = "Modificar plan de misas",
            pantallaApi = "modificar",
            periodoOptions = periodoLabelsVer(),
            defaultPeriodo = "proximo_mes",
            defaultOrden = "orden",
            editableCells = true,
            readOnlyNote = "Toca una celda (fondo suave) para asignar o borrar sacerdote.",
        )
        CuadriculaPlanMode.Preparar -> CuadriculaPlanConfig(
            title = "Preparar plan de misas",
            pantallaApi = "preparar",
            periodoOptions = periodoLabelsPreparar(),
            defaultPeriodo = "proxima_semana",
            showTipoPlantilla = true,
            canPreparar = true,
            defaultOrden = "orden",
            showVerCuadricula = false,
            readOnlyNote = "«Preparar periodo» recrea el periodo desde la plantilla. Para editar celdas usa «Modificar plan».",
        )
        CuadriculaPlanMode.ModificarPlantilla -> CuadriculaPlanConfig(
            title = "Modificar plantilla",
            pantallaApi = "modificar_plantilla",
            showPeriodo = false,
            showTipoPlantilla = true,
            editableCells = true,
            canImportarPlantilla = true,
            defaultOrden = "orden",
            readOnlyNote = "Edita celdas o importa desde otra plantilla.",
        )
        CuadriculaPlanMode.CambiarStatus -> CuadriculaPlanConfig(
            title = "Modificar estado del plan",
            useStatusApi = true,
            periodoOptions = periodoLabelsPreparar(),
            defaultPeriodo = "proxima_semana",
            showEstado = true,
            fixedTipoPlantilla = "p",
            applyStatus = true,
        )
    }
}

@Composable
fun PlanDeMisasCuadriculaScreen(
    client: OkHttpClient,
    baseUrl: String,
    contentPadding: PaddingValues,
    mode: CuadriculaPlanMode,
) {
    PlanDeMisasCuadriculaScreen(
        client = client,
        baseUrl = baseUrl,
        contentPadding = contentPadding,
        config = cuadriculaPlanConfig(mode),
    )
}

@Composable
fun PlanDeMisasCuadriculaScreen(
    client: OkHttpClient,
    baseUrl: String,
    contentPadding: PaddingValues,
    config: CuadriculaPlanConfig,
) {
    var loadingPage by remember { mutableStateOf(true) }
    var loadingGrid by remember { mutableStateOf(false) }
    var applyingStatus by remember { mutableStateOf(false) }
    var mutating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var pantalla by remember { mutableStateOf<PlanDeMisasPantalla?>(null) }
    var statusPantalla by remember { mutableStateOf<CambiarStatusPantalla?>(null) }
    var cuadricula by remember { mutableStateOf<CuadriculaZona?>(null) }

    var selectedZonaId by remember { mutableStateOf<String?>(null) }
    var selectedOrden by remember(config) { mutableStateOf(config.defaultOrden) }
    var ctxCrear by remember { mutableStateOf("") }
    var ctxImportar by remember { mutableStateOf("") }
    var selectedPeriodo by remember { mutableStateOf(config.defaultPeriodo) }
    var selectedTipoPlantilla by remember { mutableStateOf<String?>(null) }
    var selectedEstado by remember { mutableStateOf<String?>(null) }
    var importOrigen by remember { mutableStateOf<String?>(null) }

    var zonaExpanded by remember { mutableStateOf(false) }
    var ordenExpanded by remember { mutableStateOf(false) }
    var periodoExpanded by remember { mutableStateOf(false) }
    var plantillaExpanded by remember { mutableStateOf(false) }
    var estadoExpanded by remember { mutableStateOf(false) }
    var importOrigenExpanded by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(true) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showLegend by remember { mutableStateOf(false) }

    var editingMeta by remember { mutableStateOf<CuadriculaCellMeta?>(null) }
    var editEncargoLabel by remember { mutableStateOf("") }
    var editKey by remember { mutableStateOf("") }
    var editTstart by remember { mutableStateOf("") }
    var editTend by remember { mutableStateOf("") }
    var editObserv by remember { mutableStateOf("") }
    var editStatus by remember { mutableStateOf("") }
    var editSeleccion by remember { mutableStateOf("2") }
    var editIdSacdInicial by remember { mutableStateOf("") }
    var sacdOpciones by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var sacdExpanded by remember { mutableStateOf(false) }
    var editStatusExpanded by remember { mutableStateOf(false) }
    var loadingSacd by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val zonas = when {
        config.useStatusApi -> statusPantalla?.zonasOpciones.orEmpty()
        else -> pantalla?.zonasOpciones.orEmpty()
    }
    val ordenOpciones = when {
        config.useStatusApi -> statusPantalla?.ordenOpciones.orEmpty()
        else -> pantalla?.ordenOpciones.orEmpty()
    }
    val tiposPlantilla = pantalla?.tiposPlantilla.orEmpty()

    fun currentTipoPlantilla(): String {
        return config.fixedTipoPlantilla
            ?: selectedTipoPlantilla
            ?: pantalla?.plantillaSelected
            ?: "p"
    }

    fun reloadGrid(forceTipo: String? = null) {
        val zona = selectedZonaId ?: return
        scope.launch {
            loadingGrid = true
            error = null
            try {
                val tipo = forceTipo ?: currentTipoPlantilla()
                val grid = withContext(Dispatchers.IO) {
                    fetchCuadriculaZona(
                        client = client,
                        baseUrl = baseUrl,
                        idZona = zona,
                        periodo = if (config.showPeriodo) selectedPeriodo else "",
                        orden = selectedOrden,
                        tipoPlantilla = tipo,
                    )
                }
                if (grid == null) {
                    error = "No se pudo cargar la cuadrícula."
                } else if (grid.preferenceWarning?.isNotEmpty() == true &&
                    grid.rows.isEmpty() &&
                    grid.dateColumns.isEmpty()
                ) {
                    error = grid.preferenceWarning
                    cuadricula = null
                } else {
                    cuadricula = grid
                    showFilters = false
                }
            } finally {
                loadingGrid = false
            }
        }
    }

    fun applyNuevoStatus() {
        val zona = selectedZonaId ?: return
        val estado = selectedEstado ?: return
        scope.launch {
            applyingStatus = true
            error = null
            statusMsg = null
            try {
                val result = withContext(Dispatchers.IO) {
                    postNuevoStatus(
                        client = client,
                        baseUrl = baseUrl,
                        idZona = zona,
                        ctxNuevoStatus = statusPantalla?.zonaCtxMap?.get(zona).orEmpty(),
                        periodo = selectedPeriodo,
                        estado = estado,
                    )
                }
                if (result.ok) {
                    statusMsg = "Estado aplicado."
                    reloadGrid()
                } else {
                    error = result.message
                }
            } finally {
                applyingStatus = false
            }
        }
    }

    fun prepararPeriodo() {
        val zona = selectedZonaId ?: return
        val tipo = selectedTipoPlantilla ?: return
        if (ctxCrear.isBlank()) {
            error = "No se pudo validar la operación. Vuelve a abrir la pantalla."
            return
        }
        scope.launch {
            mutating = true
            error = null
            statusMsg = null
            try {
                val result = withContext(Dispatchers.IO) {
                    postCrearNuevoPeriodo(
                        client = client,
                        baseUrl = baseUrl,
                        idZona = zona,
                        tipoPlantilla = tipo,
                        periodo = selectedPeriodo,
                        orden = selectedOrden,
                        ctxCrear = ctxCrear,
                    )
                }
                if (!result.ok) {
                    error = result.message
                } else {
                    statusMsg = "Periodo preparado desde plantilla."
                    // Tras preparar, la vista operativa es plan (`p`).
                    selectedTipoPlantilla = "p"
                    showFilters = false
                    // Respuesta de crear periodo no incluye bloque SACD; recargar cuadrícula completa.
                    reloadGrid(forceTipo = "p")
                }
            } finally {
                mutating = false
            }
        }
    }

    fun importarPlantilla() {
        val zona = selectedZonaId ?: return
        val origen = importOrigen ?: return
        val destino = selectedTipoPlantilla ?: return
        if (origen == destino) {
            error = "Origen y destino deben ser distintos."
            return
        }
        if (origen.firstOrNull() == destino.firstOrNull() &&
            origen.firstOrNull() in setOf('s', 'd', 'm')
        ) {
            // Misma familia se permite en API; la web solo avisa de mismo prefijo
            // en algunos casos. No bloqueamos aquí salvo igualdad exacta.
        }
        scope.launch {
            mutating = true
            error = null
            statusMsg = null
            try {
                val result = withContext(Dispatchers.IO) {
                    postImportarPlantilla(
                        client,
                        baseUrl,
                        idZona = zona,
                        tipoOrigen = origen,
                        tipoDestino = destino,
                        ctxImportar = ctxImportar,
                    )
                }
                if (result.ok) {
                    statusMsg = "Plantilla importada."
                    showImportDialog = false
                    reloadGrid()
                } else {
                    error = result.message
                }
            } finally {
                mutating = false
            }
        }
    }

    fun openCellEditor(row: CuadriculaRow, meta: CuadriculaCellMeta) {
        if (selectedZonaId == null) return
        editingMeta = meta
        editEncargoLabel = row.encargo
        editKey = meta.key
        editTstart = meta.tstart
        editTend = meta.tend
        editObserv = meta.observ
        editStatus = meta.status.ifEmpty { cuadricula?.statusPropuesta ?: "1" }
        editSeleccion = "2"
        editIdSacdInicial = idNomFromSacdKey(meta.key)
        sacdOpciones = emptyMap()
        error = null
        statusMsg = null
    }

    LaunchedEffect(editingMeta?.dia, editingMeta?.idEnc, editSeleccion, selectedZonaId) {
        val meta = editingMeta ?: return@LaunchedEffect
        val zona = selectedZonaId ?: return@LaunchedEffect
        loadingSacd = true
        try {
            val opts = withContext(Dispatchers.IO) {
                fetchDesplegableSacd(
                    client = client,
                    baseUrl = baseUrl,
                    idZona = zona,
                    dia = meta.dia,
                    idSacd = editIdSacdInicial,
                    seleccion = editSeleccion,
                )
            }
            sacdOpciones = opts?.opciones.orEmpty()
            val preselect = editKey.ifEmpty { opts?.selected.orEmpty() }
            val resolved = resolveSacdDropdownKey(preselect, sacdOpciones)
            editKey = resolved
        } finally {
            loadingSacd = false
        }
    }

    fun saveCell(clear: Boolean = false) {
        val meta = editingMeta ?: return
        val zona = selectedZonaId ?: return
        scope.launch {
            mutating = true
            error = null
            statusMsg = null
            try {
                val key = if (clear) "" else normalizeSacdKeyForUpdate(editKey)
                val result = withContext(Dispatchers.IO) {
                    postCuadriculaUpdate(
                        client = client,
                        baseUrl = baseUrl,
                        uuidItem = ensureEncargoDiaUuid(meta.uuidItem),
                        key = key,
                        idEnc = meta.idEnc,
                        dia = meta.dia,
                        tstart = if (clear) "" else editTstart.trim(),
                        tend = if (clear) "" else editTend.trim(),
                        observ = if (clear) "" else editObserv.trim(),
                        tipoPlantilla = currentTipoPlantilla(),
                        idZona = zona,
                        ctxUpdate = cuadricula?.ctxUpdate.orEmpty(),
                        status = if (currentTipoPlantilla() == "p" && !clear) {
                            editStatus.trim()
                        } else {
                            ""
                        },
                    )
                }
                if (result.ok) {
                    statusMsg = if (clear) "Celda borrada." else "Celda guardada."
                    editingMeta = null
                    reloadGrid()
                } else {
                    error = result.message
                }
            } finally {
                mutating = false
            }
        }
    }

    LaunchedEffect(baseUrl, config) {
        loadingPage = true
        error = null
        statusMsg = null
        pantalla = null
        statusPantalla = null
        cuadricula = null
        showFilters = true
        editingMeta = null
        selectedZonaId = null
        selectedPeriodo = config.defaultPeriodo
        selectedOrden = config.defaultOrden
        ctxCrear = ""
        ctxImportar = ""
        try {
            if (config.useStatusApi) {
                val data = withContext(Dispatchers.IO) {
                    fetchCambiarStatusPantalla(client, baseUrl)
                }
                if (data == null) {
                    error = "No se pudo cargar (cambiar_status_data)."
                } else {
                    statusPantalla = data
                    selectedZonaId = data.zonasOpciones.keys.firstOrNull()
                    selectedEstado = data.estadosOpciones.keys.firstOrNull()
                    selectedOrden = resolveOrdenSeleccion(data.ordenOpciones, config.defaultOrden)
                }
            } else {
                val api = config.pantallaApi ?: "ver"
                val data = withContext(Dispatchers.IO) {
                    fetchPlanDeMisasPantalla(client, baseUrl, pantalla = api)
                }
                if (data == null) {
                    error = "No se pudo cargar (plan_de_misas_pantalla_data)."
                } else {
                    pantalla = data
                    ctxCrear = data.ctxCrear
                    ctxImportar = data.ctxImportar
                    selectedZonaId = data.zonasOpciones.keys.firstOrNull()
                    selectedTipoPlantilla = data.plantillaSelected
                        ?: data.tiposPlantilla.keys.firstOrNull()
                    importOrigen = data.tiposPlantilla.keys.firstOrNull {
                        it != selectedTipoPlantilla
                    }
                    selectedOrden = resolveOrdenSeleccion(data.ordenOpciones, config.defaultOrden)
                }
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
        val summaryBarVisible = !showFilters && cuadricula != null

        if (loadingPage) {
            MisasLoadingBox()
            return@Column
        }
        if (zonas.isEmpty()) {
            Text(error ?: "No hay zonas disponibles.", color = MaterialTheme.colorScheme.error)
            return@Column
        }

        val filterSummary = buildString {
            append(zonas[selectedZonaId].orEmpty())
            if (config.showEstado) {
                append(" · ")
                append(statusPantalla?.estadosOpciones?.get(selectedEstado).orEmpty())
            }
            if (config.showPeriodo) {
                append(" · ")
                append(config.periodoOptions[selectedPeriodo].orEmpty())
            }
            if (config.showTipoPlantilla && config.fixedTipoPlantilla == null) {
                append(" · ")
                append(tiposPlantilla[selectedTipoPlantilla].orEmpty())
            }
            if (config.showOrden) {
                append(" · ")
                append(ordenOpciones[selectedOrden].orEmpty())
            }
        }

        if (summaryBarVisible) {
            MisasFilterSummaryBar(
                summary = filterSummary,
                onShowFilters = { showFilters = true },
                onShowLegend = { showLegend = true },
            )
        }

        if (showFilters) {
            MisasFilterPanel(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MisasDropdown(
                        label = "Zona",
                        value = zonas[selectedZonaId].orEmpty(),
                        expanded = zonaExpanded,
                        onExpandedChange = { zonaExpanded = it },
                        options = zonas.map { (id, label) -> id to label },
                        onSelect = { selectedZonaId = it },
                        modifier = Modifier.weight(1f),
                    )
                    MisasLegendButton { showLegend = true }
                }
                if (config.showEstado) {
                    val estados = statusPantalla?.estadosOpciones.orEmpty()
                    MisasDropdown(
                        label = "Estado",
                        value = estados[selectedEstado].orEmpty(),
                        expanded = estadoExpanded,
                        onExpandedChange = { estadoExpanded = it },
                        options = estados.map { (id, label) -> id to label },
                        onSelect = { selectedEstado = it },
                    )
                }
                if (config.showTipoPlantilla && config.fixedTipoPlantilla == null && tiposPlantilla.isNotEmpty()) {
                    MisasDropdown(
                        label = "Tipo plantilla",
                        value = tiposPlantilla[selectedTipoPlantilla].orEmpty(),
                        expanded = plantillaExpanded,
                        onExpandedChange = { plantillaExpanded = it },
                        options = tiposPlantilla.map { (id, label) -> id to label },
                        onSelect = { selectedTipoPlantilla = it },
                    )
                }
                if (config.showOrden && ordenOpciones.isNotEmpty()) {
                    MisasDropdown(
                        label = "Orden",
                        value = ordenOpciones[selectedOrden].orEmpty(),
                        expanded = ordenExpanded,
                        onExpandedChange = { ordenExpanded = it },
                        options = ordenOpciones.map { (id, label) -> id to label },
                        onSelect = { selectedOrden = it },
                    )
                }
                if (config.showPeriodo) {
                    MisasDropdown(
                        label = "Periodo",
                        value = config.periodoOptions[selectedPeriodo].orEmpty(),
                        expanded = periodoExpanded,
                        onExpandedChange = { periodoExpanded = it },
                        options = config.periodoOptions.map { (id, label) -> id to label },
                        onSelect = { selectedPeriodo = it },
                    )
                }
                if (config.showVerCuadricula) {
                    Button(
                        onClick = { reloadGrid() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loadingGrid && !applyingStatus && !mutating && selectedZonaId != null,
                    ) {
                        Text("Ver cuadrícula")
                    }
                }
                if (config.canPreparar) {
                    Button(
                        onClick = { prepararPeriodo() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loadingGrid &&
                            !mutating &&
                            selectedZonaId != null &&
                            selectedTipoPlantilla != null &&
                            ctxCrear.isNotEmpty(),
                    ) {
                        Text(if (mutating) "Preparando…" else "Preparar periodo")
                    }
                }
                if (config.applyStatus) {
                    Button(
                        onClick = { applyNuevoStatus() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loadingGrid &&
                            !applyingStatus &&
                            selectedZonaId != null &&
                            selectedEstado != null,
                    ) {
                        Text(if (applyingStatus) "Aplicando…" else "Aplicar estado")
                    }
                }
            }
        }

        if (!showFilters && config.canImportarPlantilla && cuadricula != null) {
            OutlinedButton(
                onClick = {
                    showImportDialog = true
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !mutating && !loadingGrid,
            ) {
                Text("Importar plantilla…")
            }
        }

        if (loadingGrid || applyingStatus || mutating) MisasLoadingBox()
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        statusMsg?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
        cuadricula?.preferenceWarning?.takeIf { cuadricula?.rows?.isNotEmpty() == true }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
        }
        val grid = cuadricula
        if (grid != null && grid.rows.isNotEmpty()) {
            CuadriculaTable(
                grid = grid,
                editable = config.editableCells,
                onCellClick = if (config.editableCells) {
                    { row, _, meta -> openCellEditor(row, meta) }
                } else {
                    null
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        } else if (!loadingGrid && error == null && !showFilters && selectedZonaId != null) {
            Text(
                "Sin datos para la selección actual.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    val planEstados = cuadricula?.estadosOpciones.orEmpty()
    val showEstadoEncargo = currentTipoPlantilla() == "p" && planEstados.isNotEmpty()

    editingMeta?.let { meta ->
        AlertDialog(
            onDismissRequest = { if (!mutating) editingMeta = null },
            title = { Text("${editEncargoLabel.ifEmpty { "Celda" }} · ${meta.dia}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Selección de sacerdote",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    cuadriculaSacdSeleccionOpciones().forEach { (id, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = editSeleccion == id,
                                    onClick = { if (!mutating) editSeleccion = id },
                                    role = Role.RadioButton,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = editSeleccion == id,
                                onClick = null,
                                enabled = !mutating,
                            )
                            Text(label, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (loadingSacd) {
                        MisasLoadingBox()
                    } else {
                        MisasDropdown(
                            label = "Sacerdote",
                            value = sacdOpciones[editKey].orEmpty().ifEmpty {
                                if (editKey.isEmpty()) "(vacío / borrar)" else editKey
                            },
                            expanded = sacdExpanded,
                            onExpandedChange = { sacdExpanded = it },
                            options = listOf("" to "(vacío / borrar)") +
                                sacdOpciones.map { (id, label) -> id to label },
                            onSelect = { editKey = it },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LocalTimeField(
                            label = "Inicio",
                            value = editTstart,
                            onValueChange = { newStart ->
                                val prevDefaultEnd = if (editTstart.isNotEmpty()) {
                                    addMinutesToLocalTime(editTstart, 45)
                                } else {
                                    null
                                }
                                editTstart = newStart
                                if (newStart.isNotEmpty()) {
                                    if (editTend.isBlank() || editTend == prevDefaultEnd) {
                                        editTend = addMinutesToLocalTime(newStart, 45)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !mutating,
                        )
                        LocalTimeField(
                            label = "Fin",
                            value = editTend,
                            onValueChange = { editTend = it },
                            modifier = Modifier.weight(1f),
                            enabled = !mutating,
                        )
                    }
                    if (showEstadoEncargo) {
                        MisasDropdown(
                            label = "Estado",
                            value = planEstados[editStatus].orEmpty(),
                            expanded = editStatusExpanded,
                            onExpandedChange = { editStatusExpanded = it },
                            options = planEstados.map { (id, label) -> id to label },
                            onSelect = { editStatus = it },
                        )
                    }
                    OutlinedTextField(
                        value = editObserv,
                        onValueChange = { editObserv = it },
                        label = { Text("Observaciones") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !mutating,
                    )
                    if (meta.texto.isNotEmpty()) {
                        Text(
                            meta.texto,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { saveCell(clear = false) },
                    enabled = !mutating && !loadingSacd,
                ) {
                    Text(if (mutating) "Guardando…" else "Guardar")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = { saveCell(clear = true) },
                        enabled = !mutating && meta.uuidItem.isNotEmpty(),
                    ) {
                        Text("Borrar", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { editingMeta = null }, enabled = !mutating) {
                        Text("Cancelar")
                    }
                }
            },
        )
    }

    if (showLegend) CuadriculaLegendDialog(note = config.readOnlyNote, onDismiss = { showLegend = false })

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { if (!mutating) showImportDialog = false },
            title = { Text("Importar plantilla") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Copia asignaciones de la plantilla origen a la destino actual.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    MisasDropdown(
                        label = "Origen",
                        value = tiposPlantilla[importOrigen].orEmpty(),
                        expanded = importOrigenExpanded,
                        onExpandedChange = { importOrigenExpanded = it },
                        options = tiposPlantilla
                            .filterKeys { it != selectedTipoPlantilla }
                            .map { (id, label) -> id to label },
                        onSelect = { importOrigen = it },
                    )
                    Text(
                        "Destino: ${tiposPlantilla[selectedTipoPlantilla].orEmpty()}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { importarPlantilla() },
                    enabled = !mutating && importOrigen != null && selectedTipoPlantilla != null,
                ) {
                    Text(if (mutating) "Importando…" else "Importar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }, enabled = !mutating) {
                    Text("Cancelar")
                }
            },
        )
    }
}

private fun resolveOrdenSeleccion(
    opciones: Map<String, String>,
    preferred: String,
): String {
    if (opciones.isEmpty()) return preferred
    if (opciones.containsKey(preferred)) return preferred
    return opciones.keys.first()
}

private fun cuadriculaSacdSeleccionOpciones(): List<Pair<String, String>> = listOf(
    "1" to "Libre a 1ª hora",
    "2" to "Zona",
    "4" to "DL",
    "8" to "De paso",
)

/** Alineado con `resolveSacdSelectedKey` en la cuadrícula web. */
private fun resolveSacdDropdownKey(preselect: String, opciones: Map<String, String>): String {
    if (preselect.isEmpty()) return ""
    if (opciones.containsKey(preselect)) return preselect
    val parts = preselect.split('#')
    if (parts.size == 2) {
        val reversed = "${parts[1]}#${parts[0]}"
        if (opciones.containsKey(reversed)) return reversed
    }
    return preselect
}
