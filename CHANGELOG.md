# Historique des versions

## 0.9.1 — Séance guidée et chrono fixe (29 octobre 2026, local)

- Un exercice et la prochaine série à la fois par défaut ; passage au mouvement suivant après les séries prévues, avec respect d’une troisième série configurée.
- Chrono fixé en bas de la séance, avec Pause/Reprendre et −15 s / +30 s ; reste visible pendant le défilement en mode guidé ou liste.
- Repos automatique après une nouvelle validation réussie ; la correction d’une ancienne série ne redémarre pas le repos.
- Activation unique du mode guidé et du repos automatique à la mise à jour ; choix ultérieurs et autres préférences conservés.
- Options avancées repliables, bilan final et mode liste conservés. Les records s’affichent au-dessus du chrono.
- Fermeture/pause protégées contre une commande visant une ancienne échéance ; défilement unique pour éviter un plantage de première mise en page.

## 0.9.0 — Rear Kick et repères de la salle (29 octobre 2026, local)

- Rear Kick R1 à la place du hip thrust F11 dans les nouvelles séances B, avec retour possible au hip thrust dans les préférences.
- Fiche textuelle personnelle distincte du PDF, séries par jambe et validation après les deux côtés ; charges, vidéos et records séparés.
- Anciennes séances et pauses inchangées ; import/export du nouveau mouvement en JSON et CSV.
- Repères Chest Press et Low Row Technogym issus des photos ; majorité de matériel Technogym signalée, modèles et réglages non supposés.
- Visuel F4 choisi par l’utilisateur conservé.

## 0.8.2 — Leg curl assis sur machine (28 octobre 2026)

- Correction de la fiche F4 : machine de leg curl assis au lieu du schéma sur chaise, avec le même personnage et le même style.
- Nouvelle illustration et légende appliquées aux vignettes, à la fiche et au zoom ; provenance de la correction explicitée.
- PDF original, empreintes, données extraites et historique conservés. Aucune modification du programme ni des formats de sauvegarde.

## 0.8.1 — Adaptations machines et poulies (28 octobre 2026)

- Choix explicites des variantes existantes de poussée et de tronc pour les prochaines nouvelles séances ; anciennes séances conservées.
- Bilan réel des sept derniers jours du cycle et confirmation de maîtrise/récupération avant une troisième série.
- Correction de la comparaison de progression : deux prescriptions comparables peuvent être séparées par une autre plage de répétitions.
- Préférences facultatives sauvegardées en JSON 5, sans migration Room ni changement de CSV.
- Analyse et sources dans `docs/programme-adapte.md`. Les essais effectués sont détaillés dans `docs/validation-0.8.1.md`.

## 0.8.0 — 27 octobre 2026

- Notification silencieuse avec chronomètre natif en décompte pendant le repos ; annulation à la pause et à la fin.
- Carte « Repos terminé ✓ », vibration de fin selon les réglages, suppression du bouton Pause à zéro et fermeture après trois secondes visibles.
- +30 s / −15 s sur le minuteur actif ou en pause, avec stockage atomique de l’ajustement et protection contre la fermeture d’un nouveau repos par une ancienne échéance.
- Records personnels comparables, signalés après enregistrement et visibles sur les séries ; durée séparée de la charge pour la planche.
- Widget Android natif, redimensionnable, avec séance, objectif hebdomadaire, actualisation et retour à la séance existante.
- Room v4 et formats JSON 5 / CSV 3 conservés : les records et le widget sont dérivés des données existantes.

## 0.7.0 — 27 octobre 2026

- Alarmes de repos exactes si autorisées, repli explicite, vérification de l’échéance persistée, annulation des événements obsolètes et réarmement.
- Écran allumé configurable et mode guidé : série suivante, exercice suivant puis bilan, uniquement après validation enregistrée.
- Mesures corporelles manuelles datées, courbes, modification et suppression ; migration Room 3→4 sans perte des séances/brouillons.
- Rappel facultatif de fin de cycle, profil et repères de niveau personnels par exercice.
- Vidéos YouTube personnelles par programme/exercice ; neuf liens par défaut documentés, recherche et choix personnel sur les autres fiches.
- Cartes d’accueil réorganisables et masquables ; objectif hebdomadaire à points facultatif.
- JSON complet format 5, anciens imports conservés, récupération brute format 4 inchangée. Feuille de route explicite dans docs/ROADMAP.md.

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
