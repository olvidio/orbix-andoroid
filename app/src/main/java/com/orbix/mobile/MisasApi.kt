package com.orbix.mobile

import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject

data class PlanDeMisasPantalla(
    val zonasOpciones: Map<String, String>,
    val ordenOpciones: Map<String, String>,
    val tiposPlantilla: Map<String, String> = emptyMap(),
    val plantillaSelected: String? = null,
)

data class CuadriculaZona(
    val dateColumns: List<String>,
    val rows: List<CuadriculaRow>,
    val preferenceWarning: String? = null,
)

data class CuadriculaCellMeta(
    val uuidItem: String,
    val key: String,
    val idEnc: String,
    val dia: String,
    val tstart: String,
    val tend: String,
    val observ: String,
    val tipo: String,
    val color: String = "",
    val texto: String = "",
) {
    val isEditable: Boolean get() = tipo == "misas" && idEnc.isNotEmpty() && dia.isNotEmpty()
}

data class CuadriculaRow(
    val encargo: String,
    val isTitle: Boolean,
    val cells: Map<String, String>,
    val metaByDate: Map<String, CuadriculaCellMeta> = emptyMap(),
)

data class CambiarStatusPantalla(
    val zonasOpciones: Map<String, String>,
    val ordenOpciones: Map<String, String>,
    val estadosOpciones: Map<String, String>,
)

data class BuscarPlanSacdPage(
    val sacdOpciones: Map<String, String>,
    val sacdSelected: String,
)

data class PlanSacdRow(
    val dia: String,
    val encargo: String,
    val observ: String,
)

data class BuscarPlanCtrPage(
    val view: String,
    val zonasOpciones: Map<String, String>,
    val zonasSelected: String,
    val centrosOpciones: Map<String, String>,
    val centrosSelected: String,
    val error: String? = null,
)

data class PlanCtrColumn(
    val letra: String,
    val numDia: String,
    val numMes: String,
    val idDia: String,
)

data class PlanCtrRow(
    val descEnc: String,
    val cells: List<String>,
)

data class PlanCtrLegendItem(
    val iniciales: String,
    val nombre: String,
)

data class PlanCtrGrid(
    val columns: List<PlanCtrColumn>,
    val rows: List<PlanCtrRow>,
    val legend: List<PlanCtrLegendItem>,
)

data class ZonaOpcionesPage(
    val zonasOpciones: Map<String, String>,
    val ordenOpciones: Map<String, String> = emptyMap(),
    val error: String? = null,
)

data class InicialesZonaGrid(
    val rows: List<Map<String, String>>,
)

data class EncargosZonaGrid(
    val rows: List<Map<String, String>>,
    val tiposEncargo: Map<String, String> = emptyMap(),
    val centros: Map<String, String> = emptyMap(),
    val idiomas: Map<String, String> = emptyMap(),
)

data class EncargosCentrosGrid(
    val rows: List<Map<String, String>>,
    val centrosZona: Map<String, String> = emptyMap(),
)

data class DesplegableOpciones(
    val opciones: Map<String, String>,
    val selected: String = "",
)

private val PERIODO_VER = linkedMapOf(
    "esta_semana" to "Esta semana",
    "este_mes" to "Este mes",
    "proxima_semana" to "Próxima semana (lun–dom)",
    "proximo_mes" to "Próximo mes natural",
    "otro" to "Otro",
)

private val PERIODO_PREPARAR = linkedMapOf(
    "proxima_semana" to "Próxima semana (lun–dom)",
    "proximo_mes" to "Próximo mes natural",
    "otro" to "Otro",
)

fun periodoLabelsVer(): Map<String, String> = PERIODO_VER

fun periodoLabelsPreparar(): Map<String, String> = PERIODO_PREPARAR

/** @deprecated use [periodoLabelsVer] */
fun periodoLabels(): Map<String, String> = PERIODO_VER

fun fetchPlanDeMisasPantalla(
    client: OkHttpClient,
    baseUrl: String,
    pantalla: String = "ver",
): PlanDeMisasPantalla? {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/misas/plan_de_misas_pantalla_data",
        mapOf("pantalla" to pantalla),
    ) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    return PlanDeMisasPantalla(
        zonasOpciones = jsonObjectToStringMap(data.optJSONObject("zonas_opciones")),
        ordenOpciones = jsonObjectToStringMap(data.optJSONObject("orden_opciones")),
        tiposPlantilla = jsonObjectToStringMap(data.optJSONObject("tipos_plantilla")),
        plantillaSelected = data.optString("plantilla_selected").takeIf { it.isNotEmpty() },
    )
}

