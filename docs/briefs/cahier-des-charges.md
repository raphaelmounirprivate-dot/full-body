# Prompt Codex — Application Android Sport / Jims Namur

## Mission
Tu es mon agent de développement. Crée une application Android native de suivi de musculation et de remise en forme, en français, à partir du document fourni `Programme_Jims_Namur_images_uniformisees.pdf` (31 pages). Commence par créer ou actualiser `AGENTS.md` à la racine et applique les règles ci-dessous. Ne prétends jamais qu'une action a été exécutée si elle ne l'a pas été.

## 1. Produit attendu
Nom provisoire : **Mon Coach Sport**. Application personnelle Android pour smartphone, lisible et utilisable d'une main en salle. Importer fidèlement le cycle de 8 semaines, la rotation des séances A/B/C, les 19 fiches F1 à F19, les consignes de sécurité, les variantes et les règles de progression du PDF. Conserver les noms, les paramètres et les illustrations d'origine, sans inventer des exercices ni modifier silencieusement le programme. Les images doivent être extraites des pages 9 à 27 avec une qualité adaptée au téléphone ; conserver la source et les droits d'utilisation, ne pas confondre les illustrations avec des notices de machines.

## 2. Stack et architecture
- Application native Android : Kotlin, Jetpack Compose, Material 3, architecture MVVM, Navigation Compose.
- Persistance locale : Room (SQLite), DataStore pour préférences ; application entièrement utilisable sans connexion ni compte.
- Injection de dépendances : Hilt uniquement si cela simplifie réellement le projet ; coroutines et Flow pour les états réactifs.
- Aucun backend obligatoire pour le MVP. Le prompt web initial (TypeScript/NestJS/PostgreSQL/Vercel) est remplacé pour le mobile ; ne pas forcer Vercel pour une APK Android. Si une synchronisation multi-appareils est demandée ultérieurement, proposer une architecture optionnelle séparée avec consentement explicite.
- Build avec Gradle, Android Studio, SDK Android actuel et versions de dépendances stables compatibles. Documenter minSdk, targetSdk et versions effectivement choisies.
- Fournir APK debug installable ; préparer la génération d'un AAB release sans publier sur Google Play sans autorisation.

## 3. Règles de développement à inscrire dans AGENTS.md
KISS ; séparation des responsabilités ; clean code ; fonctions courtes ; noms explicites ; pas de duplication ni de code mort ; architecture MVVM avec logique métier dans des use cases/services et accès aux données dans des repositories ; interface sans logique de persistance ; composants accessibles et testables. Aucun service, abonnement, API ou clé payante. Pas de télémétrie ni publicité par défaut.

## 4. Navigation Android
Barre inférieure à quatre onglets : **Accueil**, **Séance**, **Progression**, **Exercices**. Accueil : semaine actuelle, prochaine séance dans la rotation, dernier entraînement, accès direct « Commencer ». Séance : liste ordonnée des exercices, séries, reps, repos et options. Progression : historique, courbes de charges et répétitions, nombre de séances, minutes de cardio et bilan hebdomadaire. Exercices : catalogue F1-F19, recherche, images agrandissables, consignes et variantes.

## 5. Séances à intégrer (valeurs exactes du guide)
### Séance A
1. Presse à cuisses (F1) — 2 × 10–15 — repos 120 s.
2. Chest press guidée (F2) — 2 × 8–12 — repos 90–120 s.
3. Tirage vertical (F3) — 2 × 10–15 — repos 90–120 s.
4. Leg curl assis (F4) — 2 × 10–15 — repos 60–90 s.
5. Élévations latérales à la poulie (F5, option) — 2 × 12–15 par bras — repos 60 s après les deux bras.
6. Planche (F6) — 2 × 15–30 secondes — repos 45–60 s.

### Séance B
1. Squat à levier (F7) OU hack squat (F8) — 2 × 10–12 — repos 120 s ; jamais les deux par défaut.
2. Rowing avec appui poitrine (F9) — 2 × 10–15 — repos 90–120 s.
3. Chest press guidée (F2) — 2 × 10–15 — repos 90–120 s.
4. Hip thrust machine (F11) — 2 × 10–15 — repos 90 s.
5. Curl poulie basse (F12, option) — 2 × 10–15 — repos 60–90 s.
6. Triceps à la corde (F13, option) — 2 × 10–15 — repos 60–90 s.

### Séance C
1. Presse à cuisses (F1) — 2 × 10–15 — repos 120 s.
2. Rowing assis à la poulie (F14) — 2 × 10–15 — repos 90 s.
3. Chest press guidée (F2) — 2 × 8–12 — repos 90–120 s.
4. Leg curl assis (F4) — 2 × 10–15 — repos 60–90 s.
5. Tirage vertical (F3) — 2 × 10–15 — repos 90 s.
6. Développé épaules machine (F15, option) — 2 × 10–15 — repos 90 s.

Les autres fiches restent disponibles comme variantes : F10 développé incliné aux poulies, F16 écarté pectoraux aux poulies, F17 rameur, F18 leg extension et F19 crunch. Respecter les conditions de remplacement décrites dans le PDF ; une variante remplace un mouvement, elle ne s'ajoute pas automatiquement.

