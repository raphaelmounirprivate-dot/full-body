# Validation 0.3.1 — 26 octobre 2026

Le titre et les deux paragraphes du bloc « À propos du guide » ont été supprimés de l’accueil. Le bouton d’enregistrement du PDF reste disponible, sans cette carte explicative.

Build APK/AAB réussi, 22 tests JVM et 8 tests UI existants réussis dans Sport_QA (port 5556), sans utiliser les données de la session interactive. Lint : aucune erreur bloquante. Contrôle de la hiérarchie Android et capture en bas de l’accueil : bloc absent, bouton PDF accessible.

Preuves : `docs/test-results/remove-about-build.txt`, `docs/test-results/remove-about-ui.txt` et `docs/screenshots/accueil-sans-about.png`. Les validations du contenu du guide réalisées pour 0.3.0 restent documentées dans `docs/validation.md` ; les assets et les règles d’entraînement ne sont pas modifiés dans cette mise à jour.