fun fetchCambiarStatusPantalla(
    client: OkHttpClient,
    baseUrl: String,
): CambiarStatusPantalla? {
    val body = postSrcForm(client, baseUrl, "/src/misas/cambiar_status_data", emptyMap()) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    return CambiarStatusPantalla(
        zonasOpciones = jsonObjectToStringMap(data.optJSONObject("zonas_opciones")),
        ordenOpciones = jsonObjectToStringMap(data.optJSONObject("orden_opciones")),
        estadosOpciones = jsonObjectToIntKeyStringMap(data.optJSONObject("estados_opciones")),
    )
}

fun fetchCuadriculaZona(
    client: OkHttpClient,
    baseUrl: String,
    idZona: String,
    periodo: String,
    orden: String,
    tipoPlantilla: String = "p",
    empiezaMin: String = "",
    empiezaMax: String = "",
): CuadriculaZona? {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/misas/ver_cuadricula_zona_data",
        mapOf(
            "id_zona" to idZona,
            "tipo_plantilla" to tipoPlantilla,
            "periodo" to periodo,
            "orden" to orden.ifEmpty { "desc_enc" },
            "empiezamin" to empiezaMin,
            "empiezamax" to empiezaMax,
            "fila" to "0",
            "columna" to "0",
            "seleccion" to "0",
        ),
    ) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    val apiError = data.optString("error")
    if (apiError.isNotEmpty()) {
        return CuadriculaZona(emptyList(), emptyList(), preferenceWarning = apiError)
    }
    return parseCuadriculaZona(data)
}

/** Alias histórico. */
fun fetchVerCuadriculaZona(
    client: OkHttpClient,
    baseUrl: String,
    idZona: String,
    periodo: String,
    orden: String,
    empiezaMin: String = "",
    empiezaMax: String = "",
): CuadriculaZona? = fetchCuadriculaZona(
    client, baseUrl, idZona, periodo, orden, "p", empiezaMin, empiezaMax,
)

fun fetchBuscarPlanSacdPage(client: OkHttpClient, baseUrl: String): BuscarPlanSacdPage? {
    val body = postSrcForm(client, baseUrl, "/src/misas/buscar_plan_sacd_data", emptyMap()) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    return BuscarPlanSacdPage(
        sacdOpciones = jsonObjectToStringMap(data.optJSONObject("sacd_opciones")),
        sacdSelected = data.optString("sacd_selected", ""),
    )
}

fun fetchVerPlanSacd(
    client: OkHttpClient,
    baseUrl: String,
    idSacd: String,
    periodo: String,
    empiezaMin: String = "",
    empiezaMax: String = "",
): List<PlanSacdRow>? {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/misas/ver_plan_sacd_data",
        mapOf(
            "id_sacd" to idSacd,
            "periodo" to periodo,
            "empiezamin" to empiezaMin,
            "empiezamax" to empiezaMax,
        ),
    ) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    val arr = data.optJSONArray("rows") ?: JSONArray()
    val out = mutableListOf<PlanSacdRow>()
    for (i in 0 until arr.length()) {
        val row = arr.optJSONObject(i) ?: continue
        out.add(
            PlanSacdRow(
                dia = row.optString("dia", ""),
                encargo = row.optString("encargo", ""),
                observ = row.optString("observ", ""),
            ),
        )
    }
    return out
}

fun fetchBuscarPlanCtrPage(
    client: OkHttpClient,
    baseUrl: String,
    idZona: String = "0",
): BuscarPlanCtrPage? {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/misas/buscar_plan_ctr_data",
        mapOf("id_zona" to idZona),
    ) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    val view = data.optString("view", "none")
    return BuscarPlanCtrPage(
        view = view,
        zonasOpciones = jsonObjectToStringMap(data.optJSONObject("zonas_opciones")),
        zonasSelected = data.opt("zonas_selected")?.toString().orEmpty(),
        centrosOpciones = jsonObjectToStringMap(data.optJSONObject("centros_opciones")),
        centrosSelected = data.optString("centros_selected", ""),
        error = if (view == "none") "No tiene permiso para ver esta pantalla." else null,
    )
}

