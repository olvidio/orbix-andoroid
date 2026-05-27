package com.orbix.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import okhttp3.OkHttpClient

private data class PlanMisasEntry(
    val label: String,
    val controller: String,
)

private val PLAN_MISAS_ENTRIES = listOf(
    PlanMisasEntry("Modificar plantilla", "modificar_plantilla.php"),
    PlanMisasEntry("Preparar nuevo plan de misas y encargos", "preparar_plan_de_misas.php"),
    PlanMisasEntry("Modificar el plan de misas y encargos", "modificar_plan_de_misas.php"),
    PlanMisasEntry("Ver el plan de misas y encargos de una zona", "ver_plan_de_misas.php"),
    PlanMisasEntry("Modificar estado del plan de misas", "cambiar_status.php"),
    PlanMisasEntry("Ver el plan de un sacerdote", "buscar_plan_sacd.php"),
    PlanMisasEntry("Ver el plan de misas y encargos de un centro", "buscar_plan_ctr.php"),
    PlanMisasEntry("Crear y modificar los encargos", "modificar_encargos.php"),
    PlanMisasEntry("Modificar los encargos visibles para un centro", "modificar_encargos_centros.php"),
    PlanMisasEntry("Modificar la tabla de iniciales de los sacerdotes", "modificar_iniciales_sacd_zona.php"),
)

/** Índice del módulo misas (`misas_index.php`), con acceso a las pantallas del plan. */
@Composable
fun PlanMisasScreen(
    client: OkHttpClient,
    baseUrl: String,
    contentPadding: PaddingValues,
) {
    var subScreen by remember { mutableStateOf<NativeMenuScreen?>(null) }

    BackHandler(enabled = subScreen != null) {
        subScreen = null
    }

    val screen = subScreen
    if (screen != null) {
        MisasNativeScreen(
            screen = screen,
            client = client,
            baseUrl = baseUrl,
            contentPadding = contentPadding,
        )
    } else {
        PlanMisasIndex(
            contentPadding = contentPadding,
            onSelectEntry = { entry ->
                subScreen = nativeScreenForController(entry.controller)
            },
        )
    }
}

@Composable
private fun PlanMisasIndex(
    contentPadding: PaddingValues,
    onSelectEntry: (PlanMisasEntry) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Text(
                text = "Plan de misas",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
            Text(
                text = "Elige una opción del módulo de misas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .padding(bottom = 8.dp),
            )
        }
        items(PLAN_MISAS_ENTRIES, key = { it.controller }) { entry ->
            val native = nativeScreenForController(entry.controller)
            val isNative = native != NativeMenuScreen.Pending
            Card(
                onClick = { onSelectEntry(entry) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = entry.label, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = if (isNative) "Pantalla nativa" else "Pendiente de implementar",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isNative) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
