# The GeckoLib troop pipeline

Paths are relative to the bannerbound checkout. Builder docstrings are the detailed truth; read the one you touch.
Line numbers drift: confirm in source before quoting them.

## Contents
1. Data flow and files
2. Builder per troop
3. Rig space and bone ownership
4. Scorecard
5. Offline renderer
6. In-game harness (geo-knight)
7. Timing and Spec
8. Verification commands

## 1. Data flow and files

```
art/hand/<troop>/build_<troop>.py         the troop's hand-built art (boxes and paint), unchanged by animation work
  | python art/anim/common/troop_rig.py <troop>        re-hangs it on the knight skeleton
  |   (knight: python art/anim/knight/build_rig.py A; hound: art/anim/wm_hound/build_rig.py)
art/anim/<troop>/out/<troop>.geo.json, <troop>.rig.json, <troop>_t<tier>_<side>.png, preview/t<tier>.geo.json
  | <family builder> (section 2)                         keys every clip; writes the animation file
art/anim/<troop>/out/<troop>.animation.json  (knight: art/anim/knight/out/knight.animation.json)
  | python art/anim/common/export.py <troop>   (knight: python art/anim/knight/export.py A)
src/main/resources/assets/gravesandglory/geo/troop/<troop>.geo.json
                                         animations/troop/<troop>.animation.json
                                         troop_rig/<troop>.json
                                         textures/entity/troop/<troop>_geo/t<tier>_<side>.png
  | GeoTroopRenderer (if APPROVED, or PREVIEW with -Dgravesandglory.geoTroops=all)
  | GaitController / StanceController / AttackController / HitController / DeathController
```

Shared builder modules in `art/anim/common/`: `deaths.py` (the v4 twelve-fall system: `death_<dir>`, `_drop1`,
`_drop2`), `feel.py` (the polish scales for every biped builder: sway, hit-back, ready sway), `stances.py`,
`reactions.py`, `charge.py`, `heldprop.py`, `hang_swing.py`, `troop_rig.py`, `export.py`.

