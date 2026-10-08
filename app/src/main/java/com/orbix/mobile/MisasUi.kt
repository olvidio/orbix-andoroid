package com.orbix.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
                Row {
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
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(end = if (value.isNotEmpty()) 80.dp else 48.dp)
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

@Composable
fun MisasFilterSummaryBar(
    summary: String,
    onShowFilters: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onShowFilters) {
            Icon(
                imageVector = Icons.Filled.FilterList,
                contentDescription = "Mostrar filtros",
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
) {
    val horizontalScroll = rememberScrollState()
    val encargoWidth = 140.dp
    val cellWidth = if (editable) 64.dp else 56.dp

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        item {
            CuadriculaHeaderRow(
                horizontalScroll = horizontalScroll,
                encargoWidth = encargoWidth,
                cellWidth = cellWidth,
                dateColumns = grid.dateColumns,
            )
        }
        items(grid.rows, key = { row -> "${row.encargo}_${row.isTitle}_${row.cells.hashCode()}" }) { row ->
            val bg = if (row.isTitle) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScroll)
                    .background(bg),
            ) {
                Box(
                    modifier = Modifier
                        .width(encargoWidth)
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
                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .then(
                                if (canEdit) {
                                    Modifier
                                        .clickable { onCellClick!!(row, date, meta!!) }
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
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

@Composable
private fun CuadriculaHeaderRow(
    horizontalScroll: androidx.compose.foundation.ScrollState,
    encargoWidth: Dp,
    cellWidth: Dp,
    dateColumns: List<String>,
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
                    text = formatMisasDateHeader(date),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
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
    val parts = isoDate.split("-")
    if (parts.size != 3) return isoDate
    return "${parts[2]}.${parts[1]}"
}
