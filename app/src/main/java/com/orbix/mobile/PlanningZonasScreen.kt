package com.orbix.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningZonasScreen(
    client: OkHttpClient,
    baseUrl: String,
    contentPadding: PaddingValues,
) {
    var loadingPage by remember { mutableStateOf(true) }
    var loadingResult by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf<PlanningZonesQuePage?>(null) }
    var result by remember { mutableStateOf<PlanningZonesSelectResult?>(null) }

    var selectedTrimestre by remember { mutableStateOf(defaultPlanningTrimestre()) }
    var selectedYear by remember {
        mutableStateOf(Calendar.getInstance().get(Calendar.YEAR).toString())
    }
    var selectedZonaId by remember { mutableStateOf<String?>(null) }

    var trimestreExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }
    var zonaExpanded by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(true) }

    val yearOptions = remember { yearActivOptions() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(baseUrl) {
        loadingPage = true
        error = null
        page = null
        result = null
        showFilters = true
        selectedZonaId = null
        try {
            val data = withContext(Dispatchers.IO) {
                fetchPlanningZonesQuePage(client, baseUrl)
            }
            if (data == null) {
                error = "No se pudo cargar (planning_zones_que_data)."
            } else {
                page = data
                data.error?.let { error = it }
                selectedZonaId = data.opcionesZonas.keys.firstOrNull()
            }
        } finally {
            loadingPage = false
        }
    }

    fun buscar() {
        val zona = selectedZonaId ?: return
        scope.launch {
            loadingResult = true
            error = null
            try {
                val data = withContext(Dispatchers.IO) {
                    fetchPlanningZonesSelect(
                        client = client,
                        baseUrl = baseUrl,
                        trimestre = selectedTrimestre,
                        year = selectedYear,
                        idZona = zona,
                    )
                }
                if (data == null) {
                    error = "No se pudo cargar (planning_zones_select_data)."
                    result = null
                } else {
                    result = data
                    showFilters = false
                    if (data.planningIniIso.isEmpty() || data.planningFinIso.isEmpty()) {
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
        Text("Planning zonas", style = MaterialTheme.typography.titleMedium)
        Text(
            "Calendario día a día: sacds ocupados; pulsa uno para ver la actividad.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (loadingPage) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        val zonas = page?.opcionesZonas.orEmpty()
        if (zonas.isEmpty()) {
            Text(
                error ?: "No hay zonas disponibles para tu usuario.",
                color = MaterialTheme.colorScheme.error,
            )
            return@Column
        }

        val filterSummary = buildString {
            append(planningTrimestreLabels()[selectedTrimestre].orEmpty())
            append(" · ")
            append(selectedYear)
            append(" · ")
            append(zonas[selectedZonaId].orEmpty())
        }

        if (!showFilters && result != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = filterSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showFilters = true }) {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = "Mostrar filtros",
                    )
                }
            }
        }

        if (showFilters) {
            PlanningDropdown(
                label = "Trimestre o mes",
                value = planningTrimestreLabels()[selectedTrimestre].orEmpty(),
                expanded = trimestreExpanded,
                onExpandedChange = { trimestreExpanded = it },
                options = planningTrimestreLabels().map { (id, label) -> id to label },
                onSelect = {
                    selectedTrimestre = it
                    trimestreExpanded = false
                },
            )

            PlanningDropdown(
                label = "Año",
                value = selectedYear,
                expanded = yearExpanded,
                onExpandedChange = { yearExpanded = it },
                options = yearOptions,
                onSelect = {
                    selectedYear = it
                    yearExpanded = false
                },
            )

            PlanningDropdown(
                label = "Zona",
                value = zonas[selectedZonaId].orEmpty(),
                expanded = zonaExpanded,
                onExpandedChange = { zonaExpanded = it },
                options = zonas.map { (id, label) -> id to label },
                onSelect = {
                    selectedZonaId = it
                    zonaExpanded = false
                },
            )

            Button(
                onClick = { buscar() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loadingResult && selectedZonaId != null,
            ) {
                Text("Ver planning")
            }
        }

        if (loadingResult) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        val data = result
        if (data != null) {
            val agenda = remember(data) { data.toAgenda() }
            var expandedSacdKey by remember(data) { mutableStateOf<String?>(null) }
            val rango = formatIsoRange(data.planningIniIso, data.planningFinIso)
            val diasOcupados = agenda.count { it.sacds.isNotEmpty() }
            if (rango.isNotEmpty()) {
                Text(
                    "${data.titulo} · $rango · $diasOcupados/${agenda.size} días con sacd",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(agenda, key = { it.isoKey }) { dia ->
                    PlanningAgendaDiaSection(
                        dia = dia,
                        expandedSacdKey = expandedSacdKey,
                        onToggleSacd = { key ->
                            expandedSacdKey = if (expandedSacdKey == key) null else key
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanningDropdown(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            options.forEach { (id, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = { onSelect(id) },
                )
            }
        }
    }
}

@Composable
private fun PlanningAgendaDiaSection(
    dia: PlanningAgendaDia,
    expandedSacdKey: String?,
    onToggleSacd: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (dia.sacds.isEmpty()) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                dia.fechaLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (dia.sacds.isEmpty()) {
                Text(
                    "Ningún sacd ocupado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                dia.sacds.forEachIndexed { index, sacd ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    }
                    val itemKey = "${dia.isoKey}|${sacd.persona}"
                    val expanded = expandedSacdKey == itemKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleSacd(itemKey) }
                            .padding(top = if (index == 0) 8.dp else 0.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            sacd.persona,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = if (expanded) {
                                Icons.Filled.ExpandLess
                            } else {
                                Icons.Filled.ExpandMore
                            },
                            contentDescription = if (expanded) "Ocultar actividad" else "Ver actividad",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (expanded) {
                        sacd.actividades.forEach { act ->
                            Column(modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)) {
                                Text(
                                    act.nomCurt.ifEmpty { act.nomLlarg },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                )
                                if (act.nomLlarg.isNotEmpty() && act.nomLlarg != act.nomCurt) {
                                    Text(
                                        act.nomLlarg,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                val horario = formatActividadHorario(act)
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

private fun formatActividadHorario(act: PlanningActividad): String {
    val ini = act.hIni
    val fin = act.hFi
    val rangoFechas = if (act.fFi.isNotEmpty() && act.fFi != act.fIni) {
        "${act.fIni} – ${act.fFi}"
    } else {
        ""
    }
    return when {
        ini.isNotEmpty() && fin.isNotEmpty() -> "$ini – $fin"
        ini.isNotEmpty() -> ini
        rangoFechas.isNotEmpty() -> rangoFechas
        else -> ""
    }
}
