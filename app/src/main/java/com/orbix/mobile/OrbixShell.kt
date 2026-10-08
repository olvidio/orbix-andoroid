package com.orbix.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import kotlinx.coroutines.launch

/**
 * Barra superior (título + hamburguesa + ajustes) y cajón lateral estilo web.
 *
 * Con un solo grupmenu, el cajón lista directamente sus entradas (sin paso intermedio).
 * Con varios grupos, el cajón lista los apartados principales como hasta ahora.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrbixShell(
    title: String,
    menuGroups: List<GrupMenuItem>,
    menusLoading: Boolean = false,
    selectedGroupId: String?,
    selectedMenuId: String?,
    onSelectHome: () -> Unit,
    onSelectGroup: (GrupMenuItem) -> Unit,
    onSelectMenu: (OrbixMenuItem) -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val singleGroup = menuGroups.size == 1

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.widthIn(max = 320.dp),
            ) {
                val drawerScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(drawerScroll),
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text("Inicio") },
                        selected = selectedMenuId == null &&
                            (singleGroup || selectedGroupId == null),
                        onClick = {
                            scope.launch { drawerState.close() }
                            onSelectHome()
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    if (menusLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (menuGroups.isEmpty()) {
                        Text(
                            text = "No hay menús disponibles. Revisa la sesión e inténtalo de nuevo.",
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (singleGroup) {
                        val group = menuGroups.first()
                        Text(
                            text = group.label,
                            modifier = Modifier.padding(
                                horizontal = 24.dp,
                                vertical = 4.dp,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        DrawerMenuRows(
                            rows = buildMenuDisplayRows(group.menus),
                            selectedMenuId = selectedMenuId,
                            onSelectMenu = { menu ->
                                scope.launch { drawerState.close() }
                                onSelectMenu(menu)
                            },
                        )
                    } else {
                        Text(
                            text = "Apartados",
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        menuGroups.forEach { item ->
                            NavigationDrawerItem(
                                label = { Text(item.label) },
                                selected = selectedGroupId == item.id,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    onSelectGroup(item)
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Logout, contentDescription = null) },
                        label = { Text("Cerrar sesión") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onLogout()
                        },
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Menú",
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Configuración",
                            )
                        }
                    },
                )
            },
            content = content,
        )
    }
}

@Composable
private fun DrawerMenuRows(
    rows: List<MenuDisplayRow>,
    selectedMenuId: String?,
    onSelectMenu: (OrbixMenuItem) -> Unit,
) {
    rows.forEach { row ->
        when (row) {
            is MenuDisplayRow.Section -> {
                Text(
                    text = row.title,
                    modifier = Modifier.padding(
                        start = (24 + row.depth * 16).dp,
                        end = 24.dp,
                        top = 10.dp,
                        bottom = 2.dp,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            is MenuDisplayRow.Item -> {
                NavigationDrawerItem(
                    label = { Text(row.menu.label) },
                    selected = selectedMenuId == row.menu.id,
                    modifier = Modifier.padding(start = (row.depth * 16).dp),
                    onClick = { onSelectMenu(row.menu) },
                )
            }
        }
    }
}
