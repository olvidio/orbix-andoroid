package com.orbix.mobile

import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject

data class SacdAusenciasJefePage(
    val sacdOpciones: Map<String, String>,
)

data class SacdSelectPage(
    val opciones: Map<String, String>,
    val selected: String,
)

data class SacdAusenciaRow(
    val idEnc: Int,
    val descEnc: String,
    val idItem: Int,
    val inicio: String?,
    val fin: String?,
    val dedicM: String,
    val dedicT: String,
    val dedicV: String,
)

data class SacdAusenciasResult(
    val tiposDisponibles: Map<String, String>,
    val rows: List<SacdAusenciaRow>,
)

fun filtroSacdLabels(): Map<String, String> = linkedMapOf(
    "n" to "n",
    "a" to "agd",
    "sssc" to "sss+",
    "cp_sss" to "cp",
)

fun fetchSacdAusenciasJefeZonaPage(
    client: OkHttpClient,
    baseUrl: String,
): SacdAusenciasJefePage? {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/encargossacd/sacd_ausencias_jefe_zona_data",
        emptyMap(),
    ) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    return SacdAusenciasJefePage(
        sacdOpciones = jsonObjectToStringMap(data.optJSONObject("a_sacd")),
    )
}

fun fetchSacdSelectPage(
    client: OkHttpClient,
    baseUrl: String,
    filtroSacd: String,
    idNom: String = "0",
): SacdSelectPage? {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/encargossacd/sacd_select_data",
        mapOf(
            "filtro_sacd" to filtroSacd,
            "id_nom" to idNom,
        ),
    ) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    return SacdSelectPage(
        opciones = jsonObjectToStringMap(data.optJSONObject("opciones")),
        selected = data.opt("selected")?.toString().orEmpty(),
    )
}

fun fetchSacdAusencias(
    client: OkHttpClient,
    baseUrl: String,
    idNom: String,
    historial: Boolean,
): SacdAusenciasResult? {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/encargossacd/sacd_ausencias_get_data",
        mapOf(
            "id_nom" to idNom,
            "historial" to if (historial) "1" else "0",
        ),
    ) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    val tipos = jsonObjectToStringMap(data.optJSONObject("array_tipo_ausencias"))
    val rowsArr = data.optJSONArray("filas") ?: JSONArray()
    val rows = mutableListOf<SacdAusenciaRow>()
    for (i in 0 until rowsArr.length()) {
        val row = rowsArr.optJSONObject(i) ?: continue
        rows.add(
            SacdAusenciaRow(
                idEnc = row.optInt("id_enc", 0),
                descEnc = row.optString("desc_enc", ""),
                idItem = row.optInt("id_item", 0),
                inicio = row.optString("inicio").takeIf { it.isNotEmpty() },
                fin = row.optString("fin").takeIf { it.isNotEmpty() },
                dedicM = row.optString("dedic_m", ""),
                dedicT = row.optString("dedic_t", ""),
                dedicV = row.optString("dedic_v", ""),
            ),
        )
    }
    return SacdAusenciasResult(tiposDisponibles = tipos, rows = rows)
}

data class SacdAusenciaDraft(
    val localKey: String,
    val idEnc: Int,
    val descEnc: String,
    val idItem: Int,
    val inicio: String,
    val fin: String,
    val dedicM: String = "",
    val dedicT: String = "",
    val dedicV: String = "",
)

fun SacdAusenciaRow.toDraft(index: Int): SacdAusenciaDraft = SacdAusenciaDraft(
    localKey = "item-$idItem-$idEnc-$index",
    idEnc = idEnc,
    descEnc = descEnc,
    idItem = idItem,
    inicio = inicio.orEmpty(),
    fin = fin.orEmpty(),
    dedicM = dedicM,
    dedicT = dedicT,
    dedicV = dedicV,
)

/**
 * Guarda ausencias (`inicio[]`, `fin[]`, `id_enc[]`, `id_item[]`).
 * `id_item=0` inserta; fechas vacías en ítem existente eliminan.
 */
fun updateSacdAusencias(
    client: OkHttpClient,
    baseUrl: String,
    idNom: String,
    drafts: List<SacdAusenciaDraft>,
): SrcMutationResult {
    val toSend = drafts.filter { draft ->
        draft.idItem != 0 || draft.inicio.isNotBlank() || draft.fin.isNotBlank()
    }
    val fields = mutableListOf(
        "id_nom" to idNom,
        "enc_num" to toSend.size.toString(),
    )
    toSend.forEachIndexed { i, draft ->
        fields += "id_enc[$i]" to draft.idEnc.toString()
        fields += "id_item[$i]" to draft.idItem.toString()
        fields += "inicio[$i]" to draft.inicio.trim()
        fields += "fin[$i]" to draft.fin.trim()
    }
    val body = postSrcFormFields(
        client,
        baseUrl,
        "/src/encargossacd/sacd_ausencias_update",
        fields,
    )
    return mutationResultFromEnvelope(body?.let { parseSrcEnvelope(it) })
}

/** Extrae `id_nom` de claves `iniciales#id_nom` del desplegable jefe de zona. */
fun sacdKeyToIdNom(key: String): String {
    val parts = key.split('#')
    return if (parts.size >= 2) parts[1] else key
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

enum class AusenciasMode {
    /** `sacd_ausencias.php` — filtro tipo + lista SACD. */
    SacdLista,
    /** `sacd_ausencias_jefe_zona.php` — desplegable jefe de zona. */
    JefeZona,
}

fun ausenciasModeForMenu(menu: OrbixMenuItem): AusenciasMode {
    return if (menu.url.contains("sacd_ausencias_jefe_zona")) {
        AusenciasMode.JefeZona
    } else {
        AusenciasMode.SacdLista
    }
}
