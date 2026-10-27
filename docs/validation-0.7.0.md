# Validation 0.7.0

Vérification locale du 27 octobre 2026, versionCode 9. Travail issu de la liste des améliorations de séance et de suivi personnel. Les documents de programme restent des sources de contenu.

## Résultats exécutés

| Contrôle | Résultat |
|---|---|
| JVM | 30 tests, aucun échec : règles historiques et progression, prochaine série, variantes/options, dates/mesures, liens vidéo, organisation et comptage hebdomadaire |
| Android debug | 40 tests, aucun échec : anciennes régressions, migrations jusqu’à Room 4, sauvegardes, nouvelles interfaces et notifications |
| Contenu Python | 4 contrôles historiques + 8 contrôles du guide réussis |
| Compilation | APK debug, APK optimisé R8 et AAB non signé construits |
| Lint debug | 0 erreur, 19 avertissements, 2 suggestions ; mises à jour de dépendances et conseils d’API, sans montée de version non nécessaire |

Total local : **82 tests réussis**. Preuves : [build](test-results/features-build-0.7.0.txt), [Android](test-results/features-android-0.7.0.txt), [JVM](test-results/features-jvm-0.7.0.json), [contenu](test-results/features-content-0.7.0.txt), [guide](test-results/features-guide-0.7.0.txt).

Les trois nouveaux parcours d’interface ont aussi été rejoués avec une police à 130 % : mode guidé, maintien de l’écran et saisie d’une mesure, tous réussis. [Trace de ce second passage](test-results/features-font130-0.7.0.txt). Il ne s’agit pas de trois tests distincts supplémentaires.

Le test réel du repos programme deux échéances successives de quatre secondes, coupe l’écran, force la veille Android et attend la notification système pour chacune. Le test ne déclenche pas lui-même le récepteur. Le dépôt conserve aussi un test distinct du récepteur et des événements annulés/dupliqués. Ces essais s’exécutent sur l’émulateur dédié **Android 11/API 30**, pas sur le téléphone personnel.

Une première assertion de notification immédiate a échoué parce que sa publication système est asynchrone ; l’attente est désormais bornée sur la présence effective de la notification. Le dernier passage complet est réussi.

Le contrôle d’intent exécuté dans ActivityScenario empêchait sa fermeture correcte dans ce banc de test ; il a été séparé du test Compose et déplacé vers la vérification système du paquet optimisé. Les 40 tests puis les trois parcours à 130 % ont été relancés et réussis après cette séparation.

## Portée des changements

- Repos précis par alarme utilisateur Android visible si autorisée, afin de couvrir des pauses rapprochées même en veille. Repli non exact, autorisation explicite, notifications françaises, retour à la séance.
- Jeton d’échéance persisté, contrôle du minuteur et de la préférence de notification dans le récepteur, événements annulés/périmés/dupliqués rejetés.
- Le retour depuis la notification utilise `CLEAR_TOP | SINGLE_TOP` et `onNewIntent` pour réutiliser l’activité existante. Ce retour est vérifié hors ActivityScenario dans l’essai du paquet optimisé.
- Réarmement après boot, mise à jour, changement d’heure et octroi de permission ; un repos expiré n’est pas rejoué.
- Écran allumé uniquement dans l’écran de séance actif ; option désactivable et retrait à la navigation.
- Mode guidé dérivé des séries persistées : progression après écriture, prise en compte des variantes/séries supplémentaires/options sautées, retour au mode complet pour correction ou fin anticipée.
- Mesures corporelles en table séparée, modification par date, suppression confirmée, courbes et historique paginé. Aucune formule de masse grasse.
- Niveau personnel et lien vidéo par programme/exercice ; neuf liens par défaut documentés dans [les sources](video-sources.md).
- Cartes d’accueil déplaçables et masquables avec boutons accessibles ; pas de geste de glisser obligatoire.
- Objectif hebdomadaire facultatif, calculé sur les séances terminées et leur date de fin locale, dix points par identifiant de séance unique.

## Conservation

Migration explicite Room 3→4, sans modification des tables de séances et de brouillons. Les tests de migration 1→2→3→4 et 2→3→4 restent exécutés ; un nouveau test vérifie aussi 3→4 avec un brouillon.

JSON 5 exporte les mesures et préférences nouvelles. JSON 1/2/3 et CSV 2/3 restent acceptés. Un ancien fichier conserve les mesures/options nouvelles existantes ; une sauvegarde JSON 5 valide les remplace. La récupération brute avec lignes illisibles reste en format 4, refusé à l’import automatique. Le rappel de cycle est désactivé lors d’une restauration de nouvelles préférences, les permissions Android ne sont pas importées.

Les nouveaux champs sont validés avant remplacement transactionnel Room. Comme auparavant, Room et DataStore restent deux stockages distincts ; leur mise à jour ne constitue pas une transaction commune. Le CSV concerne les séances, le JSON est nécessaire pour une sauvegarde complète.

## Limites et tâches explicites

La liste maintenue se trouve dans [ROADMAP.md](ROADMAP.md). Restent à vérifier sur téléphone physique Android 13+ : autorisation de notification, autorisation d’alarmes exactes refusée/accordée/révoquée, redémarrage, veille longue et économie d’énergie constructeur. Un arrêt forcé de l’application peut empêcher ses rappels.

Les dix vidéos sans sélection éditoriale disposent de la recherche YouTube et de l’enregistrement d’un lien personnel ; aucune recherche n’est qualifiée de vidéo validée. Les neuf démonstrations externes ne certifient pas le modèle de machine disponible au club. Les niveaux sont des repères personnels, pas une nouvelle prescription sportive.

Les challenges de course, meilleur score et compétition restent ultérieurs. Aucun compte, GPS, classement distant, télémétrie, téléchargement vidéo ni permission Internet ajouté.

Les tests Android instrumentés portent sur debug. La vérification du paquet optimisé est distincte, consignée dans `optimized-smoke-0.7.0.json`. La signature reste celle de test des versions précédentes.

Le paquet optimisé final fait **6 753 800 octets**. Son empreinte installée a été comparée au fichier livré : `e4111080d517664e29c019dfa9950ef4ab379a0a44dd1ef062aef3d3f0088716`. Démarrage, mode guidé, fenêtre des mesures et retour par intent dans l’activité déjà ouverte vérifiés, sans exception fatale du processus. Captures : [séance guidée](screenshots/seance-guidee-0.7.0.png), [mesures](screenshots/mesures-0.7.0.png).