fun fetchVerPlanCtr(
    client: OkHttpClient,
    baseUrl: String,
    idUbi: String,
    periodo: String,
    empiezaMin: String = "",
    empiezaMax: String = "",
): PlanCtrGrid? {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/misas/ver_plan_ctr_data",
        mapOf(
            "id_ubi" to idUbi,
            "periodo" to periodo,
            "empiezamin" to empiezaMin,
            "empiezamax" to empiezaMax,
        ),
    ) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    val columns = mutableListOf<PlanCtrColumn>()
    data.optJSONArray("columns")?.let { arr ->
        for (i in 0 until arr.length()) {
            val c = arr.optJSONObject(i) ?: continue
            columns.add(
                PlanCtrColumn(
                    letra = c.optString("letra", ""),
                    numDia = c.optString("num_dia", ""),
                    numMes = c.optString("num_mes", ""),
                    idDia = c.optString("id_dia", ""),
                ),
            )
        }
    }
    val rows = mutableListOf<PlanCtrRow>()
    data.optJSONArray("rows")?.let { arr ->
        for (i in 0 until arr.length()) {
            val r = arr.optJSONObject(i) ?: continue
            val cellsArr = r.optJSONArray("cells")
            val cells = mutableListOf<String>()
            if (cellsArr != null) {
                for (j in 0 until cellsArr.length()) {
                    cells.add(cellsArr.opt(j)?.toString().orEmpty())
                }
            }
            rows.add(PlanCtrRow(descEnc = r.optString("desc_enc", ""), cells = cells))
        }
    }
    val legend = mutableListOf<PlanCtrLegendItem>()
    data.optJSONArray("legend")?.let { arr ->
        for (i in 0 until arr.length()) {
            val l = arr.optJSONObject(i) ?: continue
            legend.add(
                PlanCtrLegendItem(
                    iniciales = l.optString("iniciales", ""),
                    nombre = l.optString("nombre", ""),
                ),
            )
        }
    }
    return PlanCtrGrid(columns = columns, rows = rows, legend = legend)
}

fun fetchModificarEncargosPage(client: OkHttpClient, baseUrl: String): ZonaOpcionesPage? {
    val body = postSrcForm(client, baseUrl, "/src/misas/modificar_encargos_data", emptyMap()) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    val error = data.optString("error").takeIf { it.isNotEmpty() }
    return ZonaOpcionesPage(
        zonasOpciones = jsonObjectToStringMap(data.optJSONObject("a_opciones_zona")),
        ordenOpciones = jsonObjectToStringMap(data.optJSONObject("a_orden")),
        error = error,
    )
}

fun fetchModificarEncargosCentrosPage(client: OkHttpClient, baseUrl: String): ZonaOpcionesPage? {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/misas/modificar_encargos_centros_data",
        emptyMap(),
    ) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    val error = data.optString("error").takeIf { it.isNotEmpty() }
    return ZonaOpcionesPage(
        zonasOpciones = jsonObjectToStringMap(data.optJSONObject("a_opciones_zona")),
        error = error,
    )
}

fun fetchModificarInicialesPage(client: OkHttpClient, baseUrl: String): ZonaOpcionesPage? {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/misas/modificar_iniciales_sacd_zona_data",
        emptyMap(),
    ) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    return ZonaOpcionesPage(
        zonasOpciones = jsonObjectToStringMap(data.optJSONObject("a_opciones")),
    )
}

fun fetchVerEncargosZona(
    client: OkHttpClient,
    baseUrl: String,
    idZona: String,
    orden: String = "desc_enc",
): EncargosZonaGrid? {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/misas/ver_encargos_zona_data",
        mapOf("id_zona" to idZona, "orden" to orden),
    ) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    return EncargosZonaGrid(
        rows = parseJsonRows(data.optJSONArray("rows")),
        tiposEncargo = jsonObjectToStringMap(data.optJSONObject("tipos_encargo")),
        centros = jsonObjectToStringMap(data.optJSONObject("centros")),
        idiomas = jsonObjectToStringMap(data.optJSONObject("idiomas")),
    )
}

fun fetchVerEncargosCentros(
    client: OkHttpClient,
    baseUrl: String,
    idZona: String,
): EncargosCentrosGrid? {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/misas/ver_encargos_centros_data",
        mapOf("id_zona" to idZona),
    ) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    return EncargosCentrosGrid(
        rows = parseJsonRows(data.optJSONArray("rows")),
        centrosZona = jsonObjectToStringMap(data.optJSONObject("a_centros_zona")),
    )
}

