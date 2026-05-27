package com.orbix.mobile

import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class PlanningZonesQuePage(
    val opcionesZonas: Map<String, String>,
    val error: String?,
)

data class PlanningActividad(
    val nomCurt: String,
    val nomLlarg: String,
    val fIni: String,
    val hIni: String,
    val fFi: String,
    val hFi: String,
    val css: String,
)

data class PlanningPersona(
    val nombre: String,
    val actividades: List<PlanningActividad>,
)

data class PlanningZonaBlock(
    val cabecera: String,
    val personas: List<PlanningPersona>,
)

data class PlanningZonesSelectResult(
    val titulo: String,
    val planningIniIso: String,
    val planningFinIso: String,
    val zonas: List<PlanningZonaBlock>,
)

private val TRIMESTRE_LABELS = linkedMapOf(
    "1" to "Enero–marzo",
    "101" to "Enero",
    "102" to "Febrero",
    "103" to "Marzo",
    "2" to "Abril–junio",
    "104" to "Abril",
    "105" to "Mayo",
    "106" to "Junio",
    "3" to "Julio–septiembre",
    "107" to "Julio",
    "108" to "Agosto",
    "109" to "Septiembre",
    "4" to "Octubre–diciembre",
    "110" to "Octubre",
    "111" to "Noviembre",
    "112" to "Diciembre",
    "5" to "Navidad (dic–ene)",
    "6" to "Verano (jul–ago)",
)

fun planningTrimestreLabels(): Map<String, String> = TRIMESTRE_LABELS

fun defaultPlanningTrimestre(): String {
    val month = Calendar.getInstance().get(Calendar.MONTH) + 1
    return when {
        month < 4 -> "1"
        month < 7 -> "2"
        month > 8 && month < 10 -> "3"
        month > 9 -> "4"
        else -> "2"
    }
}

fun fetchPlanningZonesQuePage(
    client: OkHttpClient,
    baseUrl: String,
): PlanningZonesQuePage? {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/planning/planning_zones_que_data",
        emptyMap(),
    ) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    return PlanningZonesQuePage(
        opcionesZonas = jsonObjectToStringMap(data.optJSONObject("opciones_zonas")),
        error = data.optString("error").takeIf { it.isNotEmpty() },
    )
}

fun fetchPlanningZonesSelect(
    client: OkHttpClient,
    baseUrl: String,
    trimestre: String,
    year: String,
    idZona: String,
): PlanningZonesSelectResult? {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/planning/planning_zones_select_data",
        mapOf(
            "modelo" to "1",
            "propuesta" to "1",
            "trimestre" to trimestre,
            "year" to year,
            "id_zona" to idZona,
            "actividad" to "si",
        ),
    ) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    return parsePlanningZonesSelect(data)
}

/** Calendario diario: todos los días del periodo y sacds ocupados cada día. */
data class PlanningAgendaDia(
    val isoKey: String,
    val fechaLabel: String,
    val sortKey: Long,
    val sacds: List<PlanningSacdDia>,
)

data class PlanningSacdDia(
    val persona: String,
    val actividades: List<PlanningActividad>,
)

fun PlanningZonesSelectResult.toAgenda(): List<PlanningAgendaDia> {
    val personas = zonas.flatMap { it.personas }
    return iterateIsoDays(planningIniIso, planningFinIso).map { iso ->
        val sacds = personas.mapNotNull { persona ->
            val acts = persona.actividades.filter { activityActiveOnIsoDay(it, iso) }
            if (acts.isEmpty()) {
                null
            } else {
                PlanningSacdDia(persona = persona.nombre, actividades = acts)
            }
        }.sortedBy { it.persona.lowercase() }
        PlanningAgendaDia(
            isoKey = iso,
            fechaLabel = formatPlanningDayLabel(iso),
            sortKey = isoToSortKey(iso),
            sacds = sacds,
        )
    }
}

private fun iterateIsoDays(isoIni: String, isoFin: String): List<String> {
    val start = parseIsoToCalendar(isoIni) ?: return emptyList()
    val end = parseIsoToCalendar(isoFin) ?: return emptyList()
    if (start.after(end)) return emptyList()
    val out = mutableListOf<String>()
    val cur = (start.clone() as Calendar)
    while (!cur.after(end)) {
        out.add(calendarToIso(cur))
        cur.add(Calendar.DAY_OF_MONTH, 1)
    }
    return out
}

