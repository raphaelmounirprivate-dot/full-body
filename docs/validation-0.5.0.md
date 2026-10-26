# Validation 0.5.0 — 26 octobre 2026

## Choix et reprise de séance

Le bouton « Changer » reste visible à côté du titre pendant le défilement. Il ouvre un choix A/B/C. Avant tout démarrage, « Choisir A, B ou C » donne le même accès. Choisir une autre séance met la précédente en pause sans la terminer. Revenir dessus conserve identité, semaine, options, variantes, séries, unités, réglages et brouillons. La vue revient au début de la séance choisie.

Le changement est transactionnel dans Room et n’autorise qu’une séance active. Une seule séance non terminée existe pour chaque couple programme/type. Le minuteur est arrêté avant l’activation. Les champs de saisie utilisent une identité de composition propre à la séance et les écritures de saisie/validation portent son identifiant. Les suggestions et la rotation utilisent la date de fin réelle pour tenir compte des séances reprises plus tard.

Le champ de pause est ajouté au payload JSON existant avec valeur par défaut `false` ; Room reste en version 2. Les exports JSON et CSV utilisent le format 3. JSON 1/2 et CSV format 2 restent acceptés. L’import refuse plusieurs séances actives, des pauses sur des séances terminées et les doublons non terminés de même programme/type avant remplacement des données. Les anciens APK ne savent pas restaurer le nouveau format 3.

## Vérifications exécutées

- Build APK debug et AAB release non signé réussi.
- 24 tests JVM : 18 tests de programme et 6 tests de règles, sans échec.
- 26 tests Android sur Sport_QA, port 5556 : 15 tests de stockage et 11 tests UI, sans échec.
- 11 contrôles Python : 4 sur le contenu historique et 7 sur le guide, sans échec.
- Lint : aucune erreur, 13 avertissements et 1 indication, conservés dans le rapport de build.

Les nouveaux tests couvrent A → B → A avec série validée et brouillon, le redémarrage de l’activité en B, l’arrêt du repos, le choix C avant démarrage, la séparation des valeurs d’un exercice commun, l’activation sans doublon, les restaurations JSON/CSV et le refus transactionnel des imports invalides. La rotation suit la dernière séance terminée, indépendamment de son ordre de création ; une séance reprise avec un passage insuffisant empêche une suggestion de hausse fondée sur deux passages plus anciens.

Les premiers essais UI ont révélé que le test cherchait un en-tête sorti de la zone composée et tentait de faire défiler l’écran initial non défilant. Le titre et son bouton sont désormais épinglés, et le test vérifie directement que le bouton est affiché lors des changements. Le passage complet final réussit : `OK (26 tests)`.

Preuves : `test-results/session-choice-build.txt`, `test-results/session-choice-tests.txt`, `test-results/unit-tests-0.5.0.xml`, `test-results/program-tests-0.5.0.xml` et les captures `screenshots/choisir-seance.png`, `screenshots/seance.png`. Le sélecteur et la séance ont aussi été inspectés avec une police agrandie à 130 % ; le dialogue reste défilable et la taille de police a été rétablie ensuite. Les essais utilisent uniquement l’émulateur QA. Aucun test sur téléphone physique ni résultat CI GitHub n’est revendiqué.