fun fetchDesplegableEncargos(
    client: OkHttpClient,
    baseUrl: String,
    idZona: String,
    idEnc: String = "",
): DesplegableOpciones? {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/misas/desplegable_encargos",
        mapOf("id_zona" to idZona, "id_enc" to idEnc),
    ) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    return DesplegableOpciones(
        opciones = parseOpcionesPairs(data.opt("opciones")),
        selected = data.opt("selected")?.toString().orEmpty(),
    )
}

fun postGuardarEncargoZona(
    client: OkHttpClient,
    baseUrl: String,
    idZona: String,
    idEnc: String,
    idTipoEnc: String,
    idUbi: String,
    encargo: String,
    orden: String,
    prioridad: String,
    descripcionLugar: String,
    idiomaEnc: String,
    observ: String,
): SrcMutationResult {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/misas/guardar_encargo_zona",
        mapOf(
            "id_zona" to idZona,
            "id_enc" to idEnc.ifEmpty { "0" },
            "id_tipo_enc" to idTipoEnc,
            "id_ubi" to idUbi,
            "encargo" to encargo,
            "orden" to orden,
            "prioridad" to prioridad,
            "descripcion_lugar" to descripcionLugar,
            "idioma_enc" to idiomaEnc,
            "observ" to observ,
        ),
    )
    return mutationResultFromEnvelope(body?.let { parseSrcEnvelope(it) })
}

fun postEliminarEncargoZona(
    client: OkHttpClient,
    baseUrl: String,
    idEnc: String,
): SrcMutationResult {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/misas/eliminar_encargo_zona",
        mapOf("id_enc" to idEnc),
    )
    return mutationResultFromEnvelope(body?.let { parseSrcEnvelope(it) })
}

fun postGuardarEncargoCentro(
    client: OkHttpClient,
    baseUrl: String,
    idItem: String,
    idEnc: String,
    idCtr: String,
): SrcMutationResult {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/misas/guardar_encargo_centro",
        mapOf(
            "id_item" to idItem,
            "id_enc" to idEnc,
            "id_ctr" to idCtr,
        ),
    )
    return mutationResultFromEnvelope(body?.let { parseSrcEnvelope(it) })
}

fun postEliminarEncargoCentro(
    client: OkHttpClient,
    baseUrl: String,
    idItem: String,
): SrcMutationResult {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/misas/eliminar_encargo_centro",
        mapOf("id_item" to idItem),
    )
    return mutationResultFromEnvelope(body?.let { parseSrcEnvelope(it) })
}

/** `opciones` como objeto `{id: label}` o lista `[[id, label], …]` (OpcionesDesplegable). */
private fun parseOpcionesPairs(raw: Any?): Map<String, String> {
    return when (raw) {
        is JSONObject -> jsonObjectToStringMap(raw)
        is JSONArray -> {
            val out = linkedMapOf<String, String>()
            for (i in 0 until raw.length()) {
                val pair = raw.optJSONArray(i) ?: continue
                if (pair.length() >= 2) {
                    out[pair.opt(0)?.toString().orEmpty()] = pair.opt(1)?.toString().orEmpty()
                }
            }
            out
        }
        else -> emptyMap()
    }
}

fun fetchVerInicialesZona(
    client: OkHttpClient,
    baseUrl: String,
    idZona: String,
): InicialesZonaGrid? {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/misas/ver_iniciales_zona_data",
        mapOf("id_zona" to idZona),
    ) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    return InicialesZonaGrid(rows = parseJsonRows(data.optJSONArray("rows")))
}

/** Cambia masivamente el estado del plan de misas de una zona/periodo. */
fun postNuevoStatus(
    client: OkHttpClient,
    baseUrl: String,
    idZona: String,
    periodo: String,
    estado: String,
    empiezaMin: String = "",
    empiezaMax: String = "",
): SrcMutationResult {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/misas/nuevo_status",
        mapOf(
            "id_zona" to idZona,
            "periodo" to periodo,
            "estado" to estado,
            "empiezamin" to empiezaMin,
            "empiezamax" to empiezaMax,
        ),
    )
    return mutationResultFromEnvelope(body?.let { parseSrcEnvelope(it) })
}

