# Audit corrigé — version 0.6.0, 27 octobre 2026

## Corrections

| Point | Réalisation |
| --- | --- |
| Séance illisible bloquant le démarrage | Décodage par ligne, cache des résultats et des échecs, lignes conservées dans la base et exclues des séances utilisables. Avertissement permanent avec export des données brutes. Activation et lecture d’autres séances possibles. |
| Erreurs en anglais | Messages explicites sur les préconditions de Codec, des imports et des actions ; erreurs JSON, numériques et d’accès traduites en messages français. Validation complète avant remplacement transactionnel. |
| Mauvais catalogue | `exercisesFor(t.program)` pour saisie, options, calcul des emplacements, suggestions et courbes. L’import reçoit les identifiants par programme et refuse un exercice autorisé seulement dans l’autre catalogue. |
| Deux règles de progression | Suppression de `Rules.canIncrease` et de ses tests devenus trompeurs. `Programs.canIncrease` est la seule règle utilisée et testée. Aucune hausse suggérée si des lignes illisibles empêchent de garantir l’historique complet. |
| APK lourd | Originaux PDF inchangés dans `docs/sources/`. Copies mobiles avec texte, pages et dimensions identiques ; images historiques WebP. R8 et réduction des ressources activés pour release. APK optimisé environ 6,7 Mo ; debug environ 16,7 Mo. |
| MainActivity monolithique | 102 lignes ; écrans, éditeur de série, bilan, catalogue et composants communs séparés. |
| Performances de séance | Brouillons dans une table dédiée, migration 2→3 non destructive et lecture transactionnelle avec leurs séances. Pas de réécriture du JSON à chaque frappe. Cache du décodage des payloads inchangés. Compte à rebours isolé ; plan et progression mémorisés. |
| Nouveau cycle | Bouton « Recommencer en semaine 1 » après huit semaines calendaires, confirmation et conservation de l’historique ; les séances non terminées du cycle doivent d’abord être reprises et terminées. |
| CSV | Protection des débuts `= + - @`, tabulation, retour chariot et saut de ligne. Test de conservation exacte après réimport. |
| CI | Workflow `.github/workflows/android.yml` : tests JVM et lint sur push/PR, permissions de lecture, actions officielles épinglées par SHA. Résultats et rapports dans GitHub Actions. |
| Nettoyage | 1 332 624 354 octets d’archives d’outils extraits et d’anciens APK/AAB supprimés. Archives source des anciennes versions et référence 0.5.0 conservées. Aucun outil extrait ni donnée d’entraînement personnelle supprimé. |

## Récupération et compatibilité

Les sauvegardes ordinaires restent en JSON/CSV format 3. JSON 1/2 et CSV format 2 restent acceptés. En présence de données illisibles, l’export JSON utilise le format **4 de récupération** : il contient les séances lisibles et un bloc `recovery` avec toutes les lignes et tous les brouillons bruts. Ce format est volontairement refusé par les anciens APK ; il évite qu’un ancien importeur ne restaure les seules lignes lisibles en ignorant les autres. L’import automatique est également refusé par 0.6.0 : le fichier doit être examiné et réparé. Un import ordinaire valide remplace les données après confirmation ; exporter d’abord si des lignes sont en attente de récupération.

Room reste compatible via les migrations explicites 1→2→3. Les anciens brouillons intégrés au JSON sont lus et conservés. Une ancienne séance garde son programme, sa semaine, son unité et ses options. Une rétrogradation directe de la base v3 vers un ancien APK v2 n’est pas prise en charge.

## Vérifications locales

- 23 tests JVM réussis : 19 tests de programme/progression et 4 tests de règles simples/calendrier.
- 33 tests Android réussis sur **Sport_QA, port 5556** : 6 tests de maintenance, 15 de stockage et 12 de parcours UI.
- 12 tests Python réussis : 4 sur l’archive historique et 8 sur le guide et les PDF mobiles.
- Rendu comparé de toutes les **39 pages** ; texte extrait et dimensions de page identiques, planches contact inspectées. Écart moyen maximal de pixels : 0,486/255 pour le guide et 0,272/255 pour l’archive historique. Ce contrôle complète la comparaison textuelle ; il ne remplace pas une validation sportive du document.
- Build debug, release optimisée et AAB réussi ; lint : **0 erreur, 13 avertissements et 1 indication**. Les avertissements de dépendances ne sont pas annoncés comme corrigés.

Les nouveaux tests vérifient le démarrage du ViewModel avec une séance corrompue, l’export de son payload exact, la poursuite en séance B, la migration v2→v3 sans changement de l’ancien payload, les écritures séparées de brouillons, leur reprise, le cache des lignes inchangées, les catalogues divergents et le rejet d’imports invalides avant effacement. Un cas de préférences mal formées vérifie aussi que le remplacement Room n’a pas encore eu lieu au moment du refus.

Les essais ont mis en évidence deux problèmes corrigés avant livraison : assembler deux flux séparés de séances/brouillons pouvait afficher un état intermédiaire, d’où la lecture transactionnelle ; le nettoyage des données dans le test UI pouvait être considéré fini avant le premier état Room réellement chargé, d’où une attente explicite de cet état. Les tests attendent également le résultat des écritures asynchrones de saut/réintégration d’exercice.

## Essai du paquet optimisé

Base Room v2 issue de l’application 0.5.0 : une série validée à 25 kg et un brouillon à 27 kg. Ajout volontaire d’une ligne JSON illisible, uniquement sur l’émulateur QA. Installation par-dessus avec la même signature de test : démarrage réussi, avertissement visible, série et brouillon conservés, changement A/B/A fonctionnel. Export réel par le sélecteur de fichiers Android : le fichier contient les valeurs attendues et le payload corrompu exact. Preuve : `optimized-upgrade-validation.json` ; captures : `screenshots/recuperation-0.6.0.png` et `screenshots/migration-0.6.0.png`.

Les 33 tests instrumentés portent sur le build debug. L’essai du paquet optimisé est un parcours de vérification distinct ; il ne remplace pas ces 33 tests. Aucun test sur téléphone physique n’est revendiqué. L’APK téléchargeable utilise une **clé de test**, conservée hors Git, et n’est pas une distribution Play Store signée pour la production.

Preuves d’exécution : `test-results/audit-build-0.6.0.txt`, `test-results/audit-android-0.6.0.txt`, `test-results/program-tests-0.6.0.xml`, `test-results/unit-tests-0.6.0.xml`, `test-results/audit-content-0.6.0.txt` et `test-results/audit-guide-0.6.0.txt`. Les exécutions de CI sont consultables dans le [workflow Android](https://github.com/raphaelmounirprivate-dot/full-body/actions/workflows/android.yml) ; une configuration présente ne constitue pas à elle seule une exécution réussie.

Limite de performance : le cache supprime les redécodages et revalidations des séances inchangées, mais les lectures SQL et le modèle de liste restent proportionnels à l’historique. Aucun temps constant ni benchmark sur 10 000 séances n’est revendiqué. La base SQLite entière endommagée n’est pas assimilée au cas traité ici d’une ligne JSON invalide.
