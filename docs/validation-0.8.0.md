# Validation 0.8.0

VersionCode 10, 27 octobre 2026. Cinq ajouts : notification de repos en décompte, carte terminée et fermeture, ajustement des durées, records comparables et widget Android natif.

## Tests locaux

- 40 tests JVM : les 30 précédents et 10 nouveaux sur les durées, records et modèle du widget.
- 47 tests Android : les 40 précédents et 7 nouveaux sur les métadonnées du chronomètre, son annulation, la fermeture conditionnelle, la non-répétition du signal, le rendu RemoteViews, les commandes de durée et les records après écriture.
- 12 tests Python sur les contenus et les deux guides conservés.
- Six parcours d’interface rejoués à 130 % de taille de texte ; ils ne sont pas comptés comme six tests distincts supplémentaires.
- APK debug, APK optimisé R8 et AAB ; lint sans erreur bloquante.

Le dernier passage donne **99 tests distincts réussis**. Les deux tests de brouillon/correction ont aussi été rejoués trois fois de suite avec le widget installé, avant la suite complète. Le lint final comporte 0 erreur, 23 avertissements et 2 suggestions.

Les résultats exécutés sont conservés dans `docs/test-results/rest-*-0.8.0.*`. Le détail JVM figure dans `rest-jvm-0.8.0.json`.

Un test historique de correction de charge ciblait le premier champ « kg total » alors que deux séries pouvaient être éditables. Il cible maintenant le champ accessible nommé par exercice et numéro de série, attend le brouillon de cette série, puis vérifie les 30 kg enregistrés et leur affichage après défilement. Le contrôle isolé a été réussi avant reprise de la suite complète, avec un widget déjà installé.

## Repos

Le chronomètre est rendu par Android (`setWhen`, `setUsesChronometer`, `setChronometerCountDown`), sur un canal de faible importance sans son ni vibration au démarrage. L’application ne pousse pas une notification par seconde. La notification a un contenu générique public pour l’écran verrouillé ; les réglages Android conservent la décision finale d’affichage.

L’échéance est persistée et l’alarme précise 0.7 est conservée. Pause, fermeture et changement de séance retirent le décompte. Le décompte est retiré à la fin, avec un délai d’expiration système en complément. L’échéance du ViewModel et l’alarme partagent le même marqueur de livraison : un seul signal de fin. Si les rappels sont activés et que l’application est visible sur un téléphone interactif, une vibration explicite est demandée ; la notification finale est alors silencieuse. Son et vibration restent soumis aux réglages du téléphone.

À zéro, le bouton Pause disparaît et la carte annonce « Repos terminé ✓ ». Après trois secondes dans une interface démarrée, elle ferme uniquement l’échéance attendue. Un ancien délai ne peut donc pas fermer un repos plus récent. Les boutons +30 s / −15 s fonctionnent aussi en pause ; un repos expiré ou fermé ne repart pas.

## Records et données

Les comparaisons exigent même programme, exercice, machine renseignée, réglages renseignés et unité de charge. Les répétitions sont comparées à charge égale. Une série de référence est nécessaire et les égalités ne créent pas de badge. Les séries non validées, séances en pause d’autres entraînements et lignes illisibles ne créent pas de record. Un historique incomplet désactive l’annonce.

Pour les exercices chronométrés, la durée remplace les répétitions et aucun record de charge n’est attribué. La bannière de six secondes suit l’écriture réussie, et les séries enregistrées affichent les badges calculés. Une correction remplace sa propre valeur dans la comparaison. Ces badges sont des comparaisons dérivées, pas un journal immuable de trophées ; corriger l’historique peut les modifier.

Les calculs sont mémoïsés sur les séries et l’historique terminé, sans recalcul complet à chaque frappe d’un brouillon actif. Aucune charge n’est augmentée automatiquement ; `Programs.canIncrease` reste l’unique règle de progression. Aucune migration ni changement du format des sauvegardes.

## Widget

Le fournisseur Android et sa déclaration XML sont vérifiés, ainsi que l’application des RemoteViews. Un ajout sur le vrai lanceur de l’émulateur QA complète le test de rendu : séance à reprendre/prochaine séance, objectif, actualisation et ouverture de la séance. Le format par défaut a été compacté après inspection du premier rendu, qui réservait trop d’espace vertical.

L’observation pour le widget porte sur la table des séances sans les brouillons, puis dédoublonne le modèle affiché. Le récepteur relit les données après les transactions ; il ne réutilise pas un ancien payload transmis par l’interface. Le bouton Actualiser, les événements de date et le rafraîchissement Android périodique couvrent le fonctionnement sans application ouverte. Android peut différer les mises à jour périodiques en veille ; ce widget n’est pas une minuterie.

Un historique illisible affiche « Voir mes séances » et un compteur partiel explicitement annoncé. Aucun poids, masse grasse ou détail d’une série ne figure sur le widget.

## Portée

Les tests instrumentés portent sur le build debug, sur l’émulateur Android 11/API 30 Sport_QA. Le paquet optimisé signé avec la clé de test historique est vérifié séparément. Aucun téléphone physique testé ; permissions Android 13+, politique de verrouillage, DND, vibrations réelles et rendu du lanceur constructeur restent à contrôler.

## Vérification du paquet optimisé

APK de **6 774 448 octets**, SHA-256 `4cdee765fd09b3868145be3ca85b0bc159a2f4816b64ed83f3a9aaf23a9862a9`. Le fichier installé a été relu et son empreinte comparée au livrable signé. Démarrage sans exception fatale, fournisseur de widget et retour à la séance vérifiés.

Sur le verrouillage réel du QA (`Keyguard showing=true`), les captures successives du décompte natif affichent **01:55 puis 01:51**, sans retour à l’application : [première image](screenshots/repos-notification-verrouillee-0.8.0.png), [quatre secondes après](screenshots/repos-notification-verrouillee-suite-0.8.0.png). Le verrouillage sans code a été temporairement activé sur cet émulateur puis son réglage initial rétabli ; aucun réglage du téléphone personnel n’a été modifié.

Le lanceur propose le widget en 4 × 2 ; le redimensionnement d’une première instance QA, l’actualisation sans quitter l’accueil et l’ouverture de la séance ont été contrôlés. Rendu normal et à 130 % inspectés : [widget](screenshots/widget-android-0.8.0.png), [grande police](screenshots/widget-android-grande-police-0.8.0.png). Preuve structurée : `optimized-smoke-0.8.0.json`.

Sources techniques : [chronomètre de notification](https://developer.android.com/reference/android/app/Notification.Builder), [widgets Android](https://developer.android.com/develop/ui/views/appwidgets/overview), [mise à jour des widgets](https://developer.android.com/develop/ui/views/appwidgets/advanced).