/** Inserta/actualiza iniciales y color de un sacerdote. Color: hex 6 chars sin `#`. */
fun postUpdateIniciales(
    client: OkHttpClient,
    baseUrl: String,
    idSacd: String,
    iniciales: String,
    color: String,
): SrcMutationResult {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/misas/update_iniciales",
        mapOf(
            "id_sacd" to idSacd,
            "iniciales" to iniciales,
            "color" to normalizeInicialesColor(color),
        ),
    )
    return mutationResultFromEnvelope(body?.let { parseSrcEnvelope(it) })
}

fun normalizeInicialesColor(raw: String): String {
    var s = raw.trim()
    if (s.startsWith("#")) s = s.drop(1)
    if (s.length == 3 && s.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
        s = "${s[0]}${s[0]}${s[1]}${s[1]}${s[2]}${s[2]}"
    }
    return if (s.length == 6 && s.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
        s.lowercase()
    } else {
        ""
    }
}

fun fetchDesplegableSacd(
    client: OkHttpClient,
    baseUrl: String,
    idZona: String,
    dia: String,
    idSacd: String = "0",
    seleccion: String = "2",
): DesplegableOpciones? {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/misas/desplegable_sacd",
        mapOf(
            "id_zona" to idZona,
            "dia" to dia,
            "id_sacd" to idSacd.ifEmpty { "0" },
            "seleccion" to seleccion,
        ),
    ) ?: return null
    val data = parseContestarDataObject(body) ?: return null
    val fromPairs = parseOpcionesPairs(data.opt("opciones"))
    val fromRows = parseValueLabelRows(data.optJSONArray("rows"))
    val opciones = fromPairs.ifEmpty { fromRows }
    return DesplegableOpciones(
        opciones = opciones,
        selected = data.opt("selected")?.toString().orEmpty(),
    )
}

fun postCuadriculaUpdate(
    client: OkHttpClient,
    baseUrl: String,
    uuidItem: String,
    key: String,
    idEnc: String,
    dia: String,
    tstart: String,
    tend: String,
    observ: String,
    tipoPlantilla: String,
    idZona: String,
): SrcMutationResult {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/misas/cuadricula_update",
        mapOf(
            "uuid_item" to uuidItem,
            "key" to key,
            "id_enc" to idEnc,
            "dia" to dia,
            "tstart" to tstart,
            "tend" to tend,
            "observ" to observ,
            "tipo_plantilla" to tipoPlantilla,
            "id_zona" to idZona,
        ),
    )
    return mutationResultFromEnvelope(body?.let { parseSrcEnvelope(it) })
}

data class CrearPeriodoResult(
    val ok: Boolean,
    val message: String,
    val grid: CuadriculaZona? = null,
)

fun postCrearNuevoPeriodo(
    client: OkHttpClient,
    baseUrl: String,
    idZona: String,
    tipoPlantilla: String,
    periodo: String,
    orden: String,
    empiezaMin: String = "",
    empiezaMax: String = "",
    seleccion: String = "0",
): CrearPeriodoResult {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/misas/crear_nuevo_periodo_data",
        mapOf(
            "id_zona" to idZona,
            "tipo_plantilla" to tipoPlantilla,
            "periodo" to periodo,
            "orden" to orden,
            "empiezamin" to empiezaMin,
            "empiezamax" to empiezaMax,
            "seleccion" to seleccion,
        ),
    )
    val env = body?.let { parseSrcEnvelope(it) }
    val mutation = mutationResultFromEnvelope(env)
    if (!mutation.ok) {
        return CrearPeriodoResult(ok = false, message = mutation.message)
    }
    val grid = env?.dataObject?.let { parseCuadriculaZona(it) }
    return CrearPeriodoResult(ok = true, message = "", grid = grid)
}

fun postImportarPlantilla(
    client: OkHttpClient,
    baseUrl: String,
    idZona: String,
    tipoOrigen: String,
    tipoDestino: String,
): SrcMutationResult {
    val body = postSrcForm(
        client,
        baseUrl,
        "/src/misas/importar_plantilla_data",
        mapOf(
            "id_zona" to idZona,
            "tipo_plantilla_origen" to tipoOrigen,
            "tipo_plantilla_destino" to tipoDestino,
        ),
    )
    return mutationResultFromEnvelope(body?.let { parseSrcEnvelope(it) })
}

