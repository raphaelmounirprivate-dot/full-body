# Suivi des fonctionnalités

État du développement 0.8.0, vérifié le 27 octobre 2026. Une case cochée décrit du code présent ; le rapport de validation distingue les essais réellement exécutés.

## Priorité séance

### Ajouts 0.8.0

- [x] Notification silencieuse de repos en cours avec chronomètre Android natif en décompte ; contenu générique visible sur l’écran verrouillé selon les réglages du téléphone.
- [x] Fin du repos : « Repos terminé ✓ », plus de bouton Pause à zéro, fermeture automatique après trois secondes visibles.
- [x] Retour vibratoire à la fin si les rappels sont activés, l’application visible et le téléphone interactif ; dédoublonnage partagé avec la notification système.
- [x] Ajustements +30 s / −15 s, en cours ou en pause, persistés et répercutés sur l’alarme et la notification ; aucun redémarrage d’un repos expiré.
- [x] Records personnels de charge, répétitions à charge identique, ou durée pour les exercices chronométrés. Machine, réglages, programme et unité identiques ; premier passage sans badge, historique incomplet sans suggestion.
- [x] Widget natif sur l’accueil Android : prochaine séance ou reprise, objectif hebdomadaire, actualisation et ouverture de la séance. Distinct des cartes organisables dans l’application.
- [x] Actualisation du widget après les changements pertinents, au changement de date et périodiquement selon Android ; pas à chaque frappe de brouillon.
- [ ] Vérifier le rendu de la notification verrouillée et du widget sur le téléphone physique et son lanceur.

- [x] Repos manuel et automatique, pause/reprise.
- [x] Notification locale avec alarme exacte si autorisée ; repli non exact explicite.
- [x] Accès aux réglages Android « Alarmes et rappels ».
- [x] Annulation des anciennes notifications et rejet des événements périmés ou dupliqués.
- [x] Réarmement des rappels futurs après redémarrage, mise à jour, changement d’heure ou autorisation.
- [x] Notification de repos ouvrant l’écran de séance.
- [x] Option écran maintenu allumé, limitée à l’écran de séance avec entraînement actif.
- [x] Mode guidé : prochaine série non validée, exercice suivant puis bilan.
- [x] Le mode guidé avance après l’écriture réussie ; les variantes, séries supplémentaires et options sautées sont respectées.
- [x] Retour au mode complet pour modifier une série ou terminer plus tôt.
- [ ] Vérifier la ponctualité et les permissions sur un téléphone physique Android 13 ou plus récent, y compris économie d’énergie et plusieurs repos successifs.
- [ ] Vérifier le réarmement par un redémarrage réel du téléphone. Les mécanismes existent ; ce test matériel reste distinct.

## Suivi et personnalisation

- [x] Historique, compteur des séances terminées et courbes des charges/répétitions existants conservés.
- [x] Poids et masse grasse facultatifs, saisis manuellement, datés ; modification et suppression.
- [x] Courbes des 30 dernières valeurs disponibles et historique daté ; aucune estimation automatique de masse grasse.
- [x] Rappel facultatif de fin de cycle ; alarme non exacte adaptée à une échéance de plusieurs semaines.
- [x] Niveau de profil Débutant / Avancé / Confirmé, sans modification automatique du programme.
- [x] Repère de niveau personnel configurable dans chaque fiche, séparé entre programmes ancien et corrigé.
- [x] Vidéo YouTube personnelle configurable pour chaque exercice, validée par domaine et format du lien.
- [x] Neuf démonstrations préassociées au guide corrigé, relevées sur les sites EGYM et Concept2.
- [ ] Compléter la sélection éditoriale des dix autres vidéos et la comparer avec le matériel réel de Jims Namur. La recherche YouTube et l’ajout d’un lien restent disponibles dans chaque fiche ; une recherche n’est pas présentée comme une vidéo vérifiée.
- [x] Accueil : afficher/masquer et déplacer les cartes statistiques, raccourcis, dernier entraînement et objectif ; organisation persistante.
- [x] Objectif hebdomadaire personnel facultatif : 10 points par séance réellement terminée, sans double comptage des identifiants ni points pour les séances en pause.
- [ ] Challenges de course, compétition ou meilleur score : version ultérieure ; aucun GPS, compte ou classement distant ajouté.

## Données

- [x] Room v4 avec migration explicite 3→4 ; migrations 1→2 et 2→3 conservées.
- [x] Table des mesures corporelles séparée, sans transformation des anciennes séances.
- [x] JSON normal format 5 : mesures, organisation, préférences et liens personnels ; récupération brute format 4 toujours refusée à l’import.
- [x] Imports JSON 1/2/3 conservés. Ancienne sauvegarde ou CSV : les mesures corporelles existantes sont conservées.
- [x] Toute nouvelle donnée d’import est validée avant le remplacement transactionnel des tables.
- [x] Une restauration ne réactive pas le rappel de cycle ; les autorisations restent propres au téléphone.
- [x] « Effacer toutes mes données » supprime aussi les mesures et les nouvelles préférences.

## Sources techniques et vidéos

- [Alarmes Android](https://developer.android.com/develop/background-work/services/alarms) : permission spéciale pour les alarmes exactes, repli en cas de refus ; Android garde des restrictions en veille et selon le constructeur.
- [Écran allumé](https://developer.android.com/develop/background-work/background-tasks/awake/screen-on) : maintien limité à une interface visible.
- [Démonstrations EGYM](https://sites.google.com/egym.de/egymus/home/egym-workouts/equipment-operations/equipment-setup-videos) : réglages sur des appareils EGYM, pas identification des machines du club.
- [Technique Concept2](https://www.concept2.biz/technique-videos) : lien rameur qIUio2uC2lw.

Les liens externes nécessitent Internet dans YouTube ou le navigateur. Le carnet, les illustrations, le guide et les nouvelles mesures restent hors ligne ; aucune permission Internet ajoutée à l’application.
