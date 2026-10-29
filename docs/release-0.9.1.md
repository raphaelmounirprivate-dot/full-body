La séance affiche **un seul exercice à la fois** et le **chrono de repos reste fixé en bas de l’écran**, au-dessus de la navigation.

- Valider une nouvelle série enregistre les données et lance automatiquement le repos prévu.
- La série suivante apparaît ; après deux séries, ou le nombre configuré, l’exercice suivant s’affiche pendant le repos en cours.
- Pause/Reprendre, −15 s, +30 s et fermeture restent accessibles sans remonter la page.
- Une correction ne redémarre pas le repos. Le message de record apparaît au-dessus du chrono.
- Mode liste conservé pour consulter/corriger l’ensemble de la séance ; options avancées accessibles depuis l’exercice et bilan final conservé.

Le mode guidé et le repos automatique sont activés une seule fois à cette mise à jour. Les choix ultérieurs, réglages, variantes et anciennes séances sont conservés. Les autorisations de notification ne sont pas réactivées. Room v4, JSON 5/CSV 3 inchangés.

Cette mise à jour inclut aussi les changements préparés depuis 0.8.1 : **Rear Kick dans les nouvelles séances B**, historique séparé du hip thrust, repères des machines Technogym et **visuel F4 choisi par l’utilisateur**. Les séances déjà actives ou en pause gardent leurs mouvements. Un ancien APK ne connaît pas le nouveau mouvement R1.

Validation locale : **123 tests distincts réussis** (52 JVM, 58 Android, 13 contrôles Python), plus 11 tests rejoués à 130 %. L’APK optimisé signé a été testé séparément : chrono après défilement, passage après deux séries, grande police et thème sombre.

Sauvegarder en JSON puis installer par-dessus, sans désinstaller. Même clé de test ; Android 8.0 minimum. Validation sur émulateur Android 11, pas d’essai du téléphone physique revendiqué.

[Rapport de validation](https://github.com/raphaelmounirprivate-dot/full-body/blob/main/docs/validation-0.9.1.md) · [Feuille de route](https://github.com/raphaelmounirprivate-dot/full-body/blob/main/docs/ROADMAP.md).
