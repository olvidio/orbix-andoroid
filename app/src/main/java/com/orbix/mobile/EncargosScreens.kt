package com.orbix.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

@Composable
fun EncargosZonaScreen(
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
    var grid by remember { mutableStateOf<EncargosZonaGrid?>(null) }

    var selectedZona by remember { mutableStateOf<String?>(null) }
    var selectedOrden by remember { mutableStateOf("desc_enc") }
    var zonaExpanded by remember { mutableStateOf(false) }
    var ordenExpanded by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(true) }

    var editing by remember { mutableStateOf(false) }
    var isNew by remember { mutableStateOf(false) }
    var editIdEnc by remember { mutableStateOf("") }
    var editEncargo by remember { mutableStateOf("") }
    var editTipo by remember { mutableStateOf("") }
    var editUbi by remember { mutableStateOf("") }
    var editOrden by remember { mutableStateOf("") }
    var editPrioridad by remember { mutableStateOf("") }
    var editDescLugar by remember { mutableStateOf("") }
    var editIdioma by remember { mutableStateOf("") }
    var editObserv by remember { mutableStateOf("") }
    var tipoExpanded by remember { mutableStateOf(false) }
    var ubiExpanded by remember { mutableStateOf(false) }
    var idiomaExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun buscar() {
        val zona = selectedZona ?: return
        scope.launch {
            loadingGrid = true
            error = null
            statusMsg = null
            try {
                val result = withContext(Dispatchers.IO) {
                    fetchVerEncargosZona(client, baseUrl, idZona = zona, orden = selectedOrden)
                }
                if (result == null) {
                    error = "No se pudo cargar los encargos."
                    grid = null
                } else {
                    grid = result
                    showFilters = false
                    if (result.rows.isEmpty()) error = "Sin encargos. Puedes crear uno nuevo."
                }
            } finally {
                loadingGrid = false
            }
        }
    }

    fun openNew() {
        isNew = true
        editIdEnc = ""
        editEncargo = ""
        editTipo = grid?.tiposEncargo?.keys?.firstOrNull().orEmpty()
        editUbi = ""
        editOrden = ""
        editPrioridad = ""
        editDescLugar = ""
        editIdioma = ""
        editObserv = ""
        editing = true
        error = null
        statusMsg = null
    }

    fun openEdit(row: Map<String, String>) {
        isNew = false
        editIdEnc = row["id_enc"].orEmpty()
        editEncargo = row["encargo"].orEmpty()
        editTipo = row["id_tipo_enc"].orEmpty()
        editUbi = row["id_ubi"].orEmpty()
        editOrden = row["orden"].orEmpty()
        editPrioridad = row["prioridad"].orEmpty()
        editDescLugar = row["descripcion_lugar"].orEmpty()
        editIdioma = row["idioma_enc"].orEmpty()
        editObserv = row["observ"].orEmpty()
        editing = true
        error = null
        statusMsg = null
    }

    fun guardar() {
        val zona = selectedZona ?: return
        scope.launch {
            saving = true
            error = null
            statusMsg = null
            try {
                val result = withContext(Dispatchers.IO) {
                    postGuardarEncargoZona(
                        client = client,
                        baseUrl = baseUrl,
                        idZona = zona,
                        idEnc = editIdEnc,
                        idTipoEnc = editTipo,
                        idUbi = editUbi,
                        encargo = editEncargo,
                        orden = editOrden,
                        prioridad = editPrioridad,
                        descripcionLugar = editDescLugar,
                        idiomaEnc = editIdioma,
                        observ = editObserv,
                    )
                }
                if (result.ok) {
                    statusMsg = if (isNew) "Encargo creado." else "Encargo guardado."
                    editing = false
                    buscar()
                } else {
                    error = result.message
                }
            } finally {
                saving = false
            }
        }
    }

    fun eliminar() {
        if (editIdEnc.isEmpty()) {
            editing = false
            return
        }
        scope.launch {
            saving = true
            error = null
            statusMsg = null
            try {
                val result = withContext(Dispatchers.IO) {
                    postEliminarEncargoZona(client, baseUrl, editIdEnc)
                }
                if (result.ok) {
                    statusMsg = "Encargo eliminado."
                    editing = false
                    buscar()
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
        grid = null
        showFilters = true
        editing = false
        try {
            val data = withContext(Dispatchers.IO) {
                fetchModificarEncargosPage(client, baseUrl)
            }
            if (data == null) {
                error = "No se pudo cargar la pantalla."
            } else if (data.error != null) {
                error = data.error
                page = data
            } else if (data.zonasOpciones.isEmpty()) {
                error = "No hay zonas disponibles."
                page = data
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

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Encargos de la zona", style = MaterialTheme.typography.titleMedium)
        Text(
            "Toca una fila para editar. Nuevo crea un encargo.",
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
        if (!showFilters && grid != null) {
            MisasFilterSummaryBar(
                summary = buildString {
                    append(zonas[selectedZona].orEmpty())
                    append(" · ")
                    append(page?.ordenOpciones?.get(selectedOrden).orEmpty())
                },
                onShowFilters = {
                    showFilters = true
                    grid = null
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
            if (page?.ordenOpciones?.isNotEmpty() == true) {
                MisasDropdown(
                    label = "Orden",
                    value = page!!.ordenOpciones[selectedOrden].orEmpty(),
                    expanded = ordenExpanded,
                    onExpandedChange = { ordenExpanded = it },
                    options = page!!.ordenOpciones.map { (id, label) -> id to label },
                    onSelect = { selectedOrden = it },
                )
            }
            Button(
                onClick = { buscar() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loadingGrid,
            ) {
                Text("Ver listado")
            }
        }
        if (!showFilters && grid != null) {
            OutlinedButton(
                onClick = { openNew() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving && !loadingGrid,
            ) {
                Text("Nuevo encargo")
            }
        }
        if (loadingGrid) MisasLoadingBox()
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        statusMsg?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
        val rows = grid?.rows.orEmpty()
        if (rows.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(rows, key = { it["id_enc"].orEmpty() + it["encargo"].orEmpty() }) { row ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openEdit(row) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(row["encargo"].orEmpty(), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                listOfNotNull(
                                    row["tipo_encargo"]?.takeIf { it.isNotEmpty() },
                                    row["lugar"]?.takeIf { it.isNotEmpty() },
                                    row["orden"]?.takeIf { it.isNotEmpty() }?.let { "orden $it" },
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (editing) {
        val tipos = grid?.tiposEncargo.orEmpty()
        val centros = grid?.centros.orEmpty()
        val idiomas = grid?.idiomas.orEmpty()
        AlertDialog(
            onDismissRequest = { if (!saving) editing = false },
            title = { Text(if (isNew) "Nuevo encargo" else "Editar encargo") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = editEncargo,
                        onValueChange = { editEncargo = it },
                        label = { Text("Encargo") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !saving,
                    )
                    if (tipos.isNotEmpty()) {
                        MisasDropdown(
                            label = "Tipo",
                            value = tipos[editTipo].orEmpty(),
                            expanded = tipoExpanded,
                            onExpandedChange = { tipoExpanded = it },
                            options = tipos.map { (id, label) -> id to label },
                            onSelect = { editTipo = it },
                        )
                    }
                    if (centros.isNotEmpty()) {
                        MisasDropdown(
                            label = "Lugar / centro",
                            value = centros[editUbi].orEmpty(),
                            expanded = ubiExpanded,
                            onExpandedChange = { ubiExpanded = it },
                            options = listOf("" to "(ninguno)") + centros.map { (id, label) -> id to label },
                            onSelect = { editUbi = it },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editOrden,
                            onValueChange = { editOrden = it },
                            label = { Text("Orden") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !saving,
                        )
                        OutlinedTextField(
                            value = editPrioridad,
                            onValueChange = { editPrioridad = it },
                            label = { Text("Prioridad") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !saving,
                        )
                    }
                    OutlinedTextField(
                        value = editDescLugar,
                        onValueChange = { editDescLugar = it },
                        label = { Text("Descripción lugar") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !saving,
                    )
                    if (idiomas.isNotEmpty()) {
                        MisasDropdown(
                            label = "Idioma",
                            value = idiomas[editIdioma].orEmpty(),
                            expanded = idiomaExpanded,
                            onExpandedChange = { idiomaExpanded = it },
                            options = listOf("" to "(ninguno)") + idiomas.map { (id, label) -> id to label },
                            onSelect = { editIdioma = it },
                        )
                    }
                    OutlinedTextField(
                        value = editObserv,
                        onValueChange = { editObserv = it },
                        label = { Text("Observaciones") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !saving,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { guardar() }, enabled = !saving && editEncargo.isNotBlank()) {
                    Text(if (saving) "Guardando…" else "Guardar")
                }
            },
            dismissButton = {
                Row {
                    if (!isNew) {
                        TextButton(
                            onClick = { eliminar() },
                            enabled = !saving,
                        ) {
                            Text("Eliminar", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { editing = false }, enabled = !saving) {
                        Text("Cancelar")
                    }
                }
            },
        )
    }
}

@Composable
fun EncargosCentrosScreen(
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
    var grid by remember { mutableStateOf<EncargosCentrosGrid?>(null) }
    var encargoOpciones by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    var selectedZona by remember { mutableStateOf<String?>(null) }
    var zonaExpanded by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(true) }

    var editing by remember { mutableStateOf(false) }
    var isNew by remember { mutableStateOf(false) }
    var editIdItem by remember { mutableStateOf("") }
    var editIdEnc by remember { mutableStateOf("") }
    var editIdCtr by remember { mutableStateOf("") }
    var encExpanded by remember { mutableStateOf(false) }
    var ctrExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun buscar() {
        val zona = selectedZona ?: return
        scope.launch {
            loadingGrid = true
            error = null
            statusMsg = null
            try {
                val result = withContext(Dispatchers.IO) {
                    fetchVerEncargosCentros(client, baseUrl, idZona = zona)
                }
                if (result == null) {
                    error = "No se pudo cargar los datos."
                    grid = null
                } else {
                    grid = result
                    showFilters = false
                    if (result.rows.isEmpty()) {
                        error = "Sin vínculos. Puedes crear uno nuevo."
                    }
                }
            } finally {
                loadingGrid = false
            }
        }
    }

    fun loadEncargosForDialog(idEncSel: String = "") {
        val zona = selectedZona ?: return
        scope.launch {
            val opts = withContext(Dispatchers.IO) {
                fetchDesplegableEncargos(client, baseUrl, idZona = zona, idEnc = idEncSel)
            }
            encargoOpciones = opts?.opciones.orEmpty()
            if (idEncSel.isNotEmpty() && editIdEnc.isEmpty()) {
                editIdEnc = opts?.selected?.takeIf { it.isNotEmpty() } ?: idEncSel
            }
        }
    }

    fun openNew() {
        isNew = true
        editIdItem = ""
        editIdEnc = ""
        editIdCtr = ""
        editing = true
        error = null
        statusMsg = null
        loadEncargosForDialog()
    }

    fun openEdit(row: Map<String, String>) {
        isNew = false
        editIdItem = row["id_item"].orEmpty()
        editIdEnc = row["id_encargo"].orEmpty()
        editIdCtr = row["id_centro"].orEmpty()
        editing = true
        error = null
        statusMsg = null
        loadEncargosForDialog(editIdEnc)
    }

    fun guardar() {
        if (editIdEnc.isEmpty() || editIdCtr.isEmpty()) {
            error = "Falta encargo o centro."
            return
        }
        scope.launch {
            saving = true
            error = null
            statusMsg = null
            try {
                val result = withContext(Dispatchers.IO) {
                    postGuardarEncargoCentro(
                        client,
                        baseUrl,
                        idItem = editIdItem,
                        idEnc = editIdEnc,
                        idCtr = editIdCtr,
                    )
                }
                if (result.ok) {
                    statusMsg = if (isNew) "Vínculo creado." else "Vínculo guardado."
                    editing = false
                    buscar()
                } else {
                    error = result.message
                }
            } finally {
                saving = false
            }
        }
    }

    fun eliminar() {
        if (editIdItem.isEmpty()) {
            editing = false
            return
        }
        scope.launch {
            saving = true
            error = null
            statusMsg = null
            try {
                val result = withContext(Dispatchers.IO) {
                    postEliminarEncargoCentro(client, baseUrl, editIdItem)
                }
                if (result.ok) {
                    statusMsg = "Vínculo eliminado."
                    editing = false
                    buscar()
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
        grid = null
        showFilters = true
        editing = false
        try {
            val data = withContext(Dispatchers.IO) {
                fetchModificarEncargosCentrosPage(client, baseUrl)
            }
            if (data == null) {
                error = "No se pudo cargar la pantalla."
            } else if (data.error != null) {
                error = data.error
                page = data
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
        Text("Encargos visibles por centro", style = MaterialTheme.typography.titleMedium)
        Text(
            "Toca una fila para editar o eliminar el vínculo encargo↔centro.",
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
        if (!showFilters && grid != null) {
            MisasFilterSummaryBar(
                summary = zonas[selectedZona].orEmpty(),
                onShowFilters = {
                    showFilters = true
                    grid = null
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
        if (!showFilters && grid != null) {
            OutlinedButton(
                onClick = { openNew() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving && !loadingGrid,
            ) {
                Text("Nuevo vínculo")
            }
        }
        if (loadingGrid) MisasLoadingBox()
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        statusMsg?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
        val rows = grid?.rows.orEmpty()
        if (rows.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(rows, key = { it["id_item"].orEmpty() }) { row ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openEdit(row) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(row["encargo"].orEmpty(), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                row["centro"].orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (editing) {
        val centros = grid?.centrosZona.orEmpty()
        AlertDialog(
            onDismissRequest = { if (!saving) editing = false },
            title = { Text(if (isNew) "Nuevo vínculo" else "Editar vínculo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MisasDropdown(
                        label = "Encargo",
                        value = encargoOpciones[editIdEnc].orEmpty(),
                        expanded = encExpanded,
                        onExpandedChange = { encExpanded = it },
                        options = encargoOpciones.map { (id, label) -> id to label },
                        onSelect = { editIdEnc = it },
                    )
                    MisasDropdown(
                        label = "Centro",
                        value = centros[editIdCtr].orEmpty(),
                        expanded = ctrExpanded,
                        onExpandedChange = { ctrExpanded = it },
                        options = centros.map { (id, label) -> id to label },
                        onSelect = { editIdCtr = it },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { guardar() },
                    enabled = !saving && editIdEnc.isNotEmpty() && editIdCtr.isNotEmpty(),
                ) {
                    Text(if (saving) "Guardando…" else "Guardar")
                }
            },
            dismissButton = {
                Row {
                    if (!isNew) {
                        TextButton(onClick = { eliminar() }, enabled = !saving) {
                            Text("Eliminar", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { editing = false }, enabled = !saving) {
                        Text("Cancelar")
                    }
                }
            },
        )
    }
}
