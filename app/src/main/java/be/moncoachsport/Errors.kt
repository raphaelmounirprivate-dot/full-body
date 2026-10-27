package be.moncoachsport

import org.json.JSONException

fun frenchError(error: Throwable): String =
    when (error) {
        is JSONException ->
            "Document JSON illisible : un champ obligatoire est absent ou mal formé."
        is NumberFormatException -> "Une valeur numérique du fichier est invalide."
        is IllegalArgumentException ->
            error.message?.takeUnless {
                it == "Failed requirement." || it.startsWith("For input string")
            } ?: "Les données ne respectent pas le format attendu."
        else -> "L'opération n'a pas pu aboutir. Vos données existantes sont conservées."
    }
