# Siege bots (task 3.3), 2026-11-01

Branch `claude/siege-bots-d7e12b`, base 19c7b4d. Not merged, not pushed.

## Result

Bot armies now follow a plan for each Siege stage on both sides. They switch plans as the stages advance, and every sim match finishes with the keep taken or the yard held. The `tools/mode_gate.py` balance band (attackers win 30–70%) **fails**: attackers win 18 of 20 across both banner seatings. What is left is Siege tuning, which is Kort's call (see Open questions). It is not a bot-plan defect.

## What bots do now

| Stage | Attackers (Dawn) | Defenders (Dusk) |
|---|---|---|
| 0 Yard | Plain order: attack the yard ring | Line up in the yard. On **Attack** around it, engaging within 24 instead of Hold's 10-block leash |
| 1 Gate, portcullis pending | Hold 10 blocks west of the gate | Infantry behind the passage; ranged at a ground post 8 blocks inside it (Normal and Hard) |
| 1 Gate, up | Infantry and cavalry strike the gate entity. Siege troops stand 25 off the face and shoot it (Easy sends them in with the infantry). Ranged stand 12 off and attack what they see | Infantry move to the breach at 25% gate HP; ranged stay at the posts |
| 2 Keep | Plain order: attack the keep ring | On Attack around the keep ring |

New hooks in `BattleMode`:
- `botPlan`: per-group orders, called at the end of `Campaign.botDecide` when the army is not retreating.
- `botRetreats`: Siege defenders never fall back once the yard has fallen.
- `lineup`: the point the lineup forms from.

## Rule change (Kort approved via AskUserQuestion, 2026-11-01)

These implement spec §5 lines 105, 115, 121 and 122 and the Phase 4 plan's `fieldBase`, `fieldBarracks` and `lineup` lines, which main had not built:
- Defenders respawn and have their barracks at the donjon (12, 34).
- The defenders' round-start lineup forms in the yard.
- Attackers respawn at their base, then at the yard once it falls, then at the gate's east mouth (−31.5, 0.5) after the breach.

Coordinates are code constants in `SiegeMode` (`DONJON_*`, `MOUTH_*`) until a `modeSites` base row takes them. Anchors were unchanged; they already matched the spec.

## Deviations from the plan

- **Gate-tower stations:** bots never climbed to the tower tops (feet 77). Defender ranged stood at the tower foot or in the passage. They now use ground posts behind the gate (`POST_IN` 8, `POST_SIDE` 4). The stations need reachable stairs or a navigation fix before they can be used.
- **Wards:** wards are treated as obstacles the attackers path around. No bot targets them.

## Draft values (in code, like Convoy's raidShare)

`BREACH_SHARE` .25, `STANDOFF` 25, `RANGED_STANDOFF` 12, `POST_IN` 8, `POST_SIDE` 4, `YARD_LINEUP` 12. `stations(tier)` is false on Easy.

## Sim runs

Headless `runServer` with `-PbotSim=10 -PsimMode=siege -PsimMaps=1 -PsimFast=true`, Normal against Normal, no client. Each figure is a mean over 10 matches. "Kills" is attacker kills / defender kills; the last three columns are the round second each stage fell.

| Run | Code | Banners (atk vs def) | Atk wins | Kills | Yard | Gate | Keep |
|---|---|---|---|---|---|---|---|
| 1 | plan only, defenders at the field base (+270) | gilded vs graveborn | 10/10 | 89/33 | 107 | 165 | 275 |
| 2 | same | ironclad vs graveborn | 10/10 | 105/42 | 151 | 178 | 359 |
| 3 | + donjon base and yard lineup | gilded vs graveborn | 10/10 | 105/15 | 149 | 217 | 297 |
| 4 | + no defender retreat after the yard | gilded vs graveborn | 10/10 | 118/14 | 146 | 211 | 292 |
| 5 | + defenders on Attack in yard and keep (final) | gilded vs graveborn | 10/10 | 127/32 | 174 | 243 | 341 |
| 6 | final | graveborn vs gilded | 8/10 | 119/82 | — | 240 | 325 |

Run 6's two defender wins held the yard to the clock.

What the runs found and fixed:
- Run 1: the defenders started at +270 and reached the yard after it had fallen.
- Run 3: after the yard fell, defender armies went into retreat at the donjon and never rallied, so they missed the gate and the keep.
- Run 4: on Hold the defenders were shot from outside their 10-block leash.

Final `mode_gate` output, run 5:

```
mode_gate siege matches=10 dawn=100% fail
stages 0=0 1=0 2=0 3=10
mean stage 3.00
attackers won 10 of 10, outside 30%-70%
```

Run 6:

```
mode_gate siege matches=10 dawn=80% fail
stages 0=2 1=0 2=0 3=8
mean stage 2.40
attackers won 8 of 10, outside 30%-70%
2 of 10 match(es) never got past the yard (stage 0)
```

In the final runs, attacker ranged troops are almost never lost. Attacker infantry are back to full strength within 10–20 s of a fight, while defender reinforcements trickle back from the donjon.

## Sim tooling

- `BotSimulation` adds `-PsimMode`, which is checked against `Modes.registered` and `Modes.hosts`. A non-Domination mode keeps its own clock unless `-PsimRoundSeconds` is given.
- `results.csv` gets the columns `mode,stage,gateBlows,gateDamage,yardSecond,gateSecond,keepSecond`.
- Under Siege, `botsim/siege.csv` records each army group's position and order every 5 s.
- `tools/mode_gate.py <results.csv> siege` grades a run, with exit codes 0, 1 and 2. `test-tools/standalone/test_mode_gate.py` holds 7 unit tests.

## Verification

- `compileJava` and `localTestToolsJar` compile.
- `./gradlew build compileLocalTestTools -x test --offline` succeeds.
- The standalone Python suite (`unittest discover -s test-tools/standalone`, 213 tests, including the 7 mode_gate tests) passes.
- After merging local main (e4c36d8, the per-checkout verify guard): `verify.py fast --offline` PASS and `verify.py game --offline` (the Phase 1 combat smoke) PASS, 2026-11-01.

## Open questions (for Kort, via the coordinator)

1. Siege leans to the attackers with both sides on these bot plans. Which knob should change? The candidates are gate HP (1600), yard hold seconds, the 600 s clock, or defender reinforcement speed (the donjon is about 70 blocks from the yard).
2. Should the gate-tower stations be made reachable (stairs or ladders in the gatehouse), or stay ground posts?
