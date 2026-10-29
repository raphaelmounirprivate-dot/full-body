# Validation locale 0.9.1

29 octobre 2026. Un seul exercice courant, repos automatique à la validation et chrono fixe en bas de la séance.

## Résultats exécutés

- Build debug, APK de test Android, APK optimisé et AAB : réussis.
- **52 tests JVM**, **58 tests Android**, **9 contrôles du guide** et **4 contrôles de contenu** réussis : **123 tests distincts**.
- Onze tests Android rejoués à **130 % de texte**, réussis (mode guidé, chrono, migration et actions de repos).
- Lint debug : 0 erreur, 12 avertissements, 2 indications.
- Les anciens tests de mode liste l’activent explicitement ; le mode guidé est vérifié comme expérience par défaut.

Le premier essai a détecté un plantage de première mise en page dans AwaitFirstLayoutModifier, avec deux commandes de défilement simultanées. Elles ont été réunies en une seule. Le passage final complet, la recréation de l’activité et les essais en grande police réussissent. Le message de record a été déplacé dans le contenu fixe au-dessus du chrono ; ses commandes ne le recouvrent pas.

## Scénarios vérifiés

- Saisie invalide : aucune série enregistrée, aucun avancement, aucun nouveau repos.
- Première validation persistée : série 2 du même exercice et repos démarré ; deuxième validation : exercice suivant, chrono conservé et visible.
- Défilement jusqu’au lien de bilan : Pause et +30 s restent affichés sans remonter la liste.
- Troisième série configurée : pas de passage prématuré après deux séries.
- Correction d’une série déjà enregistrée : pas de redémarrage du repos.
- Pause/reprise et fermeture comparent l’échéance attendue ; une commande ancienne ne modifie pas un repos plus récent. Un repos expiré n’est pas réactivé.
- Initialisation unique du mode guidé/repos automatique, conservation des autres préférences et séances ; choix ultérieurs conservés, y compris après effacement des données puis nouveau réglage.
- Recréation de l’activité : reprise sur le bon exercice à partir des données persistées et maintien du chrono.
- Import/export, anciennes séances, Rear Kick et mesures : suite Android complète conservée.

## Essai du paquet optimisé

APK installé et lancé sur **Sport_QA, Android 11 / API 30**. Les données de démonstration de cet émulateur dédié ont été réinitialisées pour l’essai ; aucun téléphone physique ni donnée utilisateur réelle n’est concerné.

Validation de deux séries de presse, apparition de la chest press et contrôle du chrono en bas. Captures inspectées :

- [Une seule série à saisir et chrono](screenshots/seance-guidee-chrono-0.9.1.png)
- [Chrono après défilement](screenshots/chrono-visible-apres-defilement-0.9.1.png)
- [Exercice suivant](screenshots/exercice-suivant-0.9.1.png)
- [Grande police](screenshots/chrono-grande-police-0.9.1.png)
- [Thème sombre](screenshots/chrono-theme-sombre-0.9.1.png)

Les charges des captures sont des données de démonstration. L’APK installé correspond exactement au fichier livré ; aucun crash dans le journal du processus de cet essai final.

Version 0.9.1, code 14, 6913884 octets. SHA-256 : `705971efa2e580bebbce5c96819343a06b5cb70fd88da27db6bc453bc6fec194`.

Certificat de test inchangé : `040f1fd30560381354cac6610cc36828fbf6b4b7e255759cbb5c5aa179ad3a68`.

[Résultats JVM](test-results/focused-jvm-0.9.1.json) · [suite Android](test-results/focused-android-0.9.1.txt) · [grande police](test-results/focused-font130-0.9.1.txt) · [preuve du paquet installé](optimized-smoke-0.9.1.json).

## Données et portée

Room v4 et JSON 5/CSV 3 inchangés. Le marqueur interne focusedWorkoutV1 initialise une fois la présentation et le repos automatique ; il ne réactive ni les notifications ni leurs autorisations. Le mode liste et les choix ultérieurs restent disponibles. Les anciennes séances, variantes, séries, brouillons et réglages sont conservés. Le repos correspond à la série qui vient d’être validée et continue pendant l’affichage du mouvement suivant.

APK local : `output/v0.9.1/Mon-Coach-Sport-0.9.1.apk`. [Publication GitHub 0.9.1](https://github.com/raphaelmounirprivate-dot/full-body/releases/tag/v0.9.1) vérifiée : téléchargement public et somme SHA-256 identiques au paquet testé, certificat de test conservé. [Preuve de publication](github-publication-0.9.1.json).

Le [contrôle GitHub tests et lint](https://github.com/raphaelmounirprivate-dot/full-body/actions/runs/40841290986) a réussi pour le code source `c649e56ae48705617b537f4f3ef06007ebe1f9cd`. Aucun essai sur téléphone physique revendiqué.
