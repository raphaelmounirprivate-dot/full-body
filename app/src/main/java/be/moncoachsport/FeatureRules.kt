package be.moncoachsport

import java.net.URI
import java.time.*
import java.time.temporal.TemporalAdjusters

object FeatureRules {
    val levels = listOf("Débutant", "Avancé", "Confirmé")
    val homeCards = listOf("stats", "shortcuts", "last", "challenge")
    fun validate(o: PersonalOptions) {
        require(o.level in levels) { "Niveau inconnu" }
        require(o.homeOrder.size == homeCards.size && o.homeOrder.toSet() == homeCards.toSet()) { "Organisation de l’accueil invalide" }
        require(o.hidden.all { it in homeCards }) { "Carte d’accueil inconnue" }
        require(o.weeklyGoal in 1..7) { "Objectif : de 1 à 7 séances par semaine" }
        require(o.exerciseLevels.size <= 38 && o.exerciseLevels.all { (id, level) ->
            id.matches(Regex("(cycle|legacy):F([1-9]|1[0-9])")) && level in levels
        }) { "Repère de niveau d’exercice invalide" }
        require(o.videos.size <= 38 && o.videos.all { (id, url) ->
            id.matches(Regex("(cycle|legacy):F([1-9]|1[0-9])")) && validYoutube(url)
        }) { "Lien vidéo YouTube invalide" }
    }
    fun validYoutube(url: String): Boolean = runCatching {
        val u = URI(url)
        u.scheme == "https" && u.userInfo == null && u.port == -1 &&
            when (u.host?.lowercase()) {
                "youtu.be" -> u.path.matches(Regex("/[A-Za-z0-9_-]{11}"))
                "www.youtube.com", "youtube.com", "m.youtube.com" ->
                    (u.path == "/watch" && u.rawQuery.orEmpty().split("&").any { it.matches(Regex("v=[A-Za-z0-9_-]{11}")) }) ||
                        u.path.matches(Regex("/shorts/[A-Za-z0-9_-]{11}"))
                else -> false
            }
    }.getOrDefault(false)
    fun validate(m: BodyMeasurement, today: LocalDate = LocalDate.now()) {
        val day = runCatching { LocalDate.parse(m.day) }.getOrNull()
        require(day != null && day.toString() == m.day && !day.isAfter(today) && day.year >= 1900) { "Date de mesure invalide" }
        require(m.kg != null || m.fat != null) { "Renseignez le poids ou la masse grasse" }
        require(m.kg == null || (m.kg.isFinite() && m.kg in 1.0..700.0)) { "Poids : de 1 à 700 kg" }
        require(m.fat == null || (m.fat.isFinite() && m.fat in 0.1..75.0)) { "Masse grasse : de 0,1 à 75 %" }
    }
    fun weeklyCount(sessions: List<Training>, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Int {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return sessions.distinctBy { it.id }.count { t -> t.finished?.let {
            val day = Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
            !day.isBefore(monday) && !day.isAfter(today)
        } ?: false }
    }
    /** First unvalidated required set, honoring variants and optional skipped exercises. */
    fun nextSet(t: Training, catalogue: List<Exercise>): Pair<String, Int>? =
        Programs.plan(t, catalogue).firstNotNullOfOrNull { p ->
            val e = Programs.exercise(p, t, catalogue)
            if (e.id in t.skipped) null else (0 until e.sets).firstOrNull { index ->
                t.sets.none { it.exerciseId == e.id && it.index == index && it.done }
            }?.let { p.id to it }
        }
}
