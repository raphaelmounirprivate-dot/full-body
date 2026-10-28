# Remise en forme, puis prise de muscle

Analyse du 28 octobre 2026. Objectif confirmé : remise en forme trois fois par semaine, puis prise de muscle. Les éventuelles douleurs, restrictions, le niveau et la tolérance réelle restent à préciser ; ceci ne constitue pas une validation individuelle du geste ou du matériel disponible.

## Bilan du programme du guide

La base est cohérente pour une reprise progressive : exercices guidés, travail des jambes, poussées, tirages horizontaux et verticaux, flexion du genou et extension de hanche au hip thrust. Machines et poulies conviennent à cet objectif. L’ACSM ne trouve pas de supériorité systématique d’un type de matériel pour l’adulte sain moyen ; le choix peut suivre la préférence, l’apprentissage et la régularité.

Un tour complet A/B/C à deux séries fournit 6 séries directes de pectoraux, 6 de quadriceps, 8 de dos, 4 de leg curl et 2 de hip thrust. Le tronc compte **2 séries en A seulement**, et non deux séances de planche. Les épaules ont 2 séries latérales facultatives en A et 2 séries de développé facultatives en C ; biceps/triceps ont chacun 2 séries facultatives en B. Ce sont des comptes directs par exercice, pas une mesure exacte de la sollicitation de chaque muscle. À deux séances par semaine, un tour ne représente plus une semaine.

La variété de poussée est limitée par les trois chest press ; varier les répétitions ne change pas le mouvement. Le programme n’inclut pas de charnière debout de type soulevé de terre ni de travail spécifique des mollets. Leur absence n’impose pas de les ajouter à ce stade. Le hip thrust travaille l’extension de hanche ; le leg curl a une autre fonction et ne remplace pas une charnière. Le tronc reçoit peu de travail direct : à réévaluer selon les priorités, sans empiler les exercices pendant la reprise.

## Adaptations disponibles dans l’application

Accueil → **Adapter mes prochaines séances**. Trois choix indépendants, désactivés par défaut en l’absence de choix antérieur :

| Séance | Variante après apprentissage | Prescription du guide |
|---|---|---|
| A | Crunch poulie haute F19 remplace la planche F6 | 2 × 12–15 ; repos 60–90 s |
| B | Développé incliné poulies F10 remplace la chest press F2 | 2 × 10–15 ; repos 90–120 s |
| C | Écarté poulies F16 remplace la chest press F2 | 2 × 10–15 ; repos 90 s |

Commencer par apprendre B/F10 avec un coach est une façon de diversifier la poussée tout en gardant la machine en A/C. C/F16 est un choix supplémentaire d’isolation, pas un développé équivalent. Garder les développés A et B. A/F19 change la fonction du travail du tronc sans en augmenter la fréquence. Il n’est pas nécessaire d’activer les trois choix ensemble. Les variantes de squat/hack et de rowing existantes restent disponibles et conservées.

Aucun mouvement n’est ajouté au plan de base. Un second travail du tronc ou une charnière supplémentaire nécessiterait une adaptation distincte du programme, après bilan de récupération ; cela reste à étudier. Les fiches et le PDF original restent inchangés.

## Progression

Trois séances espacées d’au moins un jour sans musculation, deux séries initiales, réserves et repos du guide. La semaine calendaire ne déclenche aucune augmentation de volume. Avant d’ajouter une série, l’interface montre le nombre réel de passages du même type et demande de confirmer un geste maîtrisé, un volume stabilisé et une bonne récupération. Ce sont des confirmations personnelles, pas un diagnostic algorithmique. Ajouter sur un seul mouvement, observer la séance suivante et garder charge/cardio stables. Les restrictions du guide en S5–6 et la stabilisation en S7–8 restent appliquées.

Pour l’hypertrophie ultérieure, l’ACSM donne environ dix séries hebdomadaires par groupe comme repère de volume, pas un minimum obligatoire pour obtenir des résultats. Aucune cible de dix séries ni hausse de charge n’est imposée par l’application.

La règle de charge conserve deux passages réussis comparables. Une autre plage de répétitions ne doit plus interrompre cette comparaison ; en revanche un échec sur une prescription comparable, un changement de machine/réglage/unité ou de nombre de séries, ou un historique incomplet bloque la suggestion. La charge reste toujours choisie manuellement.

## Activité réelle et cardio

La nouvelle carte compte les séries validées des séances terminées du cycle actuel sur les sept derniers jours glissants. Les séances en pause, en cours, anciennes ou futures sont exclues ; un historique illisible signale un total partiel. Elle montre aussi le cardio en salle enregistré. La marche extérieure n’est pas mesurée ni ajoutée automatiquement.

Le cardio du guide peut être complété progressivement par de la marche soutenue et les déplacements actifs. Le repère OMS pour les adultes est de 150–300 minutes modérées par semaine, ou un équivalent vigoureux ; une quantité inférieure reste bénéfique. Le repère inclut l’activité hors salle.

## Données et compatibilité

Les préférences ciblent seulement la création de nouvelles séances. Reprendre une séance active ou en pause conserve exactement son plan. Les séances terminées, séries, brouillons et records ne sont pas modifiés. Les préférences remplacent explicitement seulement les trois emplacements concernés ; les autres variantes sont conservées.

Room reste en v4, JSON complet v5 et CSV v3. `personal.preferredVariants` est facultatif et validé ; un JSON ancien dépourvu de ce champ conserve la préférence déjà présente sur le téléphone. Un objet vide explicite la réinitialise. Les anciennes applications ignorent cette nouvelle préférence ; les variantes effectivement effectuées restent stockées dans chaque séance avec le format existant. Le CSV garde les variantes de séance, pas ces préférences pour les prochaines séances.

## Sources consultées le 28 octobre 2026

- [ACSM, recommandations 2026](https://acsm.org/resistance-training-guidelines-update-2026/) : individualisation, régularité, matériel et volume selon l’objectif, chez l’adulte sain.
- [OMS Europe, activité physique](https://www.who.int/europe/news-room/fact-sheets/item/physical-activity) : activité totale, repères adultes et bénéfice d’une pratique inférieure aux repères.
- Guide personnel corrigé de 31 pages conservé dans `docs/sources/`, prescriptions et variantes déjà intégrées. Les adaptations sont des choix personnels parmi ces variantes, pas une nouvelle édition du guide.
