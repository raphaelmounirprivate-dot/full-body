Le repos est visible dans une notification silencieuse avec compte à rebours natif Android, y compris sur l’écran verrouillé selon ses réglages. Les boutons +30 s / −15 s ajustent le minuteur, même en pause.

À la fin, la carte affiche « Repos terminé ✓ », propose de fermer et disparaît après trois secondes visibles. Si les rappels sont activés et l’application visible, la vibration de fin est dédoublonnée avec la notification système.

Autres ajouts :

- Records personnels après enregistrement et badges sur les séries, à programme, exercice, machine, réglages et unité comparables. Répétitions à charge identique ; durée pour les exercices chronométrés. Renseigner « Machine et réglages » pour ces comparaisons.
- Vrai widget Android pour la séance à reprendre ou la prochaine séance A/B/C et l’objectif hebdomadaire, avec actualisation et accès à la séance. Ajouter depuis l’accueil de l’application ou le sélecteur Widgets du téléphone.

Validation locale : 40 tests JVM, 47 tests Android et 12 contrôles Python, soit 99 tests distincts ; six parcours d’interface rejoués à 130 % de texte. Tests instrumentés sur debug et émulateur Android 11 ; essai du paquet optimisé signé séparé. Les permissions et réglages de notification, vibrations réelles et lanceur du téléphone physique restent à vérifier.

Sauvegarder en JSON puis installer l’APK **par-dessus sans désinstaller**. Même clé de test que les versions précédentes. Room v4 et formats JSON 5 / CSV 3 conservés ; aucune migration des séances, aucune hausse de charge automatique. Aucun compte, télémétrie ou permission Internet.

[Rapport](https://github.com/raphaelmounirprivate-dot/full-body/blob/main/docs/validation-0.8.0.md) · [Feuille de route](https://github.com/raphaelmounirprivate-dot/full-body/blob/main/docs/ROADMAP.md).
