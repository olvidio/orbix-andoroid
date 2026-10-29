package com.orbix.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.font.FontWeight
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
                modifier = Modifier
                    .padding(8.dp)
                    .widthIn(max = 320.dp),
                drawerShape = RoundedCornerShape(28.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                val drawerScroll = rememberScrollState()
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Aquinate", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { scope.launch { drawerState.close() } }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cerrar menú")
                        }
                    }
                    Column(modifier = Modifier.weight(1f).verticalScroll(drawerScroll)) {
                        NavigationDrawerItem(
                            modifier = Modifier.height(DrawerItemHeight),
                            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                            label = { Text("Inicio") },
                            selected = selectedMenuId == null &&
                                (singleGroup || selectedGroupId == null),
                            onClick = {
                                scope.launch { drawerState.close() }
                                onSelectHome()
                            },
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

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
                                    modifier = Modifier.height(DrawerItemHeight),
                                    label = { Text(item.label) },
                                    selected = selectedGroupId == item.id,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        onSelectGroup(item)
                                    },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        NavigationDrawerItem(
                            modifier = Modifier.height(DrawerItemHeight),
                            icon = { Icon(Icons.Filled.Logout, contentDescription = null) },
                            label = { Text("Cerrar sesión") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onLogout()
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
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
                            modifier = Modifier
                                .padding(start = 8.dp, end = 12.dp)
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .border(4.dp, MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Menú",
                                modifier = Modifier.size(26.dp),
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

private val DrawerItemHeight = 44.dp

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (12 + row.depth * 16).dp, end = 12.dp, top = 6.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            is MenuDisplayRow.Item -> {
                NavigationDrawerItem(
                    label = { Text(row.menu.label) },
                    selected = selectedMenuId == row.menu.id,
                    modifier = Modifier
                        .padding(start = (row.depth * 16).dp)
                        .height(DrawerItemHeight),
                    onClick = { onSelectMenu(row.menu) },
                )
            }
        }
    }
}
