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
import androidx.compose.material3.TextButton
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
fun AusenciasScreen(
    client: OkHttpClient,
    baseUrl: String,
    contentPadding: PaddingValues,
    mode: AusenciasMode,
) {
    var loadingPage by remember { mutableStateOf(true) }
    var loadingRows by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var jefeSacds by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var sacdOpciones by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    var selectedSacdKey by remember { mutableStateOf<String?>(null) }
    var selectedFiltro by remember { mutableStateOf("n") }
    var historial by remember { mutableStateOf(false) }
    var rows by remember { mutableStateOf<List<SacdAusenciaRow>>(emptyList()) }

    var filtroExpanded by remember { mutableStateOf(false) }
    var sacdExpanded by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(true) }
    var resultsLoaded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val filtroLabels = filtroSacdLabels()

    fun reloadAusencias() {
        val idNom = when (mode) {
            AusenciasMode.JefeZona -> selectedSacdKey?.let { sacdKeyToIdNom(it) }
            AusenciasMode.SacdLista -> selectedSacdKey
        } ?: return
        scope.launch {
            loadingRows = true
            error = null
            try {
                val data = withContext(Dispatchers.IO) {
                    fetchSacdAusencias(client, baseUrl, idNom, historial)
                }
                if (data == null) {
                    error = "No se pudo cargar (sacd_ausencias_get_data)."
                    rows = emptyList()
                } else {
                    rows = data.rows
                    resultsLoaded = true
                    showFilters = false
                    if (data.rows.isEmpty()) {
                        error = if (historial) {
                            "Sin ausencias registradas."
                        } else {
                            "Sin ausencias vigentes. Prueba «Ver anteriores»."
                        }
                    }
                }
            } finally {
                loadingRows = false
            }
        }
    }

    fun reloadSacdList(onDone: (() -> Unit)? = null) {
        scope.launch {
            loadingPage = true
            error = null
            try {
                when (mode) {
                    AusenciasMode.JefeZona -> {
                        val page = withContext(Dispatchers.IO) {
                            fetchSacdAusenciasJefeZonaPage(client, baseUrl)
                        }
                        if (page == null) {
                            error = "No se pudo cargar (sacd_ausencias_jefe_zona_data)."
                            jefeSacds = emptyMap()
                        } else {
                            jefeSacds = page.sacdOpciones
                            if (selectedSacdKey == null || !jefeSacds.containsKey(selectedSacdKey)) {
                                selectedSacdKey = jefeSacds.keys.firstOrNull()
                            }
                        }
                    }
                    AusenciasMode.SacdLista -> {
                        val page = withContext(Dispatchers.IO) {
                            fetchSacdSelectPage(
                                client,
                                baseUrl,
                                filtroSacd = selectedFiltro,
                                idNom = selectedSacdKey.orEmpty().ifEmpty { "0" },
                            )
                        }
                        if (page == null) {
                            error = "No se pudo cargar (sacd_select_data)."
                            sacdOpciones = emptyMap()
                        } else {
                            sacdOpciones = page.opciones
                            if (selectedSacdKey == null || !sacdOpciones.containsKey(selectedSacdKey)) {
                                selectedSacdKey = page.selected.takeIf { it.isNotEmpty() && it != "0" }
                                    ?: sacdOpciones.keys.firstOrNull()
                            }
                        }
                    }
                }
            } finally {
                loadingPage = false
                onDone?.invoke()
            }
        }
    }

    LaunchedEffect(baseUrl, mode) {
        rows = emptyList()
        historial = false
        showFilters = true
        resultsLoaded = false
        selectedSacdKey = null
        reloadSacdList()
    }

    LaunchedEffect(loadingPage, mode, selectedSacdKey) {
        if (
            !loadingPage &&
            mode == AusenciasMode.JefeZona &&
            selectedSacdKey != null &&
            !resultsLoaded
        ) {
            reloadAusencias()
        }
    }

    val sacdOptions = when (mode) {
        AusenciasMode.JefeZona -> jefeSacds
        AusenciasMode.SacdLista -> sacdOpciones
    }

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (mode == AusenciasMode.JefeZona) "Ausencias" else "Ausencias SACD",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Vista de consulta. Editar fechas u horarios solo en la web.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (loadingPage) {
            MisasLoadingBox()
            return@Column
        }

        if (sacdOptions.isEmpty()) {
            Text(
                text = error ?: "No hay sacerdotes disponibles.",
                color = MaterialTheme.colorScheme.error,
            )
            return@Column
        }

        val filterSummary = buildString {
            if (mode == AusenciasMode.SacdLista) {
                append(filtroLabels[selectedFiltro].orEmpty())
                append(" · ")
            }
            append(sacdOptions[selectedSacdKey].orEmpty())
            if (historial) append(" · historial")
        }

        if (!showFilters && resultsLoaded) {
            MisasFilterSummaryBar(
                summary = filterSummary,
                onShowFilters = {
                    showFilters = true
                    rows = emptyList()
                    error = null
                },
            )
        }

        if (showFilters) {
            if (mode == AusenciasMode.SacdLista) {
                MisasDropdown(
                    label = "Tipo SACD",
                    value = filtroLabels[selectedFiltro].orEmpty(),
                    expanded = filtroExpanded,
                    onExpandedChange = { filtroExpanded = it },
                    options = filtroLabels.map { (id, label) -> id to label },
                    onSelect = {
                        selectedFiltro = it
                        reloadSacdList()
                    },
                )
            }
            MisasDropdown(
                label = "Sacerdote",
                value = sacdOptions[selectedSacdKey].orEmpty(),
                expanded = sacdExpanded,
                onExpandedChange = { sacdExpanded = it },
                options = sacdOptions.map { (id, label) -> id to label },
                onSelect = { selectedSacdKey = it },
            )
            Button(
                onClick = { reloadAusencias() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loadingRows && selectedSacdKey != null,
            ) {
                Text("Ver ausencias")
            }
        }

        if (loadingRows) MisasLoadingBox()

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (resultsLoaded && !showFilters) {
            TextButton(
                onClick = {
                    historial = !historial
                    reloadAusencias()
                },
                enabled = selectedSacdKey != null && !loadingRows,
            ) {
                Text(if (historial) "Solo vigentes" else "Ver anteriores")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(rows, key = { "${it.idItem}-${it.idEnc}" }) { row ->
                AusenciaRowCard(row)
            }
        }
    }
}

@Composable
private fun AusenciaRowCard(row: SacdAusenciaRow) {
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
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(row.descEnc, style = MaterialTheme.typography.bodyLarge)
            val fechas = listOfNotNull(
                row.inicio?.let { "Inicio: $it" },
                row.fin?.let { "Fin: $it" },
            ).joinToString(" · ")
            if (fechas.isNotEmpty()) {
                Text(fechas, style = MaterialTheme.typography.bodyMedium)
            }
            val horario = buildString {
                if (row.dedicM.isNotEmpty()) append("M: ${row.dedicM} ")
                if (row.dedicT.isNotEmpty()) append("T: ${row.dedicT} ")
                if (row.dedicV.isNotEmpty()) append("V: ${row.dedicV}")
            }.trim()
            if (horario.isNotEmpty()) {
                Text(
                    horario,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
