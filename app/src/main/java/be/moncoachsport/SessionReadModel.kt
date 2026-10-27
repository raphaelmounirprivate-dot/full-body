package be.moncoachsport

data class UnreadableSession(val id: String, val payload: String, val reason: String)

data class SessionReadModel(
    val sessions: List<Training> = emptyList(),
    val unreadable: List<UnreadableSession> = emptyList(),
    val rows: List<SessionRecord> = emptyList(),
    val draftRows: List<DraftRecord> = emptyList(),
    val loaded: Boolean = false,
)

/**
 * Cache par contenu : une frappe ne revalide pas tout l'historique. Les échecs sont conservés
 * aussi.
 */
class SessionRowDecoder(private val decode: (String) -> Training = Codec::decode) {
    private val cache = mutableMapOf<String, Pair<String, Result<Training>>>()

    @Synchronized
    fun read(row: SessionRecord): Result<Training> {
        val cached = cache[row.id]
        if (cached?.first == row.payload) return cached.second
        val result = runCatching {
            decode(row.payload).also {
                require(it.id == row.id) {
                    "L'identifiant de la ligne diffère de celui de la séance"
                }
            }
        }
        cache[row.id] = row.payload to result
        return result
    }

    @Synchronized
    fun readAll(
        rows: List<SessionRecord>,
        drafts: List<DraftRecord> = emptyList(),
    ): SessionReadModel {
        cache.keys.retainAll(rows.map { it.id }.toSet())
        val rejected = mutableListOf<UnreadableSession>()
        val readable =
            rows.mapNotNull { row ->
                read(row)
                    .fold(
                        onSuccess = { it },
                        onFailure = {
                            rejected +=
                                UnreadableSession(
                                    row.id,
                                    row.payload,
                                    it.message ?: "Séance illisible",
                                )
                            null
                        },
                    )
            }
        val valid = mutableListOf<Training>()
        val pending = mutableSetOf<Pair<String, String>>()
        var hasActive = false
        val byId = rows.associateBy { it.id }
        val draftMap = drafts.groupBy { it.sessionId }
        readable
            .sortedWith(
                compareByDescending<Training> { it.isActive }.thenBy { it.started }.thenBy { it.id }
            )
            .forEach { t ->
                val reason =
                    when {
                        t.isActive && hasActive ->
                            "Plusieurs séances actives dans les données locales"
                        t.finished == null && (t.program to t.template) in pending ->
                            "Plusieurs séances non terminées du même type"
                        else -> null
                    }
                if (reason != null)
                    rejected += UnreadableSession(t.id, byId.getValue(t.id).payload, reason)
                else {
                    if (t.isActive) hasActive = true
                    if (t.finished == null) pending += t.program to t.template
                    val overrides = draftMap[t.id].orEmpty().associate { it.field to it.value }
                    valid += if (overrides.isEmpty()) t else t.copy(drafts = t.drafts + overrides)
                }
            }
        return SessionReadModel(
            valid.sortedBy { it.started },
            rejected,
            rows,
            drafts,
            loaded = true,
        )
    }
}
