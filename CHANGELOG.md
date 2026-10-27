# Historique des versions

## 0.6.0 — 27 octobre 2026

- Lecture tolérante aux lignes de séance invalides, avertissement et export JSON des données brutes de récupération.
- Messages de validation en français et validation des imports avec le catalogue propre à chaque programme.
- Suppression de l’ancienne règle de progression inutilisée ; tests centrés sur `Programs.canIncrease`.
- Room 2→3 : brouillons enregistrés champ par champ, lecture transactionnelle et cache de décodage des lignes inchangées.
- Compte à rebours isolé ; découpage des écrans hors de `MainActivity.kt`.
- Copies PDF mobiles compressées ; originaux et empreintes conservés dans les sources. Images historiques converties en WebP. R8 et réduction des ressources activés pour release.
- Bouton de nouveau cycle après huit semaines calendaires, avec conservation de l’historique.
- Protection CSV étendue aux préfixes tabulation et retour chariot ; CI JVM/lint sur GitHub.
- Nettoyage des archives d’outils extraites et des anciens APK/AAB, avec conservation des sources et de la référence 0.5.0.

## 0.5.0 — 26 octobre 2026

- Choix A/B/C accessible avec « Changer » en haut de la séance, ou « Choisir A, B ou C » avant de commencer.
- Mise en pause et reprise sans terminer artificiellement une séance ni perdre ses séries ou brouillons. Une seule séance active ; changement enregistré dans une transaction Room.
- Arrêt du repos au changement ; valeurs de saisie séparées entre séances, y compris pour un exercice commun.
- Rotation et derniers passages fondés sur la date de fin réelle pour tenir compte des reprises.
- Sauvegardes JSON et CSV au format 3 pour conserver les pauses. Import des anciens JSON 1/2 et CSV format 2 conservé ; Room reste en version 2.

## 0.4.0 — 26 octobre 2026

- Nouvelle présentation de la séance : bandeau bleu nuit avec avancement réel, illustrations et exercices dépliables.
- Échauffement accessible à la demande ; bouton de validation bleu et saisie centrée sur la prochaine série.
- Séries enregistrées résumées sur une ligne et modifiables. Brouillons conservés quand on change d’exercice.
- Programmes, prescriptions, historique et formats de sauvegarde conservés.

## 0.3.1 — 26 octobre 2026

- Retrait du bloc « À propos du guide » de l’accueil.
- Conservation du bouton permettant d’enregistrer le PDF complet.
- Compilation APK/AAB, 22 tests JVM et 8 tests UI réussis ; lint sans erreur bloquante.

## 0.3.0

- Correction de la source : guide illustré de 31 pages, avec 19 fiches F1–F19.
- Intégration des illustrations, consignes et remplacements conditionnels du guide.
- Présentation bleu et bleu nuit inspirée de la maquette fournie, catalogue avec vignettes et filtres musculaires.
- Conservation du programme initial pour la relecture des anciennes séances.

## 0.2

- Cycle de huit semaines A/B/C, options et paramètres conservés par séance.
- Bilans hebdomadaires, migration Room 1 vers 2 et sauvegardes enrichies.
- Import JSON compatible avec les anciennes sauvegardes et CSV restaurable.

## 0.1

- Première base native Android hors ligne : séances, séries, charges, repos et historique.
- Programme illustré initial conservé comme référence des séances de cette version.
