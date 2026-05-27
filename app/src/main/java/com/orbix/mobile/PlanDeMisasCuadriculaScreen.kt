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
            readOnlyNote = "Vista de consulta. La edición de celdas no está disponible en móvil.",
        )
        CuadriculaPlanMode.Preparar -> CuadriculaPlanConfig(
            title = "Preparar plan de misas",
            pantallaApi = "preparar",
            periodoOptions = periodoLabelsPreparar(),
            defaultPeriodo = "proxima_semana",
            showTipoPlantilla = true,
            readOnlyNote = "Vista de consulta. «Preparar» (borrar periodo) solo en la web por ahora.",
        )
        CuadriculaPlanMode.ModificarPlantilla -> CuadriculaPlanConfig(
            title = "Modificar plantilla",
            pantallaApi = "modificar_plantilla",
            showPeriodo = false,
            showTipoPlantilla = true,
            readOnlyNote = "Vista de plantilla. Importar/copiar plantilla solo en la web.",
        )
        CuadriculaPlanMode.CambiarStatus -> CuadriculaPlanConfig(
            title = "Modificar estado del plan",
            useStatusApi = true,
            periodoOptions = periodoLabelsPreparar(),
            defaultPeriodo = "proxima_semana",
            showEstado = true,
            fixedTipoPlantilla = "p",
            readOnlyNote = "Vista de consulta. Cambiar estado requiere la web.",
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
    var error by remember { mutableStateOf<String?>(null) }
    var pantalla by remember { mutableStateOf<PlanDeMisasPantalla?>(null) }
    var statusPantalla by remember { mutableStateOf<CambiarStatusPantalla?>(null) }
    var cuadricula by remember { mutableStateOf<CuadriculaZona?>(null) }

    var selectedZonaId by remember { mutableStateOf<String?>(null) }
    var selectedOrden by remember { mutableStateOf("desc_enc") }
    var selectedPeriodo by remember { mutableStateOf(config.defaultPeriodo) }
    var selectedTipoPlantilla by remember { mutableStateOf<String?>(null) }
    var selectedEstado by remember { mutableStateOf<String?>(null) }

    var zonaExpanded by remember { mutableStateOf(false) }
    var ordenExpanded by remember { mutableStateOf(false) }
    var periodoExpanded by remember { mutableStateOf(false) }
    var plantillaExpanded by remember { mutableStateOf(false) }
    var estadoExpanded by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(true) }

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

    fun reloadGrid() {
        val zona = selectedZonaId ?: return
        scope.launch {
            loadingGrid = true
            error = null
            try {
                val tipo = config.fixedTipoPlantilla
                    ?: selectedTipoPlantilla
                    ?: pantalla?.plantillaSelected
                    ?: "p"
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

    LaunchedEffect(baseUrl, config) {
        loadingPage = true
        error = null
        pantalla = null
        statusPantalla = null
        cuadricula = null
        showFilters = true
        selectedZonaId = null
        selectedPeriodo = config.defaultPeriodo
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
                    if (data.ordenOpciones.containsKey(selectedOrden).not() &&
                        data.ordenOpciones.isNotEmpty()
                    ) {
                        selectedOrden = data.ordenOpciones.keys.first()
                    }
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
                    selectedZonaId = data.zonasOpciones.keys.firstOrNull()
                    selectedTipoPlantilla = data.plantillaSelected
                        ?: data.tiposPlantilla.keys.firstOrNull()
                    if (data.ordenOpciones.containsKey(selectedOrden).not() &&
                        data.ordenOpciones.isNotEmpty()
                    ) {
                        selectedOrden = data.ordenOpciones.keys.first()
                    }
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
        Text(config.title, style = MaterialTheme.typography.titleMedium)
        config.readOnlyNote?.let { MisasReadOnlyNote(it) }

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

        if (!showFilters && cuadricula != null) {
            MisasFilterSummaryBar(summary = filterSummary, onShowFilters = { showFilters = true })
        }

        if (showFilters) {
            MisasDropdown(
                label = "Zona",
                value = zonas[selectedZonaId].orEmpty(),
                expanded = zonaExpanded,
                onExpandedChange = { zonaExpanded = it },
                options = zonas.map { (id, label) -> id to label },
                onSelect = { selectedZonaId = it },
            )
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
            Button(
                onClick = { reloadGrid() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loadingGrid && selectedZonaId != null,
            ) {
                Text("Ver cuadrícula")
            }
        }

        if (loadingGrid) MisasLoadingBox()
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        cuadricula?.preferenceWarning?.takeIf { cuadricula?.rows?.isNotEmpty() == true }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
        }
        val grid = cuadricula
        if (grid != null && grid.rows.isNotEmpty()) {
            CuadriculaTable(grid = grid)
        } else if (!loadingGrid && error == null && !showFilters && selectedZonaId != null) {
            Text(
                "Sin datos para la selección actual.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
