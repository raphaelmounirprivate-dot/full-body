# Développement de Mon Coach Sport

## Objectif

Construire une application Android personnelle en français pour suivre un programme Full Body sur machines, consulter ses fiches et conserver ses résultats hors ligne. La simplicité de saisie, la lisibilité et la préservation des données orientent les évolutions.

## Architecture

- Interface : Kotlin et Jetpack Compose Material 3, navigation entre accueil, séance, progression et exercices.
- État : ViewModel, coroutines et Flow ; logique métier séparée des écrans.
- Règles : `Domain.kt` porte les données et les validations simples ; `Programs.canIncrease` est l’unique règle de progression utilisée et testée.
- Stockage : repository dans `Storage.kt`, Room pour les séances et bilans, DataStore pour les préférences.
- Sauvegardes : JSON et CSV avec validation avant remplacement transactionnel.
- Repos : `RestScheduler.kt` pour les notifications, `RestTimerCard.kt` pour le compte à rebours. Les tics ne recomposent pas le reste de la séance.
- Écrans : `WorkoutScreen.kt`, `SetEditor.kt`, `SessionSummary.kt`, `ExerciseScreen.kt` et `ProgressScreen.kt` ; `MainActivity.kt` conserve l’entrée et la navigation.
- Contenu : originaux PDF conservés dans `docs/sources/`, copies mobiles et illustrations dans les assets.

Room conserve la migration 1→2 et ajoute 2→3 pour les champs de brouillon. Une frappe ne réécrit plus le payload JSON complet. Les anciennes séances, leurs brouillons intégrés, leur semaine, leurs options et leurs unités restent conservés. Une lecture transactionnelle des séances et de leurs brouillons évite d’assembler deux états provenant d’instants différents. Le cache de `SessionRowDecoder` revalide uniquement les payloads modifiés ; les lectures SQL restent proportionnelles à l’historique et ne sont pas annoncées comme un coût constant. Une suggestion de progression ne modifie jamais automatiquement la charge.

Depuis 0.5.0, `Training.paused` est un champ optionnel du payload JSON existant, avec valeur par défaut `false` : aucune migration de table supplémentaire. `SportRepository.activate` met la séance précédente en pause et active la suivante dans une transaction. Une reprise conserve l’identité et tous les paramètres de la séance. Le minuteur est annulé avant le changement ; les saisies et validations portent l’identifiant de leur séance pour éviter une écriture tardive dans une autre séance. JSON/CSV format 3 conservent les pauses, les anciens formats restent importables. La rotation et les derniers passages utilisent la date de fin réelle.

Depuis 0.6.0, une ligne locale illisible reste dans la base, mais est exclue des séances utilisables et signalée par un avertissement. L’export JSON utilise alors la version 4 de récupération avec un bloc `recovery` contenant les lignes et brouillons bruts, pour que les anciens APK refusent aussi ce fichier. Les sauvegardes ordinaires restent en version 3. Son import automatique est refusé tant qu’il n’a pas été examiné et réparé ; un import valide remplace les données locales après confirmation, il faut donc exporter avant ce remplacement. Les erreurs de format et de validation sont expliquées en français. Chaque séance est validée avec le catalogue de son propre programme. Une hausse de charge n’est pas suggérée lorsqu’une ligne illisible empêche de garantir l’historique complet.

## Construire sur un autre poste

Installer Android Studio, le SDK Android 36 et un JDK 17. Ouvrir la racine du dépôt dans Android Studio et synchroniser Gradle. Les dépendances doivent être téléchargées lors de la première compilation ; l’application installée fonctionne ensuite hors ligne.

Sous Windows, depuis la racine :

```powershell
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Sous Linux ou macOS :

```sh
sh ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

L’APK est produit dans `app/build/outputs/apk/debug/app-debug.apk`. Le SDK, le JDK, les caches, les clés et les résultats de compilation ne sont pas inclus dans Git. Pour une diffusion finale, le propriétaire doit fournir une signature release ; la version debug sert aux essais locaux.

## Vérifications

Les tests JVM couvrent les règles métier. Les tests Android couvrent la persistance et les parcours d’interface. Les scripts Python contrôlent les fiches, leur correspondance avec le PDF et l’intégrité du document embarqué ; ils nécessitent pypdf et Pillow, ainsi que pypdfium2 pour les imports d’illustrations.

```powershell
python scripts/verify_content.py
python scripts/verify_guide.py
```

Les tests Android doivent utiliser un émulateur dédié : certains effacent les données de l’application. Ne pas les lancer sur le téléphone ou l’émulateur contenant les séances personnelles.

La validation de la version 0.5.0 rapporte une compilation APK/AAB réussie, 24 tests JVM, 26 tests Android et 11 contrôles Python réussis, ainsi qu’un lint sans erreur bloquante. Voir `validation-0.5.0.md`. Les contrôles des versions précédentes sont conservés dans `validation-0.4.0.md`, `validation-0.3.1.md` et `validation.md` : ces résultats décrivent les exécutions locales documentées, pas une intégration continue GitHub.

## Suite du développement

Restent à vérifier sur téléphone physique : notifications sous Android 13 et versions suivantes, veille prolongée et comportement selon le constructeur. Une future distribution stable nécessite également une signature release et la vérification des droits de diffusion des ressources fournies. Ces éléments ne sont pas annoncés comme terminés.

Aucun compte, backend, abonnement, publicité ou télémétrie n’est nécessaire. Le guide personnel est une source de contenu ; il ne constitue pas une notice constructeur ni une validation médicale.
