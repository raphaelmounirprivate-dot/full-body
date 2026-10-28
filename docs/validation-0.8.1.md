# Validation locale 0.8.1

28 octobre 2026. Objectif : variantes machines/poulies et adaptation du volume pour la remise en forme, sans réinterpréter l’historique.

## Résultats

- Build debug, APK de test Android, release optimisé et AAB : réussis, en mode hors ligne avec les dépendances déjà présentes.
- Tests JVM : 45, sans échec, dont 5 nouveaux tests d’adaptation et de comparaison des prescriptions.
- Contrôles Python : 4 tests de contenu et 8 tests du guide, réussis. PDF source, empreintes et fiches inchangés.
- Lint debug : 0 erreur, 12 avertissements, 2 indications.
- Tests Android : 50 tests, sans échec au passage final (dont trois nouveaux).
- Grande police : les trois nouveaux tests rejoués à 130 %, sans échec.
- APK optimisé installé et lancé sur QA : choix de B/F10 par l’éditeur puis création de B avec le développé incliné aux poulies visible. Aucun crash dans le journal du processus ; empreinte de l’APK installé identique au fichier livré.
- Captures inspectées : [bilan](screenshots/adaptation-bilan-0.8.1.png), [variantes](screenshots/adaptation-variantes-0.8.1.png), [police 130 %](screenshots/adaptation-grande-police-0.8.1.png), [thème sombre](screenshots/adaptation-sombre-0.8.1.png), [séance B adaptée](screenshots/adaptation-seance-b-0.8.1.png).

Total : **107 tests distincts réussis** (45 JVM, 50 Android, 12 Python), plus trois tests Android rejoués à 130 %. [Rapports](test-results/adaptation-jvm-0.8.1.json) · [suite Android](test-results/adaptation-android-0.8.1.txt) · [essai optimisé et SHA-256](optimized-smoke-0.8.1.json).

APK optimisé : 6 790 832 octets ; SHA-256 `69a53dac6b0690c7915d551ac03fc0641a3f5e1ac151cdd5f149e08422f97821`. Certificat de test inchangé : `040f1fd30560381354cac6610cc36828fbf6b4b7e255759cbb5c5aa179ad3a68`.

Les premiers passages de la suite Android ont révélé des attentes insuffisantes dans des tests de brouillons existants (écriture asynchrone et champs hors de la zone visible). Les tests attendent désormais la persistance et ciblent la charge par exercice/série, avec défilement explicite ; les assertions de conservation restent présentes.

## Cas nouveaux couverts

- Choix d’une variante dans l’éditeur, persistance de la préférence et application seulement aux nouvelles séances ; reprise d’une séance B en pause conservant son ancien plan.
- Préservation des variantes des autres emplacements, réinitialisation explicite et validation des préférences.
- Deux séries de départ et six mouvements conservés ; variantes avec leurs prescriptions propres.
- Activité sur sept jours : identifiants dédoublonnés, anciennes séances, séances futures/en pause/inachevées exclues, séries non validées exclues.
- Ajout de troisième série refusé sans confirmation ; contrôle dans l’interface et retour aux deux séries.
- Sauvegarde/restauration des préférences ; ancien JSON préservant le choix local, import invalide refusé, objet vide permettant une réinitialisation.
- Progression entre deux prescriptions comparables malgré une autre plage intermédiaire ; échec comparable, machine/réglage différent et historique incomplet bloquants.

## Limites

Les tests Android utilisent uniquement l’émulateur dédié **Sport_QA, Android 11 / API 30**. Aucun essai sur téléphone physique Android 13+ ni validation individuelle des gestes, douleurs ou machines du club. L’APK optimisé utilise la clé de test existante, pas une clé de distribution commerciale. Aucun nouveau tag ni téléchargement GitHub 0.8.1 n’a été publié lors de cette adaptation locale.

Room v4, JSON complet v5 et CSV v3 sont conservés. Une préférence facultative est ajoutée au JSON personnel ; les variantes réalisées restent stockées dans chaque séance. Les règles et mesures corporelles antérieures restent en place.