private fun activityActiveOnIsoDay(act: PlanningActividad, dayIso: String): Boolean {
    val day = parseIsoToCalendar(dayIso) ?: return false
    val start = parsePlanningSlashedDate(act.fIni) ?: parsePlanningSlashedDate(act.fFi) ?: return false
    val end = parsePlanningSlashedDate(act.fFi) ?: start
    return !day.before(truncateToDay(start)) && !day.after(truncateToDay(end))
}

private fun truncateToDay(cal: Calendar): Calendar {
    return (cal.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

private fun parseIsoToCalendar(iso: String): Calendar? {
    val parts = iso.split("-")
    if (parts.size != 3) return null
    val y = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val d = parts[2].toIntOrNull() ?: return null
    return truncateToDay(
        Calendar.getInstance().apply { set(y, m - 1, d) },
    )
}

private fun parsePlanningSlashedDate(fecha: String): Calendar? {
    val parts = fecha.split("/")
    if (parts.size != 3) return null
    val d = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val y = parts[2].toIntOrNull() ?: return null
    return truncateToDay(
        Calendar.getInstance().apply { set(y, m - 1, d) },
    )
}

private fun calendarToIso(cal: Calendar): String {
    val y = cal.get(Calendar.YEAR)
    val m = cal.get(Calendar.MONTH) + 1
    val d = cal.get(Calendar.DAY_OF_MONTH)
    return "%04d-%02d-%02d".format(y, m, d)
}

private fun isoToSortKey(iso: String): Long {
    val parts = iso.split("-")
    if (parts.size != 3) return Long.MAX_VALUE
    val y = parts[0].toIntOrNull() ?: return Long.MAX_VALUE
    val m = parts[1].toIntOrNull() ?: return Long.MAX_VALUE
    val d = parts[2].toIntOrNull() ?: return Long.MAX_VALUE
    return y * 10000L + m * 100L + d
}

private fun formatPlanningDayLabel(iso: String): String {
    val cal = parseIsoToCalendar(iso) ?: return iso
    val weekdays = arrayOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
    val w = weekdays[cal.get(Calendar.DAY_OF_WEEK) - 1]
    val d = cal.get(Calendar.DAY_OF_MONTH)
    val m = cal.get(Calendar.MONTH) + 1
    val y = cal.get(Calendar.YEAR)
    return "$w $d/$m/$y"
}

private fun parsePlanningZonesSelect(data: JSONObject): PlanningZonesSelectResult {
    val zonasCount = data.optInt("zonas", 0)
    val cabeceras = data.optJSONObject("cabeceras_por_zona")
    val actividades = data.optJSONObject("actividades_por_zona")
    val blocks = mutableListOf<PlanningZonaBlock>()

    for (i in 1..zonasCount) {
        val key = i.toString()
        val cabecera = cabeceras?.optString(key, "").orEmpty()
        val rawZona = actividades?.optJSONObject(key)
        blocks.add(
            PlanningZonaBlock(
                cabecera = cabecera,
                personas = if (rawZona != null) parsePersonasZona(rawZona) else emptyList(),
            ),
        )
    }

    return PlanningZonesSelectResult(
        titulo = data.optString("titulo", ""),
        planningIniIso = data.optString("planning_ini_iso", ""),
        planningFinIso = data.optString("planning_fin_iso", ""),
        zonas = blocks,
    )
}

private fun parsePersonasZona(raw: JSONObject): List<PlanningPersona> {
    val out = mutableListOf<PlanningPersona>()
    val keys = raw.keys()
    while (keys.hasNext()) {
        val personName = keys.next()
        val inner = raw.optJSONObject(personName) ?: continue
        val activities = mutableListOf<PlanningActividad>()
        val innerKeys = inner.keys()
        while (innerKeys.hasNext()) {
            val arr = inner.optJSONArray(innerKeys.next()) ?: continue
            for (i in 0 until arr.length()) {
                val row = arr.optJSONObject(i) ?: continue
                activities.add(parsePlanningActividad(row))
            }
        }
        out.add(PlanningPersona(nombre = personName, actividades = activities))
    }
    return out.sortedBy { it.nombre.lowercase() }
}

private fun parsePlanningActividad(row: JSONObject): PlanningActividad {
    return PlanningActividad(
        nomCurt = row.optString("nom_curt", ""),
        nomLlarg = row.optString("nom_llarg", ""),
        fIni = row.optString("f_ini", ""),
        hIni = row.optString("h_ini", ""),
        fFi = row.optString("f_fi", ""),
        hFi = row.optString("h_fi", ""),
        css = row.optString("css", ""),
    )
}

private fun jsonObjectToStringMap(obj: JSONObject?): Map<String, String> {
    if (obj == null) return emptyMap()
    val out = linkedMapOf<String, String>()
    val keys = obj.keys()
    while (keys.hasNext()) {
        val k = keys.next()
        out[k] = obj.optString(k, k)
    }
    return out
}

// --- Planning por casas (nuevo plan / planning casa) ---

data class PlanningCasaQuePage(
    val filtroCasas: JSONObject?,
    val modoCasas: String,
)

data class PlanningCasaBlock(
    val nombreCasa: String,
    val actividades: List<PlanningActividad>,
)

data class PlanningCasaVerResult(
    val casas: List<PlanningCasaBlock>,
    val fIniIso: String,
    val fFinIso: String,
)

private val PLANNING_CASA_PERIODO = linkedMapOf(
    "tot_any" to "Todo el año",
    "trimestre_1" to "Primer trimestre",
    "trimestre_2" to "Segundo trimestre",
    "trimestre_3" to "Tercer trimestre",
    "trimestre_4" to "Cuarto trimestre",
    "otro" to "Otro",
)

fun planningCasaPeriodoLabels(): Map<String, String> = PLANNING_CASA_PERIODO

fun defaultPlanningCasaPeriodo(): String {
    val month = Calendar.getInstance().get(Calendar.MONTH) + 1
    return when {
        month < 4 -> "trimestre_1"
        month < 7 -> "trimestre_2"
        month < 10 -> "trimestre_3"
        else -> "trimestre_4"
    }
}

fun isNuevoPlanPropuesta(menu: OrbixMenuItem): Boolean {
    return menu.url.contains("propuesta_calendario=1") ||
        menu.label.contains("nuevo", ignoreCase = true)
}

fun cdcSelOptionsForModo(modo: String): Map<String, String> {
    return when (modo) {
        "sv" -> linkedMapOf(
            "3" to "Casas comunes",
            "4" to "Casas sv",
            "9" to "Una casa o lugar",
            "11" to "Todas las actividades sv",
        )
        "sf" -> linkedMapOf(
            "3" to "Casas comunes",
            "5" to "Casas sf",
            "6" to "Casas y ctr sf",
            "9" to "Una casa o lugar",
            "12" to "Todas las actividades sf",
        )
        "casa" -> linkedMapOf("9" to "Una casa o lugar")
        else -> linkedMapOf(
            "1" to "Casas sólo sv",
            "2" to "Casas sólo sf",
            "3" to "Casas comunes",
            "4" to "Casas sv",
            "5" to "Casas sf",
            "6" to "Casas y ctr sf",
            "9" to "Una casa o lugar",
            "11" to "Todas las actividades sv",
            "12" to "Todas las actividades sf",
        )
    }
}

fun fetchPlanningCasaQuePage(client: OkHttpClient, baseUrl: String): PlanningCasaQuePage? {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/planning/planning_casa_que_data",
        emptyMap(),
    ) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    return PlanningCasaQuePage(
        filtroCasas = data.optJSONObject("filtro"),
        modoCasas = data.optString("modo_casas", "all").ifEmpty { "all" },
    )
}

fun fetchCasasOpciones(
    client: OkHttpClient,
    baseUrl: String,
    filtro: JSONObject?,
): Map<String, String> {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/ubis/casas_opciones_data",
        filtroCasasToForm(filtro),
    ) ?: return emptyMap()
    val data = parseContestarDataObject(body) ?: return emptyMap()
    return jsonObjectToStringMap(data.optJSONObject("opciones"))
}

