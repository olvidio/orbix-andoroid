package com.orbix.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var statusMsg by remember { mutableStateOf<String?>(null) }

    var jefeSacds by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var sacdOpciones by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    var selectedSacdKey by remember { mutableStateOf<String?>(null) }
    var selectedFiltro by remember { mutableStateOf("n") }
    var historial by remember { mutableStateOf(false) }
    var drafts by remember { mutableStateOf<List<SacdAusenciaDraft>>(emptyList()) }
    var tiposDisponibles by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    var filtroExpanded by remember { mutableStateOf(false) }
    var sacdExpanded by remember { mutableStateOf(false) }
    var addTipoExpanded by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(true) }
    var resultsLoaded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val filtroLabels = filtroSacdLabels()

    fun currentIdNom(): String? = when (mode) {
        AusenciasMode.JefeZona -> selectedSacdKey?.let { sacdKeyToIdNom(it) }
        AusenciasMode.SacdLista -> selectedSacdKey
    }

    fun reloadAusencias() {
        val idNom = currentIdNom() ?: return
        scope.launch {
            loadingRows = true
            error = null
            statusMsg = null
            try {
                val data = withContext(Dispatchers.IO) {
                    fetchSacdAusencias(client, baseUrl, idNom, historial)
                }
                if (data == null) {
                    error = "No se pudo cargar (sacd_ausencias_get_data)."
                    drafts = emptyList()
                    tiposDisponibles = emptyMap()
                } else {
                    tiposDisponibles = data.tiposDisponibles
                    drafts = data.rows.mapIndexed { i, row -> row.toDraft(i) }
                    resultsLoaded = true
                    showFilters = false
                    if (data.rows.isEmpty()) {
                        error = if (historial) {
                            "Sin ausencias registradas. Puedes añadir abajo."
                        } else {
                            "Sin ausencias vigentes. Prueba «Ver anteriores» o añade una."
                        }
                    }
                }
            } finally {
                loadingRows = false
            }
        }
    }

    fun saveAusencias() {
        val idNom = currentIdNom() ?: return
        scope.launch {
            saving = true
            error = null
            statusMsg = null
            try {
                val result = withContext(Dispatchers.IO) {
                    updateSacdAusencias(client, baseUrl, idNom, drafts)
                }
                if (result.ok) {
                    statusMsg = "Ausencias guardadas."
                    reloadAusencias()
                } else {
                    error = result.message
                }
            } finally {
                saving = false
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
        drafts = emptyList()
        historial = false
        showFilters = true
        resultsLoaded = false
        selectedSacdKey = null
        statusMsg = null
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
            text = "Edita fechas y guarda. Deja inicio y fin en blanco para borrar una fila.",
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
                    drafts = emptyList()
                    error = null
                    statusMsg = null
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
        statusMsg?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }

        if (resultsLoaded && !showFilters) {
            TextButton(
                onClick = {
                    historial = !historial
                    reloadAusencias()
                },
                enabled = selectedSacdKey != null && !loadingRows && !saving,
            ) {
                Text(if (historial) "Solo vigentes" else "Ver anteriores")
            }

            if (tiposDisponibles.isNotEmpty()) {
                MisasDropdown(
                    label = "Añadir ausencia / tarea",
                    value = "",
                    expanded = addTipoExpanded,
                    onExpandedChange = { addTipoExpanded = it },
                    options = tiposDisponibles.map { (id, label) -> id to label },
                    onSelect = { idEnc ->
                        val desc = tiposDisponibles[idEnc].orEmpty()
                        drafts = drafts + SacdAusenciaDraft(
                            localKey = "new-${System.currentTimeMillis()}-$idEnc",
                            idEnc = idEnc.toIntOrNull() ?: 0,
                            descEnc = desc,
                            idItem = 0,
                            inicio = "",
                            fin = "",
                        )
                        error = null
                    },
                )
            }

            Button(
                onClick = { saveAusencias() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving && !loadingRows && drafts.isNotEmpty(),
            ) {
                Text(if (saving) "Guardando…" else "Guardar")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(drafts, key = { it.localKey }) { draft ->
                AusenciaEditCard(
                    draft = draft,
                    enabled = !saving && !loadingRows,
                    onChange = { updated ->
                        drafts = drafts.map { if (it.localKey == updated.localKey) updated else it }
                    },
                )
            }
        }
    }
}

@Composable
private fun AusenciaEditCard(
    draft: SacdAusenciaDraft,
    enabled: Boolean,
    onChange: (SacdAusenciaDraft) -> Unit,
) {
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
            Text(draft.descEnc, style = MaterialTheme.typography.bodyLarge)
            if (draft.idItem == 0) {
                Text(
                    "Nueva (aún no guardada)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LocalDateField(
                    label = "Inicio",
                    value = draft.inicio,
                    onValueChange = { onChange(draft.copy(inicio = it)) },
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                )
                LocalDateField(
                    label = "Fin",
                    value = draft.fin,
                    onValueChange = { onChange(draft.copy(fin = it)) },
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                )
            }
            val horario = buildString {
                if (draft.dedicM.isNotEmpty()) append("M: ${draft.dedicM} ")
                if (draft.dedicT.isNotEmpty()) append("T: ${draft.dedicT} ")
                if (draft.dedicV.isNotEmpty()) append("V: ${draft.dedicV}")
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
