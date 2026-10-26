# In-match progression, Task 1 report

Commit: `3b50501` on `phase-2-houses` (worktree `bannerbound-p2`, BASE 4b9bfaf). 24 files, +2483/-572. Not pushed, not merged.
Message file: `bannerbound-p2/work/p2/t1/msg-final.txt`. Client roots: `work/p2/t1/suite-A-115936`, `suite-B-120534`.

## What changed
- **Sheets** (`sheets/design.json`, `tools/preflight.py`, generated `Spec.java`, `sheets/preflight.json`):
  - New `progression` (1 row), `levels` (3), `gearTiers` (3), `levelAbilities` (60, ids `<troop>_<level>`).
  - `evolutions` is now `levelNames` (id, troop, level, name; the same 40 rows).
  - `rarities` has `gearPrice2/3` (40/80, 60/120, 80/160, 100/200).
  - `abilities` has `status, taken, cleanse, rangedPenalty, tags` on every row. The four `onhit_slow` rows are `onhit_status` with status `slow`, same ticks, levels, cooldowns and pull. 60 new rows `<troop>_l2` and `<troop>_l3` (87 in all, appended after the old 27 so `Abilities`' first-mist lookup is unchanged).
  - `counters` has C1g, C3g, C15b, C15v, C14v, C18a2, C18b2, C18c2, C18d, C19, C20, C21.
  - `botCommand` has `gearMaxTier, gearReserveTroops, gearPriority`. `troopNotes` lost `level2/level3`.
  - Preflight: the evolution rules are gone. New rules are the ones listed in the plan, plus a granted-tag-read check, a status list and a banned-word scan of every `text`/`name` cell and troopNotes. The mechanics list now holds `onhit_status, onkill_heal, raise, rise, mark`. The conditions list now holds `target_slowed_or_wounded, target_wounded`.
- **Teardown** (every caller compiles at level 1):
  - `Profile.Owned.stage`, `lastStage` and the stage clamp are gone.
  - `Campaign.progression` and `RosterMath.progressionMultiplier(starPct,cap)` use stars only. `Campaign.stage` is gone.
  - `Unit.stage` is now `level, credits, weaponTier, armourTier`.
  - `ArmyMobs.levelName` and `named` read `levelNames`.
  - `ArmyCards.portrait(spec,w,a)`. The recruit `Card` has no stage and its title is the level-1 name.
  - `RecruitPages` takes the card lines from the `levelAbilities` rows' text.
  - `Abilities`: `onhit_status` with status slow.
  - `tools/release_config.py` forbids the four new harness classes.
- **Beyond the plan's file list (needed to keep things green):**
  - `TroopModels.Look.stage` is renamed `armourTier` (with `TroopModel` and ~25 test sites) so the plan's `\.stage\b` grep passes.
  - `CounterMath.cardRows` skips rows that use a granted tag (`levelOnly`). Without it the Templar card gained C15b, C15v and C14v lines.
  - `tools/build_skins.py` and `test_build_skins.py` read `levelNames` (otherwise the art build would silently see one stage).
  - `Phase1Checks` and `CampaignChecks` got the new constructor and signature.

## Verification
| Command | Result |
|---|---|
| Step 2, standalone checks against the old Spec (red) | javac fails: 37 errors, "cannot find symbol" for the new `Spec` tables |
| `python tools/preflight.py` | `Preflight clean: 4893 filled cells` |
| Deliberate failure (scratch edit of the sheet, restored) | preflight named the knight_2 mechanic (`levelAbilities/knight_2 ... keeps ... shield, not stance`), the rare gear price (`rarities/rare: gear prices ... tier III above tier II`), the non-rising kills, a missing level-3 row, a dead tag and the banned words |
| Standalone: javac of Spec, BattleMath, MatchStats, Progression, Profile, ProfileMigration, RosterMath, CounterMath + the three checks | BattleMathCheck 51, HousesCheck 268, ProfileMigrationCheck 31 checks passed |
| `python test-tools/standalone/test_build_skins.py`; `python tools/build_skins.py --verify` | 37 tests OK; 51 outputs verified |
| `test_release_integrity.py`, `test_modrinth_export.py`, `tools/economy_model.py` | OK; OK; all asserts pass |
| `gradle compileJava compileLocalTestTools --offline` | BUILD SUCCESSFUL |
| `gradle build --offline -x test` | BUILD SUCCESSFUL |
| `grep -rn "EVOLUTIONS\|stageName\|\.stage\b" src/main/java` (minus generated `Spec.java`) | no matches |
| `grep -rin "evolve\|stage " .../lang` | no matches |
| Client A, `gilded,gilded-art,chase,screens,migration` (root `suite-A-115936`) | 54 pass, 0 fail; `p2-gilded-stages`, `p2-troop-card`, `p2-cap`, `p2-card-layout`, `p2-migrate-*` pass |
| Client B, `graveborn,tidecallers,wildmoot,migration` (root `suite-B-120534`) | 65 pass, 0 fail; `p2-frostshot`, `p2-brine-longest`, `p2-harpoon-pull`, `p2-ensnare` pass; new `p2-profile-stage-ignored` passes (a profile with `"stage":3` loads, and saving writes no stage) |
| Screenshot `p2-chase-troop-card.png` | The Templar card shows three level names (Templar / Templar Knight / Templar Commander) and three ability lines (base, "Better guard...", "Unbroken..."). The counter lines are the same six as before |
| `graphify update .` | updated (3542 nodes) |

Clients: 1 at a time (the other two on the machine were the integration and Phase 3 agents; no Kort process).
Suites were not run on the final commit text beyond the Phase2Checks change (the migration suite ran with it).

## Deviations and notes for later tasks
1. **Red-first was partial.** I made the Java teardown before the red run, so the red run was a compile failure against the old `Spec`, not assertion failures.
2. **Plan's standalone command** lists `HintText.java`, which does not exist. I dropped it.
3. **`HousesCheck` anchors assertion changed.** The Oath Banner now anchors on `oath_banner` and `gl_marshal_l2`.
4. **The plan's counter table cannot work as written for guards (Task 6, CounterMath).** `CounterMath.winners` keeps the largest multiplier of a replace group. C1g (0.30) and C3g (0.5) are lower than the rows they replace (C1 0.35, C3 0.6), so the base row would win. All other new rows are larger and work. Task 6 must change the rule, for example a more specific row wins.
5. **Rows are inert until Task 6.** Nothing reads `levelAbilities` yet, and no soldier carries a granted tag.
6. **Row shape guesses for Task 6/7 to adjust** (all values are in the sheet, nothing hard-coded):
   - `mage_l3` is a zero-heal `mend` with `cleanse` true.
   - `wm_seer_l3` is a timer `aura` with amounts 0 and `cleanse` true.
   - `ic_mortar_l3` (Firepot) and `tc_mist_l3` (Chill fog) are `mist` rows with `rangedPenalty` false. Firepot has windup 30, cooldown 100, min range 10, status bleed 20 ticks. Chill fog has cooldown 160, status weaken 20 ticks, and `amount` 0.15 as the weaken share.
   - Weaken rows keep 0.15 in `amount`.
   - `gb_hollowking_l3` is `rise`: amount 0.5 = health share, amount2 1.0 = its own chance.
   - `hexer_l3` is `raise`: amount 2 = the cap, 300 ticks.
   - `knight_l3` is a `slam` with radius 0, status stun 20 ticks, cooldown 100, knockback 1.0.
   - `tc_raider_l3` is a `pounce` with cooldown 100 (the design gives none).
   - `reaper_l3`, `hexer_l3` and `wm_hunter_l3` use trigger `onhit`.
7. **Docs not touched** (owned by later tasks): `HOW-TO-EDIT.md` lines 72 and 76 still name the `evolutions` sheet; the `build_skins.py` error text still says "evolution line".
8. **`TroopFeatures` comment** ("glowFrom stage") left for Task 9.
9. `tools/preflight.py` rule "tags only on stance/shield rows" was considered and left out so Task 6 may add tags to other mechanics.

## In-game checks owed
- Task 3's regression will rerun the remaining Task 1 suites (`graveborn`, `tidecallers`, `wildmoot` ran; `engine`, `banners`, `commander`, `ironclad` were not part of this task's list).
- Eyes-on check by a person of the recruit page card (`CampScreen.card`): title is the level-1 name, and no card says "stage".
- `p2-gilded-stages` now spawns troops by level name. Task 9 turns it into an armour-tier check.
