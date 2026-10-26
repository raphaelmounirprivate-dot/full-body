# Développement de Mon Coach Sport

## Objectif

Construire une application Android personnelle en français pour suivre un programme Full Body sur machines, consulter ses fiches et conserver ses résultats hors ligne. La simplicité de saisie, la lisibilité et la préservation des données orientent les évolutions.

## Architecture

- Interface : Kotlin et Jetpack Compose Material 3, navigation entre accueil, séance, progression et exercices.
- État : ViewModel, coroutines et Flow ; logique métier séparée des écrans.
- Règles : `Domain.kt` et `Programs.kt` portent les données, prescriptions et conditions de progression.
- Stockage : repository dans `Storage.kt`, Room pour les séances et bilans, DataStore pour les préférences.
- Sauvegardes : JSON et CSV avec validation avant remplacement transactionnel.
- Repos : `RestScheduler.kt`, minuteur et notifications locales facultatives.
- Contenu : guide PDF original, fiches structurées et illustrations embarquées dans les assets.

Room utilise une migration explicite de la version 1 vers la version 2. Une ancienne séance conserve son programme, sa semaine, ses options, son unité et ses paramètres. Une suggestion de progression ne modifie jamais automatiquement la charge.

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

La validation de la version 0.3.1 rapporte une compilation APK/AAB réussie, 22 tests JVM et 8 tests UI réussis, ainsi qu’un lint sans erreur bloquante. Les contrôles complets de la version 0.3.0 sont conservés séparément. Voir `validation-0.3.1.md` et `validation.md` : ces résultats décrivent les exécutions locales documentées, pas une intégration continue GitHub.

## Suite du développement

Restent à vérifier sur téléphone physique : notifications sous Android 13 et versions suivantes, veille prolongée et comportement selon le constructeur. Une future distribution stable nécessite également une signature release et la vérification des droits de diffusion des ressources fournies. Ces éléments ne sont pas annoncés comme terminés.

Aucun compte, backend, abonnement, publicité ou télémétrie n’est nécessaire. Le guide personnel est une source de contenu ; il ne constitue pas une notice constructeur ni une validation médicale.
