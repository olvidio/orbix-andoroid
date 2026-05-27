package com.orbix.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

@Composable
fun NuevoPlanScreen(
    client: OkHttpClient,
    baseUrl: String,
    contentPadding: PaddingValues,
    propuestaCalendario: Boolean,
) {
    var loadingPage by remember { mutableStateOf(true) }
    var loadingResult by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var quePage by remember { mutableStateOf<PlanningCasaQuePage?>(null) }
    var casasOpciones by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var result by remember { mutableStateOf<PlanningCasaVerResult?>(null) }

    var selectedCdcSel by remember { mutableStateOf<String?>(null) }
    var selectedCasaId by remember { mutableStateOf<String?>(null) }
    var selectedPeriodo by remember { mutableStateOf(defaultPlanningCasaPeriodo()) }
    var selectedYear by remember {
        mutableStateOf(Calendar.getInstance().get(Calendar.YEAR).toString())
    }
    var sinActiv by remember { mutableStateOf(false) }

    var cdcExpanded by remember { mutableStateOf(false) }
    var casaExpanded by remember { mutableStateOf(false) }
    var periodoExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(true) }
    var resultsLoaded by remember { mutableStateOf(false) }

    val yearOptions = remember { yearActivOptions() }
    val scope = rememberCoroutineScope()
    val periodoLabels = planningCasaPeriodoLabels()

    val cdcOptions = remember(quePage) {
        cdcSelOptionsForModo(quePage?.modoCasas.orEmpty())
    }
    val needsCasaPick = selectedCdcSel == "9"

    fun reloadCasasOpciones() {
        val page = quePage ?: return
        scope.launch {
            casasOpciones = withContext(Dispatchers.IO) {
                fetchCasasOpciones(client, baseUrl, page.filtroCasas)
            }
            if (selectedCasaId == null || !casasOpciones.containsKey(selectedCasaId)) {
                selectedCasaId = casasOpciones.keys.firstOrNull()
            }
        }
    }

    LaunchedEffect(baseUrl) {
        loadingPage = true
        error = null
        quePage = null
        result = null
        showFilters = true
        resultsLoaded = false
        try {
            val page = withContext(Dispatchers.IO) {
                fetchPlanningCasaQuePage(client, baseUrl)
            }
            if (page == null) {
                error = "No se pudo cargar (planning_casa_que_data)."
            } else {
                quePage = page
                val opts = cdcSelOptionsForModo(page.modoCasas)
                selectedCdcSel = opts.keys.firstOrNull()
            }
        } finally {
            loadingPage = false
        }
    }

    LaunchedEffect(quePage, selectedCdcSel) {
        if (needsCasaPick && quePage != null) {
            reloadCasasOpciones()
        }
    }

    fun buscar() {
        val cdc = selectedCdcSel ?: return
        if (cdc == "9" && selectedCasaId == null) {
            error = "Selecciona una casa."
            return
        }
        scope.launch {
            loadingResult = true
            error = null
            try {
                val data = withContext(Dispatchers.IO) {
                    fetchPlanningCasaVer(
                        client = client,
                        baseUrl = baseUrl,
                        cdcSel = cdc,
                        selectedCasaIds = if (cdc == "9") listOfNotNull(selectedCasaId) else emptyList(),
                        periodo = selectedPeriodo,
                        year = selectedYear,
                        sinActiv = sinActiv,
                        propuestaCalendario = propuestaCalendario,
                    )
                }
                if (data == null) {
                    error = "No se pudo cargar (planning_casa_ver_data)."
                    result = null
                } else {
                    result = data
                    resultsLoaded = true
                    showFilters = false
                    if (data.casas.isEmpty()) {
                        error = "Sin actividades en el periodo seleccionado."
                    }
                }
            } finally {
                loadingResult = false
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (propuestaCalendario) "Nuevo plan" else "Planning por casas",
            style = MaterialTheme.typography.titleMedium,
        )
        if (propuestaCalendario) {
            Text(
                text = "Vista de consulta. Crear o modificar actividades solo en la web.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (loadingPage) {
            MisasLoadingBox()
            return@Column
        }

        if (cdcOptions.isEmpty()) {
            Text(
                text = error ?: "No hay grupos de casas disponibles.",
                color = MaterialTheme.colorScheme.error,
            )
            return@Column
        }

        val filterSummary = buildString {
            append(cdcOptions[selectedCdcSel].orEmpty())
            if (needsCasaPick) {
                append(" · ")
                append(casasOpciones[selectedCasaId].orEmpty())
            }
            append(" · ")
            append(periodoLabels[selectedPeriodo].orEmpty())
            append(" · ")
            append(selectedYear)
            if (sinActiv) append(" · sin actividad")
        }

        if (!showFilters && resultsLoaded) {
            MisasFilterSummaryBar(
                summary = filterSummary,
                onShowFilters = {
                    showFilters = true
                    result = null
                    error = null
                },
            )
        }

        if (showFilters) {
            MisasDropdown(
                label = "Grupo de casas",
                value = cdcOptions[selectedCdcSel].orEmpty(),
                expanded = cdcExpanded,
                onExpandedChange = { cdcExpanded = it },
                options = cdcOptions.map { (id, label) -> id to label },
                onSelect = { selectedCdcSel = it },
            )
            if (needsCasaPick) {
                if (casasOpciones.isEmpty()) {
                    Text(
                        "Cargando casas…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    MisasDropdown(
                        label = "Casa",
                        value = casasOpciones[selectedCasaId].orEmpty(),
                        expanded = casaExpanded,
                        onExpandedChange = { casaExpanded = it },
                        options = casasOpciones.map { (id, label) -> id to label },
                        onSelect = { selectedCasaId = it },
                    )
                }
            }
            MisasDropdown(
                label = "Periodo",
                value = periodoLabels[selectedPeriodo].orEmpty(),
                expanded = periodoExpanded,
                onExpandedChange = { periodoExpanded = it },
                options = periodoLabels.map { (id, label) -> id to label },
                onSelect = { selectedPeriodo = it },
            )
            MisasDropdown(
                label = "Año",
                value = selectedYear,
                expanded = yearExpanded,
                onExpandedChange = { yearExpanded = it },
                options = yearOptions,
                onSelect = { selectedYear = it },
            )
            Button(
                onClick = { sinActiv = !sinActiv },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (sinActiv) {
                        "Incluir casas sin actividad: sí"
                    } else {
                        "Incluir casas sin actividad: no"
                    },
                )
            }
            Button(
                onClick = { buscar() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loadingResult && selectedCdcSel != null &&
                    (!needsCasaPick || selectedCasaId != null),
            ) {
                Text("Ver planning")
            }
        }

        if (loadingResult) MisasLoadingBox()

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        val data = result
        if (data != null && data.casas.isNotEmpty()) {
            val rango = formatIsoRange(data.fIniIso, data.fFinIso)
            if (rango.isNotEmpty()) {
                Text(
                    rango,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(data.casas, key = { it.nombreCasa }) { block ->
                    PlanningCasaBlockCard(block)
                }
            }
        }
    }
}

@Composable
private fun PlanningCasaBlockCard(block: PlanningCasaBlock) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(block.nombreCasa, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (block.actividades.isEmpty()) {
                Text(
                    "Sin actividades",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                block.actividades.forEach { act ->
                    Column(modifier = Modifier.padding(bottom = 4.dp)) {
                        Text(
                            act.nomCurt.ifEmpty { act.nomLlarg },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (act.nomLlarg.isNotEmpty() && act.nomLlarg != act.nomCurt) {
                            Text(
                                act.nomLlarg,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val horario = formatPlanningCasaHorario(act)
                        if (horario.isNotEmpty()) {
                            Text(
                                horario,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatIsoRange(ini: String, fin: String): String {
    if (ini.isEmpty() || fin.isEmpty()) return ""
    return "${formatIsoDate(ini)} – ${formatIsoDate(fin)}"
}

private fun formatIsoDate(iso: String): String {
    val parts = iso.split("-")
    if (parts.size != 3) return iso
    return "${parts[2]}/${parts[1]}/${parts[0]}"
}

private fun formatPlanningCasaHorario(act: PlanningActividad): String {
    val ini = act.hIni
    val fin = act.hFi
    val rangoFechas = if (act.fFi.isNotEmpty() && act.fFi != act.fIni) {
        "${act.fIni} – ${act.fFi}"
    } else {
        act.fIni
    }
    return when {
        ini.isNotEmpty() && fin.isNotEmpty() -> "$rangoFechas · $ini – $fin"
        ini.isNotEmpty() -> "$rangoFechas · $ini"
        rangoFechas.isNotEmpty() -> rangoFechas
        else -> ""
    }
}
