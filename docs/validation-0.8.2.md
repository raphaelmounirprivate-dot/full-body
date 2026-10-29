# Validation locale 0.8.2

28 octobre 2026. F4 : visuel de machine explicitement sélectionné par l’utilisateur. Les autres essais ne sont pas utilisés dans l’application.

## Essais exécutés

- Build debug, APK de test Android, APK optimisé et AAB : réussis.
- 45 tests JVM réussis ; 9 contrôles du guide et 4 contrôles du contenu réussis.
- 1 test Android ciblé : fiche F4, ressource de machine, légende et attribution au PDF original. Rejoué à 130 % de texte avec succès.
- Total : **59 tests distincts réussis**, plus un passage répété en grande police. La suite Android complète de la version 0.8.1 n’a pas été rejouée pour cette correction visuelle.
- Lint debug : 0 erreur, 12 avertissements, 2 indications.
- APK optimisé installé séparément sur Sport_QA, Android 11 / API 30. Catalogue, fiche et zoom ouverts ; empreinte de la ressource WebP embarquée vérifiée ; APK installé identique au fichier livré ; aucun crash dans le journal du processus.
- [Fiche](screenshots/leg-curl-machine-0.8.2.png), [zoom](screenshots/leg-curl-machine-zoom-0.8.2.png) et [grande police](screenshots/leg-curl-machine-grande-police-0.8.2.png) inspectés avec le visuel retenu par l’utilisateur. Ce choix éditorial n’est pas une validation de chaque détail mécanique du dessin.

[Tests JVM](test-results/legcurl-jvm-0.8.2.json) · [test Android](test-results/legcurl-android-0.8.2.txt) · [test à 130 %](test-results/legcurl-font130-0.8.2.txt) · [preuve de lancement optimisé](optimized-smoke-0.8.2.json).

## Artefact et limites

APK 0.8.2, code 12, 6,913,884 octets. SHA-256 : `210dc134daf7156175260dd85f89635a4703a781ed4abc85ff894c5c6ec3cda6`.

Même certificat de test que 0.8.1 : `040f1fd30560381354cac6610cc36828fbf6b4b7e255759cbb5c5aa179ad3a68`. Aucune validation sur téléphone physique revendiquée. Illustration générique de repérage ; géométrie et réglages à confronter à la machine réelle, pas une notice constructeur.

PDF original, empreinte, fiche extraite et `guide/F4.webp` conservés. Une correction distincte dans `illustration-overrides.json` change seulement l’image, la légende et la note de provenance affichées. Les autres fiches et l’ancien catalogue restent inchangés. Séries, charges, séances, Room v4 et formats JSON 5/CSV 3 inchangés. Le PDF exporté reste le document source et son ancien schéma sur chaise.

[Image et prompt](illustrations/F4-machine-prompt.md).

APK local prêt : `output/v0.8.2/Mon-Coach-Sport-0.8.2.apk`. Version 0.8.2 non publiée sur GitHub ; le téléchargement public reste en 0.8.1.
