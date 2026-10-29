# Validation locale 0.9.0

29 octobre 2026. Rear Kick R1 à la place du hip thrust pour les nouvelles séances B ; anciennes séances conservées. Repères Chest Press et Low Row issus des photos et indication de majorité de matériel Technogym.

## Essais exécutés

- Build debug, APK de test Android, APK optimisé et AAB : réussis.
- Tests JVM : **51 réussis**, dont six nouveaux tests de catalogue, protocoles, préférences, séparation de l’historique et comptage des séries.
- Suite Android complète : **53 tests réussis**, dont trois nouveaux tests Rear Kick. Les trois tests rejoués à 130 % ont aussi réussi.
- Contrôles Python : 9 du guide et 4 du contenu, réussis. Sources PDF, empreintes et visuel F4 sélectionné inchangés.
- Total : **117 tests distincts réussis**, plus trois passages répétés en grande police.
- Lint debug : 0 erreur, 12 avertissements, 2 indications.

Le premier passage Android a révélé une attente incorrecte du test de reprise : le test attendait une notification entre deux instances Room. La préparation vérifie maintenant l’écriture, puis passe par la lecture transactionnelle de démarrage. Le passage final complet réussit.

## Scénarios vérifiés

- Nouvelle B avec alternative F11 → R1 explicite ; six exercices et deux séries par jambe.
- Reprise d’une ancienne B en pause : F11 et ses séries inchangés, même après un passage par A. Après sa fin, une nouvelle B reçoit R1.
- Retour manuel au hip thrust avant de commencer le mouvement ; préférence explicite permettant de conserver F11 pour les futures B.
- JSON, CSV et restauration de R1, avec conservation des anciennes données F11 ; import refusé si le catalogue fourni ne connaît pas R1, sans remplacement des données existantes.
- Aucune réutilisation des records/charges du hip thrust ; clés de vidéo et de niveau cycle:R1 reconnues, legacy:R1 refusée.
- Deux séries communes aux deux jambes comptées comme deux séries de travail du groupe, sans double comptage.
- Fiche R1 du catalogue et de la séance, sans fausse attribution au PDF ou image de hip thrust. Repères personnels de marque séparés des consignes source.

## Essai du paquet optimisé

Installation et lancement sur **Sport_QA / Android 11 API 30**. [Fiche Rear Kick](screenshots/rear-kick-fiche-0.9.0.png) et [séance B](screenshots/rear-kick-seance-b-0.9.0.png) inspectées. Saisie par jambe affichée ; aucun crash dans le journal du processus. APK installé identique au fichier livré.

Version 0.9.0, code 13, 6913884 octets. SHA-256 : `3f33828daee786040b9643b342b330b4d3a79db705b0f1a3539741de9c319bfe`.

Certificat de test inchangé : `040f1fd30560381354cac6610cc36828fbf6b4b7e255759cbb5c5aa179ad3a68`.

[Tests JVM](test-results/rearkick-jvm-0.9.0.json) · [suite Android](test-results/rearkick-android-0.9.0.txt) · [grande police](test-results/rearkick-font130-0.9.0.txt) · [preuve du paquet installé](optimized-smoke-0.9.0.json).

## Portée et limites

L’application et le fichier APK sont locaux. Aucune publication GitHub 0.9.0 n’a été effectuée dans cette étape. APK nommé : `output/v0.9.0/Mon-Coach-Sport-0.9.0.apk`.

Aucun essai sur téléphone physique ni validation du geste sportif ou des réglages réels. Les photos indiquent les plaques et la marque de certains postes, pas les charges ou tous les modèles. Le catalogue constructeur consulté ne confirme pas le modèle du Rear Kick photographié.

R1 est une adaptation personnelle distincte des fiches du guide ; les anciennes séances et le PDF restent inchangés. Room v4 et JSON 5/CSV 3 conservés. Les anciens formats restent importables ; les APK anciens ne connaissent pas le nouveau contenu R1. La fiche R1 est textuelle et ne réutilise pas une illustration de hip thrust.
