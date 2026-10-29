package com.orbix.mobile

import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class ComSacdActivPeriodoPage(
    val permModTxt: Boolean,
)

data class ActividadComunicacion(
    val propio: Boolean,
    val fIni: String,
    val fFin: String,
    val nombreUbi: String,
    val sfsv: String,
    val actividad: String,
    val asistentes: String,
    val encargado: String,
    val observ: String,
    val cargo: String,
    val nomTipo: String,
)

data class SacdComunicacion(
    val idNom: Int,
    val nomAp: String,
    /** Cabeceras de columnas y textos (`t_f_ini`, `com_sacd`, …). */
    val txt: Map<String, String>,
    val actividades: List<ActividadComunicacion>,
)

data class ComunicacionActivSacdResult(
    val periodoTxt: String,
    val lugarFecha: String,
    val sacds: List<SacdComunicacion>,
    val sacdsPaso: List<SacdComunicacion>,
    val mensajePeriodo: String?,
)

private val PERIODO_ACTIV_LABELS = linkedMapOf(
    "tot_any" to "Todo el año",
    "trimestre_1" to "Primer trimestre",
    "trimestre_2" to "Segundo trimestre",
    "trimestre_3" to "Tercer trimestre",
    "trimestre_4" to "Cuarto trimestre",
    "otro" to "Otro",
)

fun periodoActivLabels(): Map<String, String> = PERIODO_ACTIV_LABELS

/** Año anterior, actual y siguiente (como en la web para esta pantalla). */
fun yearActivOptions(): List<Pair<String, String>> {
    val current = Calendar.getInstance().get(Calendar.YEAR)
    return listOf(current - 1, current, current + 1).map { y ->
        y.toString() to y.toString()
    }
}

fun fetchComSacdActivPeriodoPage(
    client: OkHttpClient,
    baseUrl: String,
): ComSacdActivPeriodoPage? {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/actividadessacd/com_sacd_activ_periodo_page_data",
        emptyMap(),
    ) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    return ComSacdActivPeriodoPage(
        permModTxt = data.optBoolean("perm_mod_txt", true),
    )
}

fun fetchComunicacionActivSacd(
    client: OkHttpClient,
    baseUrl: String,
    periodo: String,
    year: String,
    empiezaMin: String = "",
    empiezaMax: String = "",
    que: String = "nagd",
    idNom: Int = 0,
    propuesta: String = "",
): ComunicacionActivSacdResult? {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/actividadessacd/comunicacion_activ_sacd_data",
        mapOf(
            "que" to que,
            "id_nom" to idNom.toString(),
            "propuesta" to propuesta,
            "periodo" to periodo,
            "year" to year,
            "empiezamin" to empiezaMin,
            "empiezamax" to empiezaMax,
            "sacd" to "uno",
        ),
    ) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    return parseComunicacionActivSacd(data)
}

private fun parseComunicacionActivSacd(data: JSONObject): ComunicacionActivSacdResult {
    val mensaje = data.optStr("mensaje_periodo").takeIf { it.isNotEmpty() }
    return ComunicacionActivSacdResult(
        periodoTxt = data.optStr("periodo_txt", ""),
        lugarFecha = data.optStr("lugar_fecha", ""),
        sacds = parseSacdList(data.optJSONArray("sacds")),
        sacdsPaso = parseSacdList(data.optJSONArray("sacds_paso")),
        mensajePeriodo = mensaje,
    )
}

private fun parseSacdList(arr: JSONArray?): List<SacdComunicacion> {
    if (arr == null) return emptyList()
    val out = mutableListOf<SacdComunicacion>()
    for (i in 0 until arr.length()) {
        val row = arr.optJSONObject(i) ?: continue
        out.add(
            SacdComunicacion(
                idNom = row.optInt("id_nom"),
                nomAp = row.optStr("nom_ap", ""),
                txt = jsonObjectToStringMap(row.optJSONObject("txt")),
                actividades = parseActividades(row.optJSONArray("actividades")),
            ),
        )
    }
    return out
}

private fun parseActividades(arr: JSONArray?): List<ActividadComunicacion> {
    if (arr == null) return emptyList()
    val out = mutableListOf<ActividadComunicacion>()
    for (i in 0 until arr.length()) {
        val row = arr.optJSONObject(i) ?: continue
        out.add(
            ActividadComunicacion(
                propio = parsePropio(row.opt("propio")),
                fIni = row.optStr("f_ini", ""),
                fFin = row.optStr("f_fin", ""),
                nombreUbi = row.optStr("nombre_ubi", ""),
                sfsv = row.optStr("sfsv", ""),
                actividad = row.optStr("actividad", ""),
                asistentes = row.optStr("asistentes", ""),
                encargado = row.optStr("encargado", ""),
                observ = row.optStr("observ", "").let { v ->
                    if (v == "null" || v.isEmpty()) "" else v
                },
                cargo = row.optStr("cargo", "").let { v ->
                    if (v == "null" || v.isEmpty()) "" else v
                },
                nomTipo = row.optStr("nom_tipo", ""),
            ),
        )
    }
    return out
}

private fun parsePropio(raw: Any?): Boolean {
    return when (raw) {
        is Boolean -> raw
        is String -> raw == "t" || raw.equals("true", ignoreCase = true)
        else -> false
    }
}

private fun jsonObjectToStringMap(obj: JSONObject?): Map<String, String> {
    if (obj == null) return emptyMap()
    val out = linkedMapOf<String, String>()
    val keys = obj.keys()
    while (keys.hasNext()) {
        val k = keys.next()
        val v = obj.opt(k)
        out[k] = when (v) {
            null, JSONObject.NULL -> ""
            else -> v.toString()
        }
    }
    return out
}