fun fetchPlanningCasaVer(
    client: OkHttpClient,
    baseUrl: String,
    cdcSel: String,
    selectedCasaIds: List<String>,
    periodo: String,
    year: String,
    sinActiv: Boolean,
    propuestaCalendario: Boolean,
    empiezaMin: String = "",
    empiezaMax: String = "",
): PlanningCasaVerResult? {
    val (fIni, fFin) = planningCasaDateRange(periodo, year.toIntOrNull() ?: Calendar.getInstance().get(
        Calendar.YEAR,
    ), empiezaMin, empiezaMax)
    val params = mutableMapOf(
        "modelo" to "1",
        "cdc_sel" to cdcSel,
        "sin_activ" to if (sinActiv) "1" else "0",
        "year" to year,
        "periodo" to periodo,
        "empiezamin" to empiezaMin,
        "empiezamax" to empiezaMax,
        "propuesta_calendario" to if (propuestaCalendario) "1" else "",
        "f_ini_iso" to fIni,
        "f_fin_iso" to fFin,
        "sSeleccionados" to "",
    )
    if (cdcSel == "9" && selectedCasaIds.isNotEmpty()) {
        params["sSeleccionados"] = selectedCasaIds.joinToString(",")
        selectedCasaIds.forEachIndexed { index, id ->
            params["id_cdc[$index]"] = id
        }
    }
    val body = postSrcForm(client, baseUrl, "/src/planning/planning_casa_ver_data", params) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    return PlanningCasaVerResult(
        casas = parsePlanningCasaActividades(data.optJSONObject("a_actividades")),
        fIniIso = fIni,
        fFinIso = fFin,
    )
}

