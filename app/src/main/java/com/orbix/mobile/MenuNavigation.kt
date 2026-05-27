package com.orbix.mobile

/**
 * Pantallas nativas enlazadas a entradas del menú web (`frontend/.../controller/...`).
 */
enum class NativeMenuScreen {
    PlanMisas,
    VerPlanDeMisas,
    ModificarPlanDeMisas,
    PrepararPlanDeMisas,
    ModificarPlantilla,
    CambiarStatusPlan,
    VerPlanSacd,
    VerPlanCtr,
    ModificarEncargos,
    ModificarEncargosCentros,
    ModificarIniciales,
    AtencionActividades,
    PlanningZonas,
    Ausencias,
    AusenciasJefe,
    NuevoPlan,
    PlanningCasa,
    Pending,
}

sealed class MenuDisplayRow {
    abstract val depth: Int

    data class Section(
        val title: String,
        override val depth: Int,
    ) : MenuDisplayRow()

    data class Item(
        val menu: OrbixMenuItem,
        override val depth: Int,
        val native: NativeMenuScreen,
    ) : MenuDisplayRow()
}

/** Orden y agrupación como en el layout hamburguesa de la web. */
fun buildMenuDisplayRows(menus: List<OrbixMenuItem>): List<MenuDisplayRow> {
    val sorted = menus.sortedWith(
        compareBy<OrbixMenuItem>(
            { it.order.size },
            { it.order.joinToString(".") { n -> n.toString().padStart(4, '0') } },
            { it.label.lowercase() },
        ),
    )
    val out = mutableListOf<MenuDisplayRow>()
    for (menu in sorted) {
        val depth = (menu.order.size - 1).coerceAtLeast(0)
        if (menu.url.isEmpty()) {
            out.add(MenuDisplayRow.Section(title = menu.label, depth = depth))
        } else {
            out.add(
                MenuDisplayRow.Item(
                    menu = menu,
                    depth = depth,
                    native = nativeScreenFor(menu),
                ),
            )
        }
    }
    return out
}
