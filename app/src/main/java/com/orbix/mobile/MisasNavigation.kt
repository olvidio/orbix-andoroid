package com.orbix.mobile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import okhttp3.OkHttpClient

/** Enrutador de pantallas nativas del módulo misas (plan de misas). */
@Composable
fun MisasNativeScreen(
    screen: NativeMenuScreen,
    client: OkHttpClient,
    baseUrl: String,
    contentPadding: PaddingValues,
) {
    when (screen) {
        NativeMenuScreen.PlanMisas -> PlanMisasScreen(client, baseUrl, contentPadding)
        NativeMenuScreen.VerPlanDeMisas -> PlanDeMisasCuadriculaScreen(
            client, baseUrl, contentPadding, CuadriculaPlanMode.Ver,
        )
        NativeMenuScreen.ModificarPlanDeMisas -> PlanDeMisasCuadriculaScreen(
            client, baseUrl, contentPadding, CuadriculaPlanMode.Modificar,
        )
        NativeMenuScreen.PrepararPlanDeMisas -> PlanDeMisasCuadriculaScreen(
            client, baseUrl, contentPadding, CuadriculaPlanMode.Preparar,
        )
        NativeMenuScreen.ModificarPlantilla -> PlanDeMisasCuadriculaScreen(
            client, baseUrl, contentPadding, CuadriculaPlanMode.ModificarPlantilla,
        )
        NativeMenuScreen.CambiarStatusPlan -> PlanDeMisasCuadriculaScreen(
            client, baseUrl, contentPadding, CuadriculaPlanMode.CambiarStatus,
        )
        NativeMenuScreen.VerPlanSacd -> VerPlanSacdScreen(client, baseUrl, contentPadding)
        NativeMenuScreen.VerPlanCtr -> VerPlanCtrScreen(client, baseUrl, contentPadding)
        NativeMenuScreen.ModificarEncargos -> EncargosZonaScreen(client, baseUrl, contentPadding)
        NativeMenuScreen.ModificarEncargosCentros -> EncargosCentrosScreen(
            client, baseUrl, contentPadding,
        )
        NativeMenuScreen.ModificarIniciales -> InicialesZonaScreen(client, baseUrl, contentPadding)
        NativeMenuScreen.AtencionActividades -> AtencionActividadesScreen(client, baseUrl, contentPadding)
        NativeMenuScreen.PlanningZonas -> PlanningZonasScreen(client, baseUrl, contentPadding)
        NativeMenuScreen.Ausencias -> AusenciasScreen(
            client, baseUrl, contentPadding, AusenciasMode.SacdLista,
        )
        NativeMenuScreen.AusenciasJefe -> AusenciasScreen(
            client, baseUrl, contentPadding, AusenciasMode.JefeZona,
        )
        NativeMenuScreen.NuevoPlan -> NuevoPlanScreen(
            client, baseUrl, contentPadding, propuestaCalendario = true,
        )
        NativeMenuScreen.PlanningCasa -> NuevoPlanScreen(
            client, baseUrl, contentPadding, propuestaCalendario = false,
        )
        NativeMenuScreen.Pending -> Unit
    }
}

fun nativeScreenForController(controller: String): NativeMenuScreen {
    return when {
        controller.contains("misas_index") -> NativeMenuScreen.PlanMisas
        controller.contains("ver_plan_de_misas") -> NativeMenuScreen.VerPlanDeMisas
        controller.contains("modificar_plan_de_misas") -> NativeMenuScreen.ModificarPlanDeMisas
        controller.contains("preparar_plan_de_misas") -> NativeMenuScreen.PrepararPlanDeMisas
        controller.contains("modificar_plantilla") -> NativeMenuScreen.ModificarPlantilla
        controller.contains("cambiar_status") -> NativeMenuScreen.CambiarStatusPlan
        controller.contains("buscar_plan_sacd") -> NativeMenuScreen.VerPlanSacd
        controller.contains("buscar_plan_ctr") -> NativeMenuScreen.VerPlanCtr
        controller.contains("modificar_encargos_centros") -> NativeMenuScreen.ModificarEncargosCentros
        controller.contains("modificar_encargos") -> NativeMenuScreen.ModificarEncargos
        controller.contains("modificar_iniciales_sacd_zona") -> NativeMenuScreen.ModificarIniciales
        controller.contains("com_sacd_activ_periodo") -> NativeMenuScreen.AtencionActividades
        controller.contains("planning_zones_que") -> NativeMenuScreen.PlanningZonas
        controller.contains("sacd_ausencias_jefe_zona") -> NativeMenuScreen.AusenciasJefe
        controller.contains("sacd_ausencias") -> NativeMenuScreen.Ausencias
        controller.contains("planning_casa_que") -> NativeMenuScreen.PlanningCasa
        else -> NativeMenuScreen.Pending
    }
}

fun isMisasNativeScreen(screen: NativeMenuScreen): Boolean {
    return screen != NativeMenuScreen.Pending
}

fun nativeScreenFor(menu: OrbixMenuItem): NativeMenuScreen {
    val screen = nativeScreenForController(menu.url)
    // Refinamiento para distinguir Planning Casa vs Nuevo Plan (Propuesta)
    if (screen == NativeMenuScreen.PlanningCasa && isNuevoPlanPropuesta(menu)) {
        return NativeMenuScreen.NuevoPlan
    }
    return screen
}
