package be.moncoachsport

/** Personal club observations are separate from the immutable F1-F19 source guide. */
object ClubMachines {
    const val REAR_KICK = "R1"
    const val equipmentSummary = "Ta salle utilise majoritairement du matériel Technogym, selon ton indication. Les réglages restent propres à chaque poste."
    val rearKick = Exercise(
        id = REAR_KICK,
        name = "Rear Kick machine",
        sets = 2,
        min = 10,
        max = 15,
        rest = 90,
        unit = "rép / jambe",
        muscles = "Fessiers ; ischio-jambiers et quadriceps en assistance",
        setup = "Fais montrer les appuis et les réglages de ce poste. Pied de travail sur la plateforme mobile, autre jambe stable. Note la machine, ses réglages et le mode de charge utilisé.",
        accessory = "Repère de ta salle : plaque REAR KICK photographiée le 29 octobre 2026. Modèle exact non confirmé.",
        instructions = "Pousse la plateforme vers l’arrière avec une jambe, puis reviens lentement et avec contrôle. Fais ensuite l’autre jambe.",
        image = "",
        check = "Garde le bassin stable, sans le tourner ni cambrer pour gagner de l’amplitude. Valide une série après les deux jambes ; saisis les répétitions et la réserve du côté le plus limité.",
        caption = "Adaptation personnelle en séance B : remplace le hip thrust, sans ajouter de mouvement. Deux séries de 10–15 répétitions par jambe ; 90 s après les deux jambes, à ajuster selon la récupération.",
        warning = "Mouvement distinct du hip thrust : réglages, charges et historique séparés. Ne reporte pas sa charge sur le Rear Kick. La photo identifie le nom du poste, pas ses réglages ni sa résistance initiale.",
    )

    private val notes = mapOf(
        "F2" to "Repère de ta salle : CHEST PRESS Technogym, plaque photographiée le 29 octobre 2026. Fais régler le siège et la position de départ sur le poste réel.",
        "F9" to "Repère de ta salle : LOW ROW Technogym, plaque photographiée le 29 octobre 2026. À rapprocher du rowing machine avec appui ; ne le confonds pas avec le rowing à la poulie F14. Fais confirmer les appuis et réglages.",
    )

    fun catalogue(guide: List<Exercise>): List<Exercise> =
        guide.map { exercise -> notes[exercise.id]?.let { exercise.copy(accessory = it) } ?: exercise } + rearKick
}
