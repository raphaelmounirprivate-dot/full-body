# Validation 0.4.0 — 26 octobre 2026

L’écran Séance affiche désormais un bandeau d’avancement, des illustrations et des exercices dépliables. La prochaine série non enregistrée est proposée à la saisie ; les autres restent accessibles. Les séries enregistrées se résument sur une ligne avec accès à la correction. L’échauffement reste consultable au-dessus des exercices.

## Résultats exécutés localement

- Compilation APK debug et AAB release non signé réussie.
- 22 tests JVM réussis : 16 de programme et 6 de règles.
- 20 tests Android réussis sur Sport_QA, port 5556 : 11 de persistance et 9 de parcours UI.
- 11 contrôles Python réussis : 4 sur le contenu historique, 7 sur le guide corrigé.
- Lint : 0 erreur, 13 avertissements et 1 indication. Les avertissements concernent les versions de dépendances ; ils ne sont pas annoncés comme corrigés.

Le nouveau parcours UI vérifie la conservation d’une charge saisie après ouverture d’un autre exercice, puis la correction d’une série enregistrée. Les parcours existants couvrent toujours la recréation de l’activité, les fiches et leurs brouillons, les options sautées/réintégrées et les remplacements.

Un premier passage a détecté une assertion trop précoce après l’enregistrement asynchrone d’un exercice sauté. Le test attend désormais le retour « Réintégrer cet exercice » avant de faire défiler le message. Le passage complet suivant réussit : `OK (20 tests)`.

Captures réelles sur émulateur : `screenshots/seance.png`, `screenshots/seance-apercu.png` et `screenshots/seance-grande-police.png` (police système à 130 %, rétablie ensuite). Les contenus restent défilables avec la grande police. Les données de l’émulateur interactif et du téléphone personnel n’ont pas été utilisées.

Preuves : `test-results/session-design-build.txt`, `test-results/session-design-tests.txt`, `test-results/unit-tests.xml` et `test-results/program-tests.xml`. Le programme, les règles métier et la persistance n’ont pas été modifiés. Aucun essai sur téléphone physique ni exécution CI GitHub n’est revendiqué.
