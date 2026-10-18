# Phase 1 playtest gate: Graves and Glory 0.4.0

For Kort. Three fights (about 45 minutes), then three questions. Phase 1, the solo game, closes only when you say it feels good.

**Setup.** Import `dist/graves-and-glory-0.4.0.mrpack` into Prism the way you did with 0.3.7 (or put `dist/graves-and-glory-0.4.0.zip` into a Fabric 1.20.1 instance) and start it. A fresh camp is fine. Keep your own key bindings: the lessons and hints name the keys you have bound. In camp, R (your menu key) opens the camp guide.

Known, so no need to note them: the hunger bar and the plain battle HUD stay until the UI plan brings the new HUD, its sun and moon glyphs and no hunger; vanilla advancement lines still appear in chat (seen since 0.3.7).

## Skirmish 1: The Long March, battle 1, "The Training Field" (Easy)

Camp guide, **THE LONG MARCH / BATTLE 1**: you against one Easy bot on Rivergate Valley.

| You should see | Yes | No |
|---|:-:|:-:|
| A briefing card during the opening camera: the battle's name, its orders and why it is fought. | [ ] | [ ] |
| Four lessons, one at a time, each naming your own key: send a group with Move, change formation, attack, recruit at your barracks lectern. | [ ] | [ ] |
| Arrows visibly in the air before they land, and puffs where they hit in the shooter's colour (cyan Dawn, purple Dusk). | [ ] | [ ] |
| Your group card flashing when your troops take hits. | [ ] | [ ] |
| At least one enemy group breaking and running, with a horn. | [ ] | [ ] |
| Recruits marching out of Trade Crossing (B, the centre) instead of your base once it has been yours for 10 s with no enemy within 20 blocks; the action bar reads "... trained: deploying at Trade Crossing". | [ ] | [ ] |
| The report after the battle: your kills, losses, gold and seconds on points. | [ ] | [ ] |

## Skirmish 2: solo ready pad, 3v3 on Normal, your map vote

Camp guide: set **Bots** to Normal. At the War Gates stand alone on the **DAWN** pad. The countdown runs 10 s ("War gates: 10s / Step off to cancel"); during it, open the camp guide, **Ready gates & map voting**, and vote for any map. Five bots fill the 3v3.

| You should see | Yes | No |
|---|:-:|:-:|
| Your own line routing at least once: cut below a quarter of its strength, or attacked from the side or rear by three or more troops while under 60%. A horn sounds, its group card reads ROUTING and the troops run for home. | [ ] | [ ] |
| It regrouping within 15 s, with the rally horn. | [ ] | [ ] |
| An order you give the routing group (Hold, or Move somewhere) being what it does once it regroups. | [ ] | [ ] |
| Bots that lose troops buying replacements with their gold and sending them back in. | [ ] | [ ] |
| The match readable from the HUD without colour: each point with its holder in words (A: Dawn, B: Contested) and the score as DAWN 120 / 300. The sun and moon glyphs come with the UI plan, not in this build. | [ ] | [ ] |

## Skirmish 3: solo ready pad, 3v3 on Hard, Kingsfield Crossroads

Set **Bots** to Hard. Stand alone on the **DUSK** pad this time, so the enemy Hard bots are Dawn, whose knights carry shields, and vote for Kingsfield Crossroads (five points).

| You should see | Yes | No |
|---|:-:|:-:|
| Dawn's Hard knights raising a shieldwall when three or more of your Bone Rangers are within 30 blocks of their army and none of your revenants is within 8. | [ ] | [ ] |
| Hard charges starting close, within about 18 blocks, with the charge horn, not from across the map. | [ ] | [ ] |
| Hard harder to beat than the Normal bots of Skirmish 2. | [ ] | [ ] |
| The report naming an MVP (a crest and "MVP" on the best row). | [ ] | [ ] |

Hard no longer flanks (your call, 2026-10-18). The same switch stopped it leaving garrisons on the points it takes, so it picks points the way Normal does: do not expect it to hunt your weakest point.

## Your verdict (write freely)

1. Did it feel good?
2. Which fight was hardest to read?
3. Anything that felt unfair?

The Phase 1 gate passes only when your answer to 1 is yes. Every other note becomes a Phase 1 fix or a line in `docs/PARKED.md`.

## Appendix: the numbers behind "the walk is shorter" and "Hard is harder"

**The walk.** One knight bought on Rivergate Valley, timed from purchase until he stands in the army's formation 40 blocks past Trade Crossing: 83.8 s from the barracks, 13.4 s once Trade Crossing had been held for 10 s and he deployed there (Task 3, `work/p1/reinforce-191613`). The 0.4.0 regression measured it again (`work/p1/regression-112412`):

```
[p1] reinforce-walk before=84.2s after=13.5s
```

**The bots.** Bot against bot, 3v3, maps 0 to 4 in turn, 300 s rounds, 10 matches a line, on the code of commit 50121f5, before the shield timing fix 186bdaa, so read them as close to this build, not exact. Hard as it ships (no flanking, experiment A), then Easy, each against Normal from both seats:

```
[botsim] matches=10 dawn(hard)=7 dusk(normal)=3 draws=0 dawnWin=70% meanGoldEarnedAtEnd dawn=1678 dusk=1410
[botsim] matches=10 dawn(normal)=8 dusk(hard)=2 draws=0 dawnWin=80% meanGoldEarnedAtEnd dawn=1608 dusk=1428
[botsim] matches=10 dawn(easy)=9 dusk(normal)=1 draws=0 dawnWin=90% meanGoldEarnedAtEnd dawn=1664 dusk=1352
[botsim] matches=10 dawn(normal)=5 dusk(easy)=4 draws=1 dawnWin=50% meanGoldEarnedAtEnd dawn=1532 dusk=1479
```

What they say: Hard wins 9 of 20 against Normal, level rather than harder, and 0 of its 4 on Kingsfield Crossroads (with flanking it won 7 of 20, all 4 Kingsfield matches among them). Easy beats Normal 13 of 20 with one draw; Normal given only Easy's cautious charge (2.5 to 1, experiment B) won 9 of 20, so that is not why, and no knob tested explains it. The Dawn seat wins 15 of 20 Normal mirrors and 13 to 17 of every 20 in all six batches, more than any tier. You parked the tier ladder until the Phase 3 maps (2026-10-18) and Hard plays without flanking, so in Skirmish 3 "harder" is your judgement, not the sim's. Full table: `docs/QUESTIONS-FOR-KORT.md`, problem 5 follow-up; run roots `work/p1/botsim-5e-*` (the ladder), `work/p1/botsim-5f-A-*` and `work/p1/botsim-5f-B-*`.
