# Audit proposal A, measured in the engine

2026-11-01, branch claude/audit-measure-5b5fc8 (from cc1028b, Set A and arrows 1.0 / bolts 1.4 / cap 32). Measurement only: no shipped stat or rule changed.

**Method.** DuelSimulation, plan units-r4 regenerated with `python tools/balance_method.py plan-units --reps 4 --mirror-reps 4 --calib-reps 2` (720 fights, 612 non-mirror troop-vs-troop), map 0, world seed 4242, `-PsimFast=true -PduelSeed=7`, four headless servers on the same plan and seed (no Minecraft client). Scratch flags live in `test-tools/java/gg/gravesandglory/ScratchFlags.java` (name added to the forbidden list in `tools/release_config.py`; never in a player JAR):

- `-PscratchNoHurtCooldown=true`: `timeUntilRegen = 0` before every troop-typed blow (Fabric ALLOW_DAMAGE, which runs before the invulnerability check). It also fires for bleed ticks, which already handle the cooldown themselves.
- `-PscratchTroopBypassArmor=true`: troop-typed damage ignores armour and toughness (a -1024 modifier added on the first troop hit; the same effect as `troop` in `bypasses_armor`).
- Always on: counts each queued damaging arrow, trident or bolt per shooting troop, and each one that left the queue before its landing tick with its target dead or removed.

Raw data: `work/method/{base,nohurt,noarmor,both}-*/duel/{duels,types,drops}.csv` (git-ignored, not committed). Tables come from `docs/reports/audit-measure-2026-11.py`. House win rate is over cross-House fights (n = 224 to 260 per cell, about +/-3 points standard error). The baseline reads Graveborn 60.0 / Gilded 39.3 against the handoff's 58.5 / 40.2 (a different tally of the same plan), inside noise. At most 2 fights per run ended at the cap; none aborted.

## House win % (band 40-60)

| House | Baseline | No hurt cooldown | Troop ignores armour | Both |
|---|---|---|---|---|
| Graveborn | 60.0 | 50.4 | 59.6 | 50.4 |
| Tidecallers | 54.9 | 38.8 | 64.3 | 48.2 |
| Ironclad | 45.0 | 59.2 | 39.6 | 51.9 |
| Gilded | 39.3 | 50.0 | 36.6 | 49.1 |

Pairings (row House win %; baseline / no cooldown / no armour / both):

- Graveborn v Tidecallers 55.0 / 66.2 / 43.8 / 55.0
- Graveborn v Ironclad 62.0 / 46.0 / 67.0 / 51.0
- Graveborn v Gilded 62.5 / 40.0 / 66.2 / 45.0
- Tidecallers v Ironclad 60.0 / 38.8 / 68.8 / 48.8
- Tidecallers v Gilded 60.9 / 45.3 / 68.8 / 51.6
- Ironclad v Gilded 58.8 / 63.8 / 56.2 / 56.2

## Per-troop win % (non-mirror fights; change from baseline in brackets; n = 68 each, about +/-6 points)

| Troop | House | Base | No cooldown | No armour | Both |
|---|---|---|---|---|---|
| archer | gilded | 27.9 | 50.0 (+22.1) | 30.9 (+2.9) | 66.2 (+38.2) |
| knight | gilded | 73.5 | 73.5 (0.0) | 70.6 (-2.9) | 64.7 (-8.8) |
| longbow | gilded | 14.7 | 57.4 (+42.6) | 19.1 (+4.4) | 57.4 (+42.6) |
| sentinel | gilded | 48.5 | 19.1 (-29.4) | 35.3 (-13.2) | 8.8 (-39.7) |
| bonebow | graveborn | 82.4 | 88.2 (+5.9) | 92.6 (+10.3) | 94.1 (+11.8) |
| hexer | graveborn | 20.6 | 16.2 (-4.4) | 20.6 (0.0) | 14.7 (-5.9) |
| reaper | graveborn | 83.8 | 36.8 (-47.1) | 54.4 (-29.4) | 32.4 (-51.5) |
| revenant | graveborn | 76.5 | 64.7 (-11.8) | 79.4 (+2.9) | 57.4 (-19.1) |
| stalker | graveborn | 25.0 | 45.6 (+20.6) | 39.7 (+14.7) | 52.9 (+27.9) |
| ic_arbalist | ironclad | 36.8 | 64.7 (+27.9) | 39.7 (+2.9) | 70.6 (+33.8) |
| ic_ballista | ironclad | 39.7 | 79.4 (+39.7) | 52.9 (+13.2) | 79.4 (+39.7) |
| ic_breaker | ironclad | 45.6 | 38.2 (-7.4) | 41.2 (-4.4) | 33.8 (-11.8) |
| ic_hearthguard | ironclad | 73.5 | 57.4 (-16.2) | 51.5 (-22.1) | 32.4 (-41.2) |
| ic_mortar | ironclad | 35.3 | 45.6 (+10.3) | 25.0 (-10.3) | 41.2 (+5.9) |
| tc_brute | tidecallers | 51.5 | 10.3 (-41.2) | 58.8 (+7.4) | 29.4 (-22.1) |
| tc_harpooner | tidecallers | 75.0 | 83.8 (+8.8) | 85.3 (+10.3) | 88.2 (+13.2) |
| tc_raider | tidecallers | 58.8 | 51.5 (-7.4) | 79.4 (+20.6) | 55.9 (-2.9) |
| tc_tideguard | tidecallers | 30.9 | 17.6 (-13.2) | 23.5 (-7.4) | 20.6 (-10.3) |

