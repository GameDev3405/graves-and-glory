# In-game check, 2026-11-02 (branch claude/ingame-check-557622)

Driven through the Gradle harness only (`tools/ingame_check_run.sh <run> <suite>`); evidence under `work/ingame-check/<run>/` (gitignored: gradle.log, evidence/local-checks.txt, run/screenshots).

| Item | Result | Evidence |
|---|---|---|
| Campaign `hold_all` (march-1 Domination) | PASS | `bonus1..3`: tracker heldAll, win, bonus paid. Harness forced all points to Dawn after 60 ticks (not played out). |
| Campaign `kill_unit` (march-6 Siege) | PASS after two fixes | `bonus5`: Hollow King in the enemy army, struck down by the player's blow (harness did the damage), tracker killed, win, bonus paid. |
| Keep-door ward walls in Siege | PASS | `siege`: `p4-siege-wards/-sealed/-breach/-end` pass; walls stand beside the shut gate, stay after breach, cleaned at end. Shots p4-siege-wards, -gate, -breach, -keep. No dedicated keep-door shot exists; I read "keep-door wards" as the two yard wards. |
| Set A balance + arrow speed, bot battle per House pairing | NOT RUN | See blocker. |
| Briefing/ledger display of the bonus | PARTIAL | Result shot `p4-march-result-march-1.png`; no War Table march-page shot of the bonus text. |

## Defects found and fixed (commit 2f88e84)
1. `MarchMath.Tracker.killed()` was never fed: no kill_unit bonus could ever be earned. AFTER_DEATH now feeds it (GravesAndGlory.java).
2. The Hollow King was never fielded in march-6 (no botBanners row used him at the Normal tier). `Campaign.bossBanner` makes the enemy's first bot army use the botBanners row holding the target troop (graveborn_hard). Kort's ruling 2026-11-01 "Add him to march-6".

## Blocker
Bot battles via `runServer -PbotSim` need `eula.txt` (eula=true) in the server run dir; none exists on this machine. Accepting the Minecraft EULA is Kort's call, so I did not create it.

## Open
- Archers' arcs and hit rates under the new projectile speeds are unobserved in game.
