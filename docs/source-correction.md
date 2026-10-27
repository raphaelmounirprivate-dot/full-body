## Référence corrigée et présentation

Le document de référence est `Programme_Jims_Namur_images_uniformisees.pdf` : 31 pages, édition illustrée harmonisée du 25 octobre 2026. Depuis 0.6.0, l’original reste conservé sans modification dans `docs/sources/` ; une copie compressée est embarquée dans `app/src/main/assets/guide/`. Son texte et ses pages sont vérifiés contre l’original. Les empreintes distinctes sont dans `docs/asset-optimization.json`, les correspondances de source dans `docs/guide-source-mapping.json`.

Les 19 images sont extraites des pages 9–27 à leur cadrage d’origine puis adaptées en WebP. Noms, muscles, légendes, Installer/Bouger/Vérifier et mises en garde sont transcrits depuis chaque fiche. Les légendes du leg curl (schéma du geste, pas une notice de machine) et de la planche (deux variantes, pas deux phases) sont conservées. Le guide personnel précise que ses dessins sont générés et qu’il ne s’agit pas d’un document officiel du club.

Les règles générales des pages 1–3, le cardio p. 7, les remplacements p. 8 et le bilan p. 31 sont consultables dans l’application. Le PDF mobile complet est enregistrable depuis le bouton situé en bas de l’accueil. Le RIR 3–4 en S1–2 puis environ 2–3 est confirmé par la page 2.

Remplacements : F7/F8, presse ou leg extension dans le premier emplacement de B ; rowing assis au lieu du rowing avec appui ; développé incliné F10 en B ; écarté F16 en C ; crunch F19 à la place de la planche. Les conditions et les plages spécifiques sont affichées. Chaque remplacement prend un seul emplacement. Le leg extension ne remplace pas le leg curl. Pour une substitution sans plage différente chiffrée, les paramètres de l’emplacement remplacé sont conservés. La planche ne suggère jamais de hausse de charge.

L’ancien PDF de 8 pages et ses images servent uniquement à relire les anciennes séances. Programme, semaine, valeurs, unités et données ne sont pas effacés ni convertis. Les nouvelles séances se réfèrent au bon guide, y compris après restauration d’une ancienne sauvegarde.

Le visuel s’inspire de la maquette FITPRO fournie : bleu vif/bleu nuit, carte Full Body, raccourcis, catalogue compact avec vignettes et filtres musculaires, fiches illustrées plus larges. Les images proviennent du guide ; aucun contenu anatomique supplémentaire n’est inventé.

Contrôle : `python scripts/verify_guide.py`. Réimport : `python scripts/import_guide.py --source <chemin-du-guide>` avec pypdf, pypdfium2 et Pillow.