/** En carga a veces viene `id_nom#iniciales`; al guardar debe ser `iniciales#id_nom`. */
fun normalizeSacdKeyForUpdate(key: String): String {
    val parts = key.split('#')
    if (parts.size == 2 && parts[0].all { it.isDigit() } && parts[0].isNotEmpty()) {
        return "${parts[1]}#${parts[0]}"
    }
    return key
}

fun idNomFromSacdKey(key: String): String {
    val parts = key.split('#')
    if (parts.size != 2) return "0"
    return when {
        parts[0].all { it.isDigit() } -> parts[0]
        parts[1].all { it.isDigit() } -> parts[1]
        else -> "0"
    }
}

fun ensureEncargoDiaUuid(uuid: String): String =
    uuid.ifBlank { java.util.UUID.randomUUID().toString() }

private fun parseValueLabelRows(arr: JSONArray?): Map<String, String> {
    if (arr == null) return emptyMap()
    val out = linkedMapOf<String, String>()
    for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        out[obj.opt("value")?.toString().orEmpty()] = obj.optString("label", "")
    }
    return out
}

private fun parseJsonRows(arr: JSONArray?): List<Map<String, String>> {
    if (arr == null) return emptyList()
    val out = mutableListOf<Map<String, String>>()
    for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        val map = linkedMapOf<String, String>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            map[k] = obj.opt(k)?.toString().orEmpty()
        }
        out.add(map)
    }
    return out
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

private fun jsonObjectToIntKeyStringMap(obj: JSONObject?): Map<String, String> {
    if (obj == null) return emptyMap()
    val out = linkedMapOf<String, String>()
    val keys = obj.keys()
    while (keys.hasNext()) {
        val k = keys.next()
        out[k] = obj.optString(k, k)
    }
    return out
}

private fun parseCuadriculaZona(data: JSONObject): CuadriculaZona {
    val rawRows = data.optJSONArray("data_cuadricula") ?: JSONArray()
    val parsedRows = mutableListOf<CuadriculaRow>()
    val dateColumns = linkedSetOf<String>()

    for (i in 0 until rawRows.length()) {
        val row = rawRows.optJSONObject(i) ?: continue
        val encargo = row.optString("encargo", "")
        val isTitle = row.optString("color_encargo", "") == "titulo"
        val cells = linkedMapOf<String, String>()
        val metaByDate = linkedMapOf<String, CuadriculaCellMeta>()
        val metaObj = when (val rawMeta = row.opt("meta")) {
            is JSONObject -> rawMeta
            is String -> runCatching { JSONObject(rawMeta) }.getOrNull()
            else -> null
        }
        if (metaObj != null) {
            val metaKeys = metaObj.keys()
            while (metaKeys.hasNext()) {
                val date = metaKeys.next()
                val cellMeta = metaObj.optJSONObject(date) ?: continue
                metaByDate[date] = CuadriculaCellMeta(
                    uuidItem = cellMeta.optString("uuid_item", ""),
                    key = cellMeta.optString("key", ""),
                    idEnc = cellMeta.opt("id_enc")?.toString().orEmpty(),
                    dia = cellMeta.optString("dia", date),
                    tstart = cellMeta.optString("tstart", ""),
                    tend = cellMeta.optString("tend", ""),
                    observ = cellMeta.optString("observ", ""),
                    tipo = cellMeta.optString("tipo", ""),
                    color = cellMeta.optString("color", ""),
                    texto = cellMeta.optString("texto", ""),
                )
                if (date.matches(DATE_COLUMN_REGEX)) {
                    dateColumns.add(date)
                }
            }
        }
        val keys = row.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key in IGNORED_ROW_KEYS) continue
            val value = row.opt(key)?.toString().orEmpty()
            cells[key] = value
            if (key.matches(DATE_COLUMN_REGEX)) {
                dateColumns.add(key)
            }
        }
        parsedRows.add(
            CuadriculaRow(
                encargo = encargo,
                isTitle = isTitle,
                cells = cells,
                metaByDate = metaByDate,
            ),
        )
    }

    return CuadriculaZona(
        dateColumns = dateColumns.sorted(),
        rows = parsedRows,
        preferenceWarning = data.optString("preference_warning").takeIf { it.isNotEmpty() },
    )
}

private val IGNORED_ROW_KEYS = setOf("encargo", "id_nom", "color_encargo", "meta")
private val DATE_COLUMN_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")
