package com.orbix.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
fun CuadriculaTable(grid: CuadriculaZona) {
    val horizontalScroll = rememberScrollState()
    val encargoWidth = 140.dp
    val cellWidth = 56.dp

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
                    Box(
                        modifier = Modifier
                            .width(cellWidth)
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
