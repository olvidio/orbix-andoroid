package com.orbix.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtencionActividadesScreen(
    client: OkHttpClient,
    baseUrl: String,
    contentPadding: PaddingValues,
) {
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<ComunicacionActivSacdResult?>(null) }

    var selectedPeriodo by remember { mutableStateOf("trimestre_1") }
    val yearOptions = remember { yearActivOptions() }
    var selectedYear by remember {
        mutableStateOf(Calendar.getInstance().get(Calendar.YEAR).toString())
    }
    var periodoExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(baseUrl) {
        result = null
        error = null
        showFilters = true
    }

    fun buscar() {
        scope.launch {
            loading = true
            error = null
            try {
                val data = withContext(Dispatchers.IO) {
                    fetchComunicacionActivSacd(
                        client = client,
                        baseUrl = baseUrl,
                        periodo = selectedPeriodo,
                        year = selectedYear,
                    )
                }
                if (data == null) {
                    error = "No se pudo cargar (comunicacion_activ_sacd_data)."
                    result = null
                } else if (data.mensajePeriodo != null) {
                    error = data.mensajePeriodo
                    result = null
                } else {
                    result = data
                    showFilters = false
                    if (data.sacds.isEmpty() && data.sacdsPaso.isEmpty()) {
                        error = "Sin datos para el periodo seleccionado."
                    }
                }
            } finally {
                loading = false
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
            "Atención actividades",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "Lista para comunicar a los sacd (misma consulta que «buscar» en la web).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val filterSummary = buildString {
            append(periodoActivLabels()[selectedPeriodo].orEmpty())
            append(" · ")
            append(selectedYear)
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
            ActivPeriodoDropdown(
                label = "Periodo",
                value = periodoActivLabels()[selectedPeriodo].orEmpty(),
                expanded = periodoExpanded,
                onExpandedChange = { periodoExpanded = it },
                options = periodoActivLabels().map { (id, label) -> id to label },
                onSelect = {
                    selectedPeriodo = it
                    periodoExpanded = false
                },
            )

            ActivPeriodoDropdown(
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

            Button(
                onClick = { buscar() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
            ) {
                Text("Buscar")
            }
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        val data = result
        if (data != null && (data.sacds.isNotEmpty() || data.sacdsPaso.isNotEmpty())) {
            if (data.periodoTxt.isNotEmpty()) {
                Text(
                    data.periodoTxt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(data.sacds, key = { it.idNom }) { sacd ->
                    SacdActividadesCard(sacd = sacd, footer = data.lugarFecha)
                }
                if (data.sacdsPaso.isNotEmpty()) {
                    item {
                        Text(
                            "Sacd de paso",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(data.sacdsPaso, key = { "paso_${it.idNom}" }) { sacd ->
                        SacdActividadesCard(sacd = sacd, footer = data.lugarFecha)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivPeriodoDropdown(
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
private fun SacdActividadesCard(
    sacd: SacdComunicacion,
    footer: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(sacd.nomAp, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            val intro = sacd.txt["com_sacd"].orEmpty()
            if (intro.isNotEmpty() && intro != "-") {
                Text(
                    intro,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
            if (sacd.actividades.isEmpty()) {
                Text(
                    "Sin actividades en el periodo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ActividadesTable(sacd = sacd)
            }
            if (footer.isNotEmpty()) {
                Text(
                    footer,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ActividadesTable(sacd: SacdComunicacion) {
    val txt = sacd.txt
    val columns = listOf(
        txt["t_f_ini"].orEmpty().ifEmpty { "Inicio" } to { a: ActividadComunicacion ->
            (if (a.propio) "* " else "") + a.fIni
        },
        txt["t_f_fin"].orEmpty().ifEmpty { "Final" } to { a: ActividadComunicacion -> a.fFin },
        txt["t_nombre_ubi"].orEmpty().ifEmpty { "Lugar" } to { a: ActividadComunicacion -> a.nombreUbi },
        txt["t_sfsv"].orEmpty().ifEmpty { "SF/SV" } to { a: ActividadComunicacion -> a.sfsv },
        txt["t_actividad"].orEmpty().ifEmpty { "Act." } to { a: ActividadComunicacion -> a.actividad },
        txt["t_asistentes"].orEmpty().ifEmpty { "Asist." } to { a: ActividadComunicacion -> a.asistentes },
        txt["t_encargado"].orEmpty().ifEmpty { "Enc." } to { a: ActividadComunicacion -> a.encargado },
        txt["t_observ"].orEmpty().ifEmpty { "Obs." } to { a: ActividadComunicacion ->
            listOf(a.cargo, a.observ).filter { it.isNotEmpty() }.joinToString(". ")
        },
        txt["t_nom_tipo"].orEmpty().ifEmpty { "Tipo" } to { a: ActividadComunicacion -> a.nomTipo },
    )
    val horizontalScroll = rememberScrollState()
    val cellWidth = 72.dp

    Column(modifier = Modifier.horizontalScroll(horizontalScroll)) {
        Row {
            columns.forEach { (header, _) ->
                TableCell(text = header, width = cellWidth, bold = true, header = true)
            }
        }
        sacd.actividades.forEach { act ->
            Row {
                columns.forEach { (_, valueFn) ->
                    TableCell(text = valueFn(act), width = cellWidth, bold = false, header = false)
                }
            }
        }
    }
    val propioLegend = txt["t_propio"].orEmpty()
    if (propioLegend.isNotEmpty()) {
        Text(
            "*) $propioLegend",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    bold: Boolean,
    header: Boolean,
) {
    Box(
        modifier = Modifier
            .width(width)
            .background(
                if (header) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .padding(4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontFamily = if (header) FontFamily.Default else FontFamily.Monospace,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