## 6. Parcours de séance et saisie
- Échauffement guidé : 5–7 min vélo/elliptique, 1–2 séries faciles jambes, puis une série légère sur la première poussée et le premier tirage ; ne pas compter ces séries dans les séries de travail.
- Afficher la photo/illustration, la cible, les consignes essentielles, les séries, les répétitions et le repos.
- Saisir pour chaque série la charge (kg, avec précision « par côté » ou « total » si pertinent), les répétitions ou secondes, la réserve estimée (RIR), et marquer la série terminée.
- Afficher les résultats de la dernière séance sur le même exercice et la même machine ; enregistrer les réglages du siège et les notes.
- Minuteur de repos démarrable automatiquement après validation d'une série, avec pause/reprise, vibration ou notification locale si autorisée ; fonctionne écran verrouillé autant que le permet Android.
- Séance interruptible et reprenable sans perte, possibilité de corriger les saisies et de sauter les options.
- Fin de séance : durée, cardio (appareil, minutes, intensité /10), énergie /10, inconfort/douleur, notes et prochaine décision.

## 7. Progression sur 8 semaines
- Rotation A → B → C indépendante des jours de la semaine ; ne pas avancer une séance non réalisée. Conseiller au moins un jour sans musculation entre séances.
- Semaines 1–2 : 2 séries, RIR 3–4, cardio 8–12 min facile.
- Semaines 3–4 : 2 séries, RIR 2–3, cardio 12–18 min facile à modéré.
- Semaines 5–6 : 2 séries suffisantes ; troisième série facultative et progressive uniquement sur les exercices 1–3 ; cardio 15–20 min modéré après stabilisation du volume.
- Semaines 7–8 : volume stable ; cardio 20–25 min modéré ; intervalles facultatifs à partir de la semaine 7.
- Conseiller une augmentation de charge seulement après deux passages consécutifs atteignant le haut de la plage sur toutes les séries avec la réserve prévue et des réglages identiques ; plus petit incrément disponible. Ne jamais augmenter automatiquement.
- En cas de récupération insuffisante, privilégier retrait des options et adaptation du volume ; ne jamais culpabiliser l'utilisateur ni imposer une progression.
- Intervalles facultatifs : 5 min faciles, 4 cycles de 30 s soutenues contrôlées / 90 s faciles, puis 3–5 min faciles ; pas de sprint maximal. Jusqu'à 5–6 cycles ultérieurement si bien toléré.

## 8. Données et confidentialité
Modèles : Exercise, ExerciseVariant, WorkoutTemplate, WorkoutSession, ExerciseEntry, SetEntry, MachineSetting, CardioEntry, WeeklyReview, UserPreferences. Identifiants stables, horodatage local, unités explicites et migrations Room. Export/import JSON et CSV de l'historique avec validation, sauvegarde/restauration locales, suppression de toutes les données sur demande. Ne pas collecter automatiquement de données de santé ni envoyer de données sur un serveur.

## 9. UX/UI
Design sportif sobre, moderne, accessible : fond clair/sombre, contrastes élevés, texte lisible, grandes zones tactiles, chiffres de charge/reps faciles à modifier, clavier numérique, retours haptiques configurables. Français intégral. Mode portrait prioritaire, prise en charge de tailles d'écran variées et de l'agrandissement de police. Graphiques simples sans gamification culpabilisante. Pas d'écran de connexion obligatoire.

## 10. Illustrations et contenu
Importer les 19 fiches du guide en associant F1–F19 aux exercices correspondants. Extraire ou recadrer proprement les illustrations des pages 9–27 ; ne pas incorporer chaque page PDF complète comme seule interface. Afficher « Installer / Bouger / Vérifier » et les mises en garde depuis le texte source. Mentionner que les équipements réels peuvent différer et que les sécurités doivent être montrées par un coach. Garder un lien vers le guide PDF complet dans « À propos » si le fichier est embarqué.

## 11. Skills Codex
Examiner les skills demandés dans le prompt d'origine : UI UX Pro Max, Design Taste Frontend, GPT Taste, Humanizer, Impeccable, Stop Design Slop, Graphify. Chercher uniquement leurs sources officielles GitHub ; vérifier leur compatibilité Codex, gratuité et sécurité en lisant SKILL.md et scripts avant installation. Ne pas inventer de source, ne pas installer de skill ambigu ou inadapté à Android. Consigner dans AGENTS.md les skills réellement installés, source, version/commit et usage ; signaler les autres.

## 12. Tests et livraison
Écrire des tests unitaires des règles de rotation/progression, des tests Room (persistance et migration), des tests UI Compose du flux séance et des tests instrumentés sur émulateur Android. Vérifier hors ligne, redémarrage en cours de séance, notifications, export/import, affichage des 19 fiches et absence de pertes de données. Documenter les commandes de build et test dans README.md. Fournir le code source, AGENTS.md, README.md, APK debug et captures d'écran. Si accès GitHub autorisé : créer un dépôt privé ou réutiliser l'existant sans écrasement, vérifier secrets et tests, puis pousser. Ne jamais publier sur Google Play ni créer un compte/service payant sans mon accord. En cas de blocage, détailler l'étape et l'action attendue.

## 13. Plan d'exécution
1. Inspecter les deux PDF et dresser la correspondance des fiches/exercices/illustrations.
2. Créer AGENTS.md et le squelette Android ; obtenir un premier build local.
3. Implémenter les modèles Room et précharger le programme exact.
4. Développer Accueil, Séance, Exercices, Progression.
5. Ajouter chronomètre, sauvegarde locale, export et règles de progression.
6. Exécuter les tests, corriger les défauts et produire l'APK.
7. Documenter ce qui fonctionne réellement et les limites restantes.
