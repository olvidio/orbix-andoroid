package com.orbix.mobile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import okhttp3.OkHttpClient

/** Delegado a la cuadrícula en modo consulta (`ver_plan_de_misas.php`). */
@Composable
fun VerPlanDeMisasScreen(
    client: OkHttpClient,
    baseUrl: String,
    contentPadding: PaddingValues,
) {
    PlanDeMisasCuadriculaScreen(
        client = client,
        baseUrl = baseUrl,
        contentPadding = contentPadding,
        mode = CuadriculaPlanMode.Ver,
    )
}
