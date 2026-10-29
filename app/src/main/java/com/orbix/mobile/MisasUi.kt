package com.orbix.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisasDropdown(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.fillMaxWidth(),
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
                    onClick = {
                        onSelect(id)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}

/**
 * Campo de fecha local (`dd/MM/yyyy`) que abre un [DatePickerDialog] al pulsar.
 * El icono de borrar deja el valor vacío (útil p. ej. en ausencias).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalDateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text("dd/mm/aaaa") },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (value.isNotEmpty() && enabled) {
                        IconButton(
                            onClick = { onValueChange("") },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Borrar fecha",
                            )
                        }
                    }
                    IconButton(
                        onClick = { if (enabled) showPicker = true },
                        enabled = enabled,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = "Seleccionar fecha",
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(end = if (value.isNotEmpty()) 88.dp else 48.dp)
                .clickable(enabled = enabled) { showPicker = true },
        )
    }

    if (showPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = parseLocalDateToUtcMillis(value)
                ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            onValueChange(formatUtcMillisToLocalDate(millis))
                        }
                        showPicker = false
                    },
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancelar")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/** Parsea `d/m/yyyy`, `dd/mm/yyyy` o `yyyy-mm-dd` → medianoche UTC. */
fun parseLocalDateToUtcMillis(raw: String): Long? {
    val s = raw.trim().replace('-', '/')
    if (s.isEmpty()) return null
    val parts = s.split('/')
    if (parts.size != 3) return null
    val (day, month, year) = when {
        parts[0].length == 4 -> Triple(parts[2].toIntOrNull(), parts[1].toIntOrNull(), parts[0].toIntOrNull())
        else -> Triple(parts[0].toIntOrNull(), parts[1].toIntOrNull(), parts[2].toIntOrNull())
    }
    if (day == null || month == null || year == null) return null
    if (day !in 1..31 || month !in 1..12 || year < 1000) return null
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.clear()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month - 1)
    cal.set(Calendar.DAY_OF_MONTH, day)
    return cal.timeInMillis
}

fun formatUtcMillisToLocalDate(millis: Long): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.timeInMillis = millis
    val d = cal.get(Calendar.DAY_OF_MONTH)
    val m = cal.get(Calendar.MONTH) + 1
    val y = cal.get(Calendar.YEAR)
    return "%02d/%02d/%04d".format(d, m, y)
}

/** Parsea `H:mm` o `HH:mm` (opcional `:ss`). */
fun parseLocalTime(raw: String): Pair<Int, Int>? {
    val s = raw.trim()
    if (s.isEmpty()) return null
    val parts = s.split(':')
    if (parts.size < 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour to minute
}

fun formatLocalTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

fun normalizeLocalTimeDisplay(raw: String): String {
    val (h, m) = parseLocalTime(raw) ?: return raw.trim()
    return formatLocalTime(h, m)
}

fun addMinutesToLocalTime(time: String, minutesToAdd: Int): String {
    val (h, m) = parseLocalTime(time) ?: return ""
    val totalMinutes = h * 60 + m + minutesToAdd
    val wrapped = ((totalMinutes % (24 * 60)) + (24 * 60)) % (24 * 60)
    return formatLocalTime(wrapped / 60, wrapped % 60)
}

/**
 * Campo de hora (`HH:mm`) que abre un selector de hora al pulsar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalTimeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val displayValue = normalizeLocalTimeDisplay(value)

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text("HH:mm") },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (displayValue.isNotEmpty() && enabled) {
                        IconButton(
                            onClick = { onValueChange("") },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Borrar hora",
                            )
                        }
                    }
                    IconButton(
                        onClick = { if (enabled) showPicker = true },
                        enabled = enabled,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = "Seleccionar hora",
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(end = if (displayValue.isNotEmpty()) 88.dp else 48.dp)
                .clickable(enabled = enabled) { showPicker = true },
        )
    }

    if (showPicker) {
        val now = Calendar.getInstance()
        val (initialHour, initialMinute) = parseLocalTime(value)
            ?: (now.get(Calendar.HOUR_OF_DAY) to now.get(Calendar.MINUTE))
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange(
                            formatLocalTime(timePickerState.hour, timePickerState.minute),
                        )
                        showPicker = false
                    },
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancelar")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            },
        )
    }
}

/** Ancho máximo de los filtros desplegados: no hace falta ocupar toda la pantalla. */
private val FilterPanelMaxWidth = 440.dp

/**
 * Contenedor de filtros desplegados: centrado, con ancho acotado y con scroll propio
 * (en horizontal los filtros no caben y el botón de buscar quedaba inalcanzable).
 * Con [onInfo] muestra el botón ⓘ de la explicación de la pantalla.
 */
@Composable
fun MisasFilterPanel(
    modifier: Modifier = Modifier,
    onInfo: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Alto acotado (no `weight`): un hermano con fillMaxSize lo dejaría a 0.
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.6f).dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = FilterPanelMaxWidth).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (onInfo != null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    MisasLegendButton(onInfo)
                }
            }
            content()
        }
    }
}