private fun filtroCasasToForm(filtro: JSONObject?): Map<String, String> {
    val out = mutableMapOf<String, String>()
    if (filtro == null) {
        out["active"] = "1"
        return out
    }
    out["active"] = if (filtro.optBoolean("active", true)) "1" else "0"
    if (filtro.has("sv")) out["sv"] = if (filtro.optBoolean("sv")) "1" else "0"
    if (filtro.has("sf")) out["sf"] = if (filtro.optBoolean("sf")) "1" else "0"
    val idArr = filtro.optJSONArray("id_ubi_in")
    if (idArr != null) {
        val ids = buildList {
            for (i in 0 until idArr.length()) {
                val id = idArr.optInt(i, 0)
                if (id > 0) add(id.toString())
            }
        }
        if (ids.isNotEmpty()) out["id_ubi_in"] = ids.joinToString(",")
    }
    return out
}

private fun planningCasaDateRange(
    periodo: String,
    year: Int,
    empiezaMin: String,
    empiezaMax: String,
): Pair<String, String> {
    if (periodo == "otro" && empiezaMin.isNotEmpty() && empiezaMax.isNotEmpty()) {
        return slashedDateToIso(empiezaMin) to slashedDateToIso(empiezaMax)
    }
    return when (periodo) {
        "trimestre_1" -> "${year}-01-01" to "${year}-03-31"
        "trimestre_2" -> "${year}-04-01" to "${year}-06-30"
        "trimestre_3" -> "${year}-07-01" to "${year}-09-30"
        "trimestre_4" -> "${year}-10-01" to "${year}-12-31"
        "tot_any" -> "${year}-01-01" to "${year}-12-31"
        else -> "${year}-01-01" to "${year}-12-31"
    }
}

private fun slashedDateToIso(fecha: String): String {
    val parts = fecha.replace('-', '/').split("/")
    if (parts.size != 3) return fecha
    val d = parts[0].padStart(2, '0')
    val m = parts[1].padStart(2, '0')
    val y = parts[2]
    return "$y-$m-$d"
}

private fun parsePlanningCasaActividades(obj: JSONObject?): List<PlanningCasaBlock> {
    if (obj == null) return emptyList()
    val blocks = mutableListOf<PlanningCasaBlock>()
    val keys = obj.keys()
    while (keys.hasNext()) {
        val casaName = keys.next()
        if (casaName.contains("##")) continue
        val inner = obj.optJSONObject(casaName) ?: continue
        val activities = mutableListOf<PlanningActividad>()
        val innerKeys = inner.keys()
        while (innerKeys.hasNext()) {
            val cdcKey = innerKeys.next()
            if (cdcKey.contains("##")) continue
            val arr = inner.optJSONArray(cdcKey) ?: continue
            for (i in 0 until arr.length()) {
                val row = arr.optJSONObject(i) ?: continue
                activities.add(parsePlanningActividad(row))
            }
        }
        blocks.add(PlanningCasaBlock(nombreCasa = casaName, actividades = activities))
    }
    return blocks
        .filter { it.nombreCasa.isNotEmpty() }
        .sortedBy { it.nombreCasa.lowercase() }
}