## Shots lost in flight (target dead or removed before landing)

| Troop | Fired (baseline) | Dropped (baseline) | Drop % base | Drop % no cooldown | Drop % no armour | Drop % both |
|---|---|---|---|---|---|---|
| archer | 10123 | 321 | 3.2 | 7.1 | 3.9 | 9.9 |
| bonebow | 16395 | 1208 | 7.4 | 16.8 | 8.6 | 19.5 |
| ic_arbalist | 6585 | 401 | 6.1 | 10.9 | 5.9 | 13.5 |
| ic_ballista | 3673 | 464 | 12.6 | 27.4 | 15.4 | 29.5 |
| longbow | 5496 | 185 | 3.4 | 9.4 | 3.5 | 10.5 |
| stalker | 6836 | 223 | 3.3 | 4.9 | 4.3 | 8.0 |
| tc_harpooner | 12534 | 1014 | 8.1 | 15.0 | 9.4 | 18.8 |
| all | 61642 | 3816 | 6.2 | 12.7 | 7.1 | 15.3 |

The other runs fire fewer shots because their fights end sooner, which is also why their drop rate is higher. Hexer and Mortar never appear: their shots do not go through the target-bearing queue path counted here, so the count does not cover them, nor the mage and seer. The landed/dropped split uses `b.time < lands` at the end of the tick, so a shot lost on its final tick can read as landed (a small share of drops).

## What the numbers say

- **Gilded's low rate is mostly the hurt cooldown.** Removing it lifts Gilded from 39.3 to 50.0 (+10.7, about 3 standard errors). The cooldown eats a large share of ranged and multi-hitter output: longbow +43, archer +22, arbalist +28, ballista +40, stalker +21 points. Graveborn's reaper drops 47 and Tidecallers' brute 41, because their edge was partly the cooldown shielding them.
- **Removing it overshoots.** Graveborn falls to 50.4, Ironclad rises to 59.2 (the band edge) and Tidecallers falls to 38.8, below the band. Gilded is fixed only by moving the others.
- **Armour is not the Gilded lever.** Ignoring armour moves Gilded 39.3 to 36.6 (inside noise, and the wrong direction: Sentinel and Knight armour helps Gilded more than it hurts). It lifts Tidecallers 9.4 to 64.3 and lowers Ironclad 5.4, so it is a real, House-specific lever there (raider +21, hearthguard -22).
- **Both together** put all four Houses in 48-52, the tightest spread, but that is a large rule change, not a small one.
- **In-flight drops** are 6.2% of all counted shots at baseline (ballista 12.6, harpooner 8.1, bonebow 7.4, archer and longbow about 3). They roughly double when every blow lands in full (shorter fights, more overkill).

## Which of proposals B-F the numbers support (Kort decides)

- **B (hurt-cooldown rule): strongly supported as a lever.** It is the largest effect measured and the only one that lifts Gilded into the band. Rule (i), zero before each blow, overshoots and breaks Tidecallers. A partial form (a shorter cooldown, or ranged hits only) would need its own measurement.
- **C (armour): not supported for Gilded.** Folding armour into health helps Tidecallers and hurts Ironclad and Gilded. It is worth doing for clarity (option ii, show the number), or if Tidecallers should come down.
- **D (magic through armour): not measurable here.** Hexer is flat (0.0 to -6.0, inside noise); mage, seer and mist were not in the fight set.
- **E (knockback): untested** by these runs; nothing here for or against.
- **F (retarget or refund dropped shots): weak.** 6% of shots at baseline and about 3% for Gilded's archer and longbow, so it would not move Gilded. Ballista (12.6%) and harpooner (8.1%) gain most.
- **G (show armour and effective HP): cheap insurance.** Armour changes individual troop win rates by tens of points (hearthguard, reaper, raider).

## Caveats

- One seed, one plan; n about 224-260 per House cell and about 68 per troop cell (+/-6 points). Gilded's +10.7 is well outside that; the small armour shifts are not.
- Fixed-size equal-gold duels in an open cell, not full battles with terrain, player abilities or commanders.
- Zeroing the cooldown also touches bleed and area hits; bleed already waits out the cooldown, so that effect is small but not zero.