Clip names (see the knight's file): gaits and stances come in `<name>_legs` + `<name>_upper` pairs; each attack
`attack_<id>` has an `attack_<id>_legs` for the standing case; hits `hit_<weight>_<side>[_legs]`, `hit_block`,
`hit_knockback`, `hit_pulled`; deaths `death_<back|forward|left|right>[_drop1|_drop2]`.

## 2. Builder per troop

| Troops | Builder | Select the troop with |
|---|---|---|
| knight | `art/anim/knight/build_anims.py` | (default) |
| sentinel, gl_marshal, revenant, tc_raider, gb_hollowking | `art/anim/knight/build_anims.py` | `GG_ANIM_TROOP=<troop>` |
| ic_hearthguard | `art/anim/axe/build_axe_anims.py` | `GG_AXE_TROOP=<troop>` (default) |
| archer, longbow, wm_hunter, bonebow, stalker | `art/anim/archer/build_bow_anims.py` | `GG_BOW_TROOP=<troop>` |
| ic_arbalist | `art/anim/crossbow/build_crossbow_anims.py` | `GG_XBOW_TROOP=<troop>` |
| tc_harpooner | `art/anim/thrower/build_thrower_anims.py` | `GG_THROW_TROOP=<troop>` |
| wm_tusk, tc_tideguard, tc_admiral | `art/anim/spear/build_spear_anims.py` | `GG_SPEAR_TROOP=<troop>` |
| ic_breaker | `art/anim/greataxe/build_greataxe_anims.py` | `GG_GREATAXE_TROOP=<troop>` |
| mage, hexer, tc_mist, wm_seer | `art/anim/caster/build_caster_anims.py` | `GG_CAST_TROOP=<troop>` |
| wm_hound | `art/anim/wm_hound/build_wm_hound_anims.py` | (own rig) |

Every family builder imports the knight builder for gait, leg IK, stance legs, hit footwork and deaths, so a change in
`art/anim/knight/build_anims.py` or `art/anim/common/` can move every troop. Check this list against the builders'
`TROOP = os.environ.get(...)` lines and `ROLLOUT-PLAN.md`; group F troops (wm_stalker, wm_oldhorn, tc_brute,
ic_ballista, ic_mortar, ic_colossus, reaper) have no GeckoLib set yet and are new work.

Builder commands here use Git Bash's `VAR=x cmd` form. In PowerShell: `$env:GG_ANIM_TROOP = 'sentinel'; python
art/anim/knight/build_anims.py` (then `Remove-Item Env:GG_ANIM_TROOP`).

Useful switches (knight builder): `--check` prints each gait clip's foot-contact drift; `GG_V2=0` builds the
pre-v2 style; `GG_V2_OUT=<path>` redirects the output (for an A/B reel without touching `out/`).

## 3. Rig space and bone ownership

From the knight builder's docstring (checked numerically against render_geo): world frame in blocks, +Z forward,
+Y up, the troop's right is -X; 1 px = 1/16 block.

| Bone, channel | Positive means |
|---|---|
| leg x | foot swings back |
| shin x | knee bends (correct direction) |
| foot x | toe points down |
| hips y | turns toward its right |
| arm x | negative raises the hand forward |
| arm z | right hand swings out |
| head y | turns toward its right |

Gaits are solved, not hand keyed: a foot plan goes through inverse kinematics on render_geo's own bone maths, so a
planted foot stays put. Spears, bows and two-handed grips are solved per sample (fist positions, aim line, string on
the nock). Prefer changing a plan or solve target over adding raw keys.

Bone ownership (PLAN.md, no two writers per bone): `legs` controller owns root, hips, legs; `upper` owns torso,
chest, arms, item and shield bones; procedural head look owns head yaw/pitch only; the attack layer blends over the
upper bones; death takes the whole body. A clip that keys a bone its controller does not own is a bug.

Keys are thinned at build time (a key is kept only if dropping it moves the pose more than 0.02 deg or 0.002 px).
Do not bake dense keys by hand: dense clips cost frame time (PLAN.md, Performance).

## 4. Scorecard

`python tools/anim/scorecard.py --troop <troop> [--json <out>]` or `--all`. Exit 0 all pass, 1 any fail, 2 usage.
It reads the exported assets, so export first. Limits in `tools/anim/scorecard_limits.json` are derived from the
approved Knight v2 (`--derive` recomputes them; never re-derive to make a troop pass). Metrics: `elbow_bend`,
`knee_bend` (negative = hyperextension), `ang_speed_<group>`, `foot_slide`, `pen_weapon` and `pen_limb` (px inside
the body), `edge_lead` (slash edge leads the tip's travel), `impact_tick` (contact vs design `impactTicks`),
`loop_pose`/`loop_pos`/`loop_vel` (looping clips close). A metric that does not apply is `n/a`, never pass.
`booth.py build` runs it and refuses any failing batch.

## 5. Offline renderer

`tools/anim/render_geo.py` mirrors GeckoLib 4.8.4 on Minecraft 1.20.1 (sources cited in its docstring).

```
A=src/main/resources/assets/gravesandglory
python tools/anim/render_geo.py --geo $A/geo/troop/<troop>.geo.json \
  --texture $A/textures/entity/troop/<troop>_geo/t3_crimson.png \
  --anim $A/animations/troop/<troop>.animation.json --clip attack_overhead_legs \
  --anim $A/animations/troop/<troop>.animation.json --clip attack_overhead \
  --ticks 0:24 --views front,side,three_quarter --out work/render/<troop>-overhead
```
`--distance game16` for the 16-block read; `--speed <blocks/tick>` moves the root for gaits (walk about 0.16,
run 0.26 for the knight; the booth config holds each troop's value) and `--report` prints AABBs and foot slide;
`--tick` for one frame; `--bones` labels pivots. Always pair an upper clip with its legs clip, as the game does.
Write renders under `work/` (untracked); commit only what a review record cites.

## 6. In-game harness (geo-knight)

The suite (`test-tools/java/gg/gravesandglory/GeoKnightChecks.java`) places the troop, marches it, duels, hits and
kills it, and asserts impact sync, clip choice, weights and deaths. Run it from the lane worktree in Git Bash (as below),
within the client ceiling (five clients across all worktrees, at most 30 minutes each), with no other client running
from that checkout. Submit it through the shared queue rather than launching it yourself: `python tools/checkq.py submit client --root <troop>-r<N> --env GRAVES_PHASE1_CHECKS=geo-knight --env GRAVES_GEO_CHECK_TROOP=<troop> --env GRAVES_GEO_TROOPS=all`, then `python tools/checkq.py wait <id>` as a background shell command (docs/SETUP.md). The queue's `passed` means gradle exit 0 only; the harness pass criterion below (22 `[p1] p1-` lines, 0 `=false`) must still be checked in the log. What it runs:

```
export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot"
ROOT="$PWD/work/ingame-check/<troop>-r<N>"; mkdir -p "$ROOT/run" "$ROOT/evidence"
GRAVES_ROADMAP_TEST_ROOT="$ROOT" GRAVES_PHASE1_CHECKS=geo-knight GRAVES_GEO_CHECK_TROOP=<troop> GRAVES_GEO_TROOPS=all \
  ./gradlew.bat runClient -PlocalIntegration --offline -I test-tools/isolated-checks.gradle > "$ROOT/gradle.log" 2>&1
grep -c "^\[p1\] p1-" "$ROOT/gradle.log"; grep -c "=false" "$ROOT/gradle.log"
```
In PowerShell set each variable first (`$env:GRAVES_PHASE1_CHECKS = 'geo-knight'`, and so on), then run the same
`.\gradlew.bat` line. Pass: 22 `[p1] p1-` lines and 0 `=false`. `GRAVES_GEO_TROOPS=all` is needed for any troop still in `PREVIEW`.
Screens land under the run root. A loaded machine can fail a speed or sample-count check (see IC_BREAKER-REVIEW.md);
rerun once before diagnosing, and record both runs. Performance: `GRAVES_PHASE1_CHECKS=geo-perf` (PLAN.md).

## 7. Timing and Spec

`sheets/design.json` `attackVariants` rows hold each attack's `impactTicks`; the builder's `IMPACT` table writes the
clip's `impact` timeline marker from the same values, and the harness asserts marker = server delay. Clip length,
impact ticks and cooldowns are gameplay (they once flipped Siege results): change them only on Kort's decision. After
any sheet edit run `python tools/preflight.py`, which regenerates `Spec.java`; never hand-edit it.

## 8. Verification commands

```
python tools/anim/scorecard.py --troop <troop>     # exit 0
python -m unittest discover -s tools/anim          # the anim tool tests (scorecard, booth, render_geo)
python tools/verify.py fast                        # never launches Minecraft; NOT the anim tool tests (it runs test-tools/standalone)
python tools/verify.py game                        # client tier; defers while a client from this checkout runs
python tools/land.py <branch> --dry-run            # only when booth.py decide says land
```