@Composable
fun MisasFilterSummaryBar(
    summary: String,
    onShowFilters: () -> Unit,
    onShowLegend: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp),
            )
            .padding(start = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (onShowLegend != null) MisasLegendButton(onShowLegend)
        IconButton(onClick = onShowFilters) {
            Icon(
                imageVector = Icons.Filled.FilterList,
                contentDescription = "Mostrar filtros",
            )
        }
    }
}

@Composable
fun MisasLegendButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = "Leyenda de colores",
        )
    }
}

@Composable
fun MisasInfoDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        title = { Text(title) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
    )
}

private val cuadriculaLegendEstadoMisa = listOf(
    "rojoclaro" to "Propuesta",
    "amarilloclaro" to "Comunicado al sacerdote",
    "verdeclaro" to "Comunicado al CTR",
)

private val cuadriculaLegendSacd = listOf(
    "verdeclaro" to "Sin incidencias",
    "verde" to "Sin misa ese día y está en la zona",
    "azulclaro" to "Sin misa ese día y no está en la zona",
    "amarillo" to "Dos misas ese día / no es de la zona",
    "rojo" to "Más de dos misas, dos a primera hora o misa a primera hora fuera de la zona",
)

@Composable
fun CuadriculaLegendDialog(note: String? = null, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        title = { Text("Leyenda") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                note?.let { MisasReadOnlyNote(it) }
                CuadriculaLegendSection("Estado de la misa", cuadriculaLegendEstadoMisa)
                CuadriculaLegendSection("Disponibilidad del sacerdote", cuadriculaLegendSacd)
                Text(
                    "En la cuadrícula de sacerdotes: SI/NO indica si está en la zona; «--» que está fuera.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun CuadriculaLegendSection(title: String, items: List<Pair<String, String>>) {
    Text(title, style = MaterialTheme.typography.labelLarge)
    items.forEach { (css, label) ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(cuadriculaCellBackground(css) ?: Color.Transparent),
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
fun MisasLoadingBox() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun CuadriculaTable(
    grid: CuadriculaZona,
    editable: Boolean = false,
    onCellClick: ((row: CuadriculaRow, date: String, meta: CuadriculaCellMeta) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val horizontalScroll = rememberScrollState()
    val encargoWidth = 140.dp
    val cellWidth = if (editable) 64.dp else 56.dp

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        item(key = "cuadricula-header") {
            CuadriculaHeaderRow(
                horizontalScroll = horizontalScroll,
                encargoWidth = encargoWidth,
                cellWidth = cellWidth,
                dateColumns = grid.dateColumns,
                columnHeaders = grid.columnHeaders,
            )
        }
        itemsIndexed(grid.rows, key = { index, _ -> "cuadricula-row-$index" }) { _, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScroll),
            ) {
                val encargoBg = cuadriculaCellBackground(row.encargoColorClass)
                    ?: if (row.isTitle) {
                        cuadriculaCellBackground("titulo")
                            ?: MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                Box(
                    modifier = Modifier
                        .width(encargoWidth)
                        .background(encargoBg)
                        .padding(6.dp),
                ) {
                    Text(
                        text = row.encargo,
                        fontWeight = if (row.isTitle) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                grid.dateColumns.forEach { date ->
                    val meta = row.metaByDate[date]
                    val canEdit = editable && !row.isTitle && meta?.isEditable == true && onCellClick != null
                    val cellCss = meta?.color.orEmpty()
                    val cellBg = cuadriculaCellBackground(cellCss)
                        ?: if (canEdit) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        } else if (row.isTitle) {
                            cuadriculaCellBackground("titulo") ?: MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .background(cellBg)
                            .then(
                                if (canEdit) {
                                    Modifier.clickable { onCellClick!!(row, date, meta!!) }
                                } else {
                                    Modifier
                                },
                            )
                            .padding(6.dp),
                    ) {
                        Text(
                            text = row.cells[date].orEmpty().trim(),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** Colores de fondo de celdas como en la cuadrícula web (`modificar_cuadricula_zona.phtml`). */
fun cuadriculaCellBackground(cssClass: String): Color? {
    return when (cssClass.trim().lowercase()) {
        "verdeclaro" -> Color(0xFFD1E7DD)
        "verde" -> Color(0xFF00FF00)
        "rojoclaro" -> Color(0xFFF8D7DA)
        "rojo" -> Color(0xFFFF0000)
        "amarilloclaro" -> Color(0xFFFFFFAA)
        "amarillo" -> Color(0xFFFFFF00)
        "violetaclaro" -> Color(0xFFE8A0E8)
        "azulclaro2" -> Color(0xFF80E0E0)
        "azulclaro" -> Color(0xFFA0F0F0)
        "titulo" -> Color(0xFFF6F6F6)
        else -> null
    }
}

@Composable
private fun CuadriculaHeaderRow(
    horizontalScroll: androidx.compose.foundation.ScrollState,
    encargoWidth: Dp,
    cellWidth: Dp,
    dateColumns: List<String>,
    columnHeaders: Map<String, String>,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(horizontalScroll),
    ) {
        Box(
            modifier = Modifier
                .width(encargoWidth)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(6.dp),
        ) {
            Text("Encargo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
        }
        dateColumns.forEach { date ->
            Box(
                modifier = Modifier
                    .width(cellWidth)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(6.dp),
            ) {
                Text(
                    text = columnHeaders[date]?.takeIf { it.isNotEmpty() }
                        ?: formatMisasDateHeader(date),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun PlanCtrGridTable(
    columns: List<PlanCtrColumn>,
    rows: List<PlanCtrRow>,
    legend: List<PlanCtrLegendItem>,
) {
    val horizontalScroll = rememberScrollState()
    val encargoWidth = 140.dp
    val cellWidth = 56.dp

    Column(modifier = Modifier.fillMaxSize()) {
        if (legend.isNotEmpty()) {
            Text(
                "Leyenda",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            legend.forEach { item ->
                Text(
                    "${item.iniciales}: ${item.nombre}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScroll),
                ) {
                    Box(
                        modifier = Modifier
                            .width(encargoWidth)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(6.dp),
                    ) {
                        Text("Encargo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                    columns.forEach { col ->
                        Box(
                            modifier = Modifier
                                .width(cellWidth)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(6.dp),
                        ) {
                            Text(
                                "${col.letra} ${col.numDia}.${col.numMes}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            items(rows, key = { it.descEnc + it.cells.hashCode() }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScroll),
                ) {
                    Box(
                        modifier = Modifier
                            .width(encargoWidth)
                            .padding(6.dp),
                    ) {
                        Text(row.descEnc, style = MaterialTheme.typography.bodySmall)
                    }
                    row.cells.forEach { cell ->
                        Box(
                            modifier = Modifier
                                .width(cellWidth)
                                .padding(6.dp),
                        ) {
                            Text(
                                cell.trim(),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 2,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleRowsTable(
    columns: List<String>,
    rows: List<Map<String, String>>,
    columnKeys: List<String>,
) {
    val horizontalScroll = rememberScrollState()
    val width = 120.dp

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScroll),
            ) {
                columns.forEach { header ->
                    Box(
                        modifier = Modifier
                            .width(width)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(6.dp),
                    ) {
                        Text(header, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        items(rows.size) { index ->
            val row = rows[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScroll),
            ) {
                columnKeys.forEach { key ->
                    Box(
                        modifier = Modifier
                            .width(width)
                            .padding(6.dp),
                    ) {
                        Text(
                            row[key].orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SacdPlanList(rows: List<PlanSacdRow>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(rows, key = { "${it.dia}_${it.encargo}_${it.observ}" }) { row ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(10.dp),
            ) {
                Text(row.dia, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                if (row.encargo.isNotEmpty()) {
                    Text(row.encargo, style = MaterialTheme.typography.bodySmall)
                }
                if (row.observ.isNotEmpty()) {
                    Text(
                        row.observ,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun MisasReadOnlyNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

fun formatMisasDateHeader(isoDate: String): String {
    return runCatching {
        val date = LocalDate.parse(isoDate)
        val dow = listOf("L", "M", "X", "J", "V", "S", "D")[date.dayOfWeek.value - 1]
        val day = date.dayOfMonth
        val month = date.monthValue.toString().padStart(2, '0')
        "$dow $day.$month"
    }.getOrElse {
        val parts = isoDate.split("-")
        if (parts.size != 3) isoDate else "${parts[2]}.${parts[1]}"
    }
}
