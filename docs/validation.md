# Validation Android 0.3.0

Vérifications du 26 octobre 2026 sous Windows, JDK 17 et SDK Android local. Émulateur isolé Sport_QA (port 5556), AOSP Android 11/API 30, x86_64, 432 × 936 pixels, densité 180, WHPX. La session interactive Sport_Test (port 5554) n’a pas été utilisée pour les tests destructifs.

## Résultats obtenus

- Build debug et AAB release non signé : réussis.
- Android Lint : aucune erreur bloquante. Avertissements conservés, notamment sur les versions de dépendances plus récentes disponibles.
- 22 tests JVM : prescriptions, variantes du guide et plages spécifiques, refus de hausse de charge sur la planche, rotation, options, séries supplémentaires, RIR, unités, changement de machine, changement d’heure, CSV et progression. Les six tests métier initiaux restent présents.
- 19 tests instrumentés : 11 tests de stockage/notifications et 8 tests UI. Exécution directe ADB : `OK (19 tests)`.
- 4 tests Python : 19 identifiants, images/consignes, paramètres du programme illustré et absence de permission Internet dans le manifeste source.
- 7 tests Python supplémentaires : PDF corrigé de 31 pages et empreinte, pages F1–F19, concordance de chaque texte avec sa page source, 19 images distinctes de même taille, légendes spécifiques F4/F6/F17, noms et consignes générales. Total : 52 tests réussis.
- Signature APK v2 vérifiée ; certificat SHA-256 identique à la v0.1 : `040f1fd30560381354cac6610cc36828fbf6b4b7e255759cbb5c5aa179ad3a68`.
- APK `versionCode=4`, `versionName=0.3.0`, minSdk 26, targetSdk 36.

Les nouveaux tests vérifient « Voir la fiche » depuis la séance et le catalogue, la conservation du brouillon de charge à la fermeture, ainsi que l’accès à la fiche d’un exercice facultatif sauté. Vérification visuelle de la fenêtre défilante avec illustration et consignes.

Les tests Android vérifient la conservation d’une séance après migration SQLite 1→2, le décodage des anciens JSON, l’aller-retour CSV avec unités/guillemets/retours à la ligne/brouillons, la restauration des bilans/préférences, le refus d’un import invalide sans effacer l’existant, le refus de sauter une obligation dans un import, le rappel local, les onglets, la validation/reprise d’une série et le retrait/rétablissement d’une option.

Les captures actuelles sont issues de l’émulateur de test : accueil, cycle, séance, progression, catalogue et fiche. Les données de démonstration des tests ne sont pas embarquées dans l’APK.

## Reproduction

```powershell
./scripts/build.ps1
./scripts/build.ps1 -Tasks ':app:assembleDebugAndroidTest', ':app:bundleRelease'
./scripts/test-device.ps1 -DeviceId emulator-5554
python scripts/verify_content.py
```

Le lanceur Gradle UTP échoue dans cet environnement sandboxé avant exécution ; la commande ADB exécute réellement les tests. Ne pas confondre ce problème d’outil avec un succès de `connectedDebugAndroidTest`. Les logs effectifs sont dans `docs/test-results/`.

## Limites

Pas d’essai sur téléphone physique ni de validation sportive/médicale. La permission de notification Android 13+, la veille prolongée et les particularités des constructeurs restent à vérifier. Le test de rappel appelle le récepteur local et contrôle la notification, sans prétendre vérifier une échéance exacte en veille. Le sélecteur Android de fichiers n’est pas automatisé de bout en bout ; codecs, validation et restauration sont testés sur appareil.

Le bon guide de 31 pages est fourni, intégré et conservé sans modification. Ses 19 illustrations et textes sont importés depuis les pages 9–27. La nouvelle présentation reprend les bleus et les cartes de la maquette fournie. La planche ne suggère pas d’augmentation de charge. Les anciens enregistrements et leur ancien document restent séparés.
Le test de navigation supplémentaire vérifie le retour à Accueil après le démarrage d’une séance et un changement d’onglet.
