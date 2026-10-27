package be.moncoachsport

import org.json.JSONArray
import org.json.JSONObject

/** CSV de l'historique : champs lisibles et état complet de séance pour la reprise. */
object HistoryCsv {
    private val header =
        listOf(
            "version",
            "session",
            "programme",
            "début",
            "fin",
            "séance",
            "exercice",
            "série",
            "kg",
            "unité_charge",
            "valeur",
            "RIR",
            "machine",
            "réglage",
            "terminée",
            "état_séance_json",
        )

    private fun dangerous(s: String) =
        s.firstOrNull() in listOf('\t', '\r', '\n') ||
            s.trimStart().firstOrNull() in listOf('=', '+', '-', '@')

    private fun cell(value: String): String {
        val safe = if (dangerous(value) || value.startsWith("'")) "'$value" else value
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }

    private fun original(value: String): String =
        if (value.startsWith("'") && (value.drop(1).startsWith("'") || dangerous(value.drop(1))))
            value.drop(1)
        else value

    fun encode(sessions: List<Training>): String {
        val rows =
            sessions.flatMap { t ->
                val metadata = Codec.encode(t.copy(sets = emptyList()))
                (t.sets.map { it as LoggedSet? }.ifEmpty { listOf(null) }).map { s ->
                    listOf(
                        "3",
                        t.id,
                        t.program,
                        t.started.toString(),
                        t.finished?.toString() ?: "",
                        t.template,
                        s?.exerciseId ?: "",
                        s?.index?.plus(1)?.toString() ?: "",
                        s?.kg?.toString() ?: "",
                        s?.loadUnit ?: "",
                        s?.reps?.toString() ?: "",
                        s?.rir?.toString() ?: "",
                        s?.machine ?: "",
                        s?.setting ?: "",
                        s?.done?.toString() ?: "",
                        metadata,
                    )
                }
            }
        return (listOf(header) + rows).joinToString("\r\n") {
            it.joinToString(";", transform = ::cell)
        }
    }

    fun decode(raw: String): List<Training> =
        try {
            decodeRows(raw)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Une valeur numérique du CSV est invalide.", e)
        }

    private fun decodeRows(raw: String): List<Training> {
        require(raw.length <= 5_000_000) { "Fichier trop volumineux" }
        val rows = parse(raw.removePrefix("\uFEFF"))
        require(rows.isNotEmpty() && rows.first() == header) {
            "CSV non reconnu : utilisez un export de Mon Coach Sport v0.2 ou ultérieure."
        }
        val data =
            rows.drop(1).map { row ->
                require(row.size == header.size && row[0] in listOf("2", "3")) {
                    "Ligne CSV invalide"
                }
                val r = row.map(::original)
                val t = Codec.decode(r[15])
                require(row[0] == "3" || !t.paused) { "Les pauses nécessitent un CSV au format 3" }
                require(t.sets.isEmpty()) {
                    "Les séries CSV doivent être dans leurs colonnes, pas dans les métadonnées"
                }
                require(
                    r[1] == t.id &&
                        r[2] == t.program &&
                        r[3] == t.started.toString() &&
                        r[4] == (t.finished?.toString() ?: "") &&
                        r[5] == t.template
                ) {
                    "Métadonnées CSV incohérentes"
                }
                val set =
                    if (r[6].isBlank()) {
                        require(r.subList(7, 15).all { it.isBlank() }) {
                            "Une ligne sans exercice contient des valeurs de série"
                        }
                        null
                    } else
                        LoggedSet(
                                r[6],
                                r[7].toInt() - 1,
                                r[8].toDouble(),
                                r[10].toInt(),
                                r[11].toInt(),
                                r[12],
                                r[13],
                                done =
                                    r[14].let {
                                        require(it in listOf("true", "false")) {
                                            "Le statut de validation CSV doit être true ou false"
                                        }
                                        it.toBoolean()
                                    },
                                loadUnit = r[9],
                            )
                            .also {
                                require(Rules.validate(it)) {
                                    "Une série CSV contient une charge, une valeur, un RIR ou une unité invalide"
                                }
                            }
                Triple(t, set, r[15])
            }
        return data
            .groupBy { it.first.id }
            .values
            .map { group ->
                require(group.map { it.third }.distinct().size == 1) {
                    "Séance dupliquée incohérente"
                }
                require(group.size == 1 || group.all { it.second != null }) {
                    "Une séance CSV mélange des lignes vides et des séries"
                }
                Codec.decode(
                    Codec.encode(group.first().first.copy(sets = group.mapNotNull { it.second }))
                )
            }
    }

    fun asBackup(sessions: List<Training>) =
        JSONObject()
            .put("version", 3)
            .put(
                "sessions",
                JSONArray().apply { sessions.forEach { put(JSONObject(Codec.encode(it))) } },
            )

    /** RFC 4180 avec séparateur point-virgule ; refuse les guillemets mal fermés. */
    fun parse(raw: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var closed = false
        var i = 0
        fun endField() {
            row += field.toString()
            field.setLength(0)
            closed = false
        }
        while (i < raw.length) {
            val c = raw[i]
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < raw.length && raw[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        quoted = false
                        closed = true
                    }
                } else field.append(c)
            } else
                when (c) {
                    '"' -> {
                        require(field.isEmpty() && !closed) { "Guillemet CSV invalide" }
                        quoted = true
                    }
                    ';' -> endField()
                    '\r',
                    '\n' -> {
                        endField()
                        rows += row.toList()
                        row.clear()
                        if (c == '\r' && i + 1 < raw.length && raw[i + 1] == '\n') i++
                    }
                    else -> {
                        require(!closed) { "Texte après un champ CSV fermé" }
                        field.append(c)
                    }
                }
            i++
        }
        require(!quoted) { "Guillemet CSV non fermé" }
        if (field.isNotEmpty() || closed || row.isNotEmpty()) {
            endField()
            rows += row.toList()
        }
        return rows
    }
}
