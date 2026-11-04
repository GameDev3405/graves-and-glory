# How we animate: the pipeline

Paths are relative to the bannerbound checkout. Confirm line numbers in source before quoting them; they drift.

## Contents
1. Data flow
2. Clip sources and the editing loop
3. The clip JSON and tools/clips.py checks
4. Runtime: ClipLibrary, TroopMotion, MotionEngine, MotionBody, MotionStack
5. Rig space and sign conventions
6. Joints, handlib.limb() and bone headroom
7. attackVariants, preflight and Spec.java
8. In-game contact sheets (client harness only)
9. Verification commands

## 1. Data flow

```
art/clips/<rig>/<family>.bbmodel          the bare rig plus its animations (source of truth, edited only via anim_edit)
   | python tools/anim_edit.py open <family>     -> work/anim-edit/<file>.bbmodel with the real troop model and skins
   | (edit in Blockbench, Ctrl+S)
   | python tools/anim_edit.py apply             -> copies clips back, runs tools/clips.py, verifies
src/main/resources/assets/gravesandglory/clips/<rig>/<family>/<clip>.json     what the game loads
   | ClipLibrary (client resource reload) -> TroopMotion.family() -> MotionEngine layers -> bones
```

Families with clips today on main: `biped/bow`, `biped/caster`, `biped/common` (hits, deaths, fall, raise) and
`biped/sword_shield`. The other families (sword, spear, axe, axe_shield, greatsword, scythe, crossbow, thrower, crew,
hound, spider, bear, ravager, colossus) are being authored on `anim/attack-clips`. Each family has a design spec and a
plan: `docs/superpowers/specs/2026-11-03-<family>-animation-design.md`, `plans/2026-11-03-<family>-animation.md`, and
the roadmap `specs/2026-11-03-animation-roadmap.md` (section 7 holds Kort's 2026-11-04 rulings).

## 2. Clip sources and the editing loop

`docs/ANIMATION-EDITING.md` is the user guide. Key points:

- `python tools/anim_edit.py open <family> [--troop <id>] [--fresh] [--launch]` builds a working file with the
  troop's real model and skins. Re-running `open` keeps unapplied edits; `--fresh` discards them. It takes a rig
  argument for hound, spider and colossus rigs.
- The timeline snaps to 20 fps, one game tick. Rotation is degrees, position is pixels, both added to the bone's rest
  pose. Markers (`impact`) are lines in the Effects row and must sit exactly on a tick.
- Do not rename clips or bones; do not move or resize cubes in the clip project.
- `python tools/anim_edit.py apply` writes the clip JSON and runs the checks; fix what it reports, then commit.
- `python tools/anim_edit.py preview <family> <clip>` prints the motion-grade client command (see section 8).
- **Never run `art/clips/build_clips.py`.** It is the old generator and overwrites hand edits.
- **Never edit `art/clips/biped/*.bbmodel` in Blockbench directly**; they hold only the bare rig.
- Blockbench does not show the weapon, the shield board, the game's scale and hunch, or the engine layers (gait,
  breath, look, foot planting, cloth springs, carries). Judge the final look in game, never from Blockbench alone.

## 3. The clip JSON and tools/clips.py checks

`tools/clips.py` converts the bbmodel animations to JSON and checks them; `--verify` rebuilds in memory and compares
with the committed files (part of `tools/verify.py fast`). It refuses:

- a bone not in `art/rigs/<rig>.json`, Molang expressions, global rotation or quaternion keys, scale keys;
- keys or markers outside the clip, two keys at one time on a channel;
- an `impact` marker off a tick, or not equal to the attackVariants row's `impactTicks`;
- a melee (stab, slash, blunt) attack clip with no impact marker;
- any attack clip longer than `BattleMath.ATTACK_CLIP_MAX_TICKS` (40 ticks; a late joiner is sent a swing that long);
- a looping death; a death whose last frame leaves a pivot or box corner more than 0.1 px off the ground;
- a looping clip whose end differs from its start by more than 0.01.

On `anim/attack-clips` (674b97e0) it also refuses any attackVariants row of a family a troop uses that has no clip, and
`test-tools/standalone/test_attack_clips.py` asserts every troop resolves each attack and that new families never bend
an elbow backwards. The turn suite prints NO CLIP and fails instead of shooting a stand-in.

Interpolations: `linear`, `catmullrom`, `bezier`, `step`. Sampling replicates Blockbench 5.2.2 exactly (Clip.java does
the same), so what you scrub is what plays, before the engine layers.

Naming: `<family>_<move>` for attacks (the attackVariants id), `<id>_wall` for the version thrown from the wall or brace
hold, `hit_<zone>[_n]`, `death_<kind>[_n]`, and stance clips (`idle`, `guard`, `carry`, `shield`, `brace`, `wall`, `aim`).
A family folder overrides `common` by name.

## 4. Runtime

- **ClipLibrary.java**: loads every clip on client resource reload; a clip that fails to parse logs
  `[clips] <file>: <reason>` and is skipped. `variants(family)` lists attacks in attackVariants order. A variant with
  no clip is only logged at runtime; the build check is what makes it fail.
- **TroopMotion.java**: the client adapter. Per frame: builds MotionInput (time, limbs, head/body turn, stance from
  the server, newest swing and blow, death, seed), runs the engine on the troop's Family (its family folder over
  `common`), adds the pose to the bones, clamps joint limits (`limits()`), adds carries for what no clip holds (shield
  arm, bow turn, idleArms), scales root offsets to height. `build()` maps attackVariants rows to clips; a row with no
  clip gets index -1, and with no attacks the engine starts nothing (the 18-troop silent failure).
  `force()` plays one clip whole-body for the harness.
- **MotionEngine.java**: six summed layers per bone in order: locomotion, stance, life, action, look, secondary; feet
  planted after look. Weights ramp on render time with eased blends; frame-rate independent at 20/60/240 fps. Pure.
- **MotionBody.java**: procedural locomotion (walk/run gait, knees, hip sway, arm swing, elbows), life (breath,
  fidgets), hit reactions, look and secondary springs.
- **MotionStack.java**: the stance stack and action tracks. A change pushes a new entry that ramps in with a cosine
  ease; acc = acc (1 - w) + clip w from the bottom up.
- **MotionFeet.java**: stride solve and foot planting; `ground` only lifts soles out of the floor, it never drops a
  floating body (so a knees-up clip without root y keys hovers).

Consequence: a clip is never the final pose. Gait arm swing, hit overlays and look add on top. A hit overlay of +9 on a
+72 forearm made 81 degrees backwards. Design keys with the stack in mind and check in game.

## 5. Rig space and sign conventions

ModelPart convention, as in `MotionBody` and `tools/clips.py`: **y is down, -z is forward**, rotations are added to the
rest rotation in ZYX order, degrees.

| Channel | Sign | Means |
|---|---|---|
| arm / leg rx | negative | swings forward |
| **forearm rx** | **negative** | **elbow flexes (correct). Positive is a backwards, hyperextended elbow. Never key it.** |
| shin rx | positive | knee bends the right way. Negative is a backwards knee. |
| body rx | positive | leans forward |
| head rx | positive | looks down |
| root ty | positive | body drops (an impact sinks with +y; -y rises and kills the weight) |
| root tz | negative | body moves forward (a lunge) |

Useful targets from the 2026-11-04 review: overhead wind-up arm about -150, forearm about -100, hand about +20 (tip
behind and down over the shoulder); impact arm -80, forearm -15, hand -10 (tip forward at head height); bow draw elbow
-110 to -130 with the upper arm yawed out.

Leaning troops (wm_seer 30, wm_hunter and wm_tusk 20, tc_raider 16, revenant 15, bonebow and stalker 8, reaper 7,
hexer 9) have a rest body pitch, so arm keys authored on the upright knight point down by the lean. Check aim and strike
keys on a leaner, not only on the knight; apply one rule (compensate in the clip, in the engine, or bake the lean into
the root) consistently.

## 6. Joints, handlib.limb() and bone headroom

`art/hand/common/handlib.py`: each 4x12x4 biped limb is two 4x6x4 boxes on two bones. `ELBOW` is (-1, 4, 0) on the
right arm and (1, 4, 0) on the left, `KNEE` (0, 6, 0), `HAND` (0, 4, 0) under the forearm.
`limb(name, parent, pivot, origin, paint, lower, joint, size, dil, sheet, limit=None, upper_h=6)` returns the upper and
lower bones, painted from one painter (Box `cut`). `limit=[min, max]` degrees of pitch is clamped at runtime by
`TroopMotion.limits`; `handlib.JOINT_LIMITS` defaults every forearm to [-150, 0] and shin to [0, 150] unless the model
sets its own (Ironclad knees [0, 120], mage shins [0, 10]).

Known joint problems and fixes:
- The hinge on the cut plane opens a notch past about 30 degrees. Extend the lower box 1-2 px up inside the upper box
  (same UV cut), and optionally move the knee hinge 1 px forward.
- Rigid gear spanning a joint floats when it bends (tc_tideguard elbow wrap, stalker arm wrap, thigh skirts past the
  knee). Split it at the joint or end it above the joint, or give the troop a tighter limit.
- **64 bones maximum** (`TroopModel.MAX_BONES`; the client refuses a bigger model). tc_admiral is at 64, ic_ballista 63,
  revenant 62. Merge static gear into parent bones before adding any. The handlib build guard covers only hand-built
  troops; check generated and Blockbench-built troops yourself.

## 7. attackVariants, preflight and Spec.java

`sheets/design.json` `attackVariants` rows: `{id, family, zone (head|body|legs), kind (stab|slash|blunt|...),
impactTicks, weight}`. The engine cycles a family's attacks in this order. `python tools/preflight.py` validates the
sheet (family used by a troop, impactTicks a whole tick, cooldown at least impactTicks + the late-swing margin) and
**generates `Spec.java`. Never hand-edit Spec.java**; save its hash before and after.

impactTicks, clip length, bow cooldowns and stance changes are gameplay. On 2026-11-04 the animation rework (bow
cooldown +25%, shield and crawl changes, hit weight) flipped Siege attacker wins from 4/6 to 0/6 with no single cause
(`docs/reports/mode-gate-timing-2026-11-04.md`). Do not change timing without Kort's decision, and after any
timing-adjacent change run the mode-gate sims and report before and after.

## 8. In-game contact sheets (client harness only)

Never drive Minecraft with desktop computer-use. Use the harness, from isolated run roots, within the client ceiling,
never touching a client you did not start.

- **Turn runner:** `tools/ingame_check_run.sh <root> <suites>` only sets `GRAVES_PHASE4_CHECKS`. For the Turn suite use
  a copy that instead sets `GRAVES_PHASE2_CHECKS=turn`, `GRAVES_PHASE2_TURN`, `GRAVES_PHASE2_TURN_ATTACKS` and
  `GRAVES_PHASE2_TURN_ANGLES` (root `work/ingame-check/<root>`, shots in `<root>/run/screenshots/`). One run shoots one
  attack list, so a second list (the knight overhead) needs a second run. `GRAVES_PHASE2_TURN_DIST` sets the camera
  distance in blocks (default 3); big troops need more (ic_colossus 4.2 keeps the maul head in frame).
- **Turn suite** (`GRAVES_PHASE2_CHECKS=turn`, troops from `GRAVES_PHASE2_TURN` = a House id or comma list): each troop
  frozen through `TroopMotion.force` at 8 angles (0 front, 90 weapon side, 180 back, 270 off side) in idle, attack
  peak or bow draw/loose, cast, walk at a quarter. Shots `p2-turn-TROOP-POSE-ANGLE.png`; weapon axes logged as
  `[p2-turn-fact]` lines. Options: `GRAVES_PHASE2_TURN_ATTACKS`, `GRAVES_PHASE2_TURN_ANGLES`, `GRAVES_PHASE2_TURN_FAR`,
  `GRAVES_PHASE2_STAGE`, `GRAVES_PHASE2_WEAPON`. See `test-tools/isolated-checks.gradle`.
- **Motion grade** (`GRAVES_PHASE2_CHECKS=motion-grade`, `GRAVES_MOTION_FAMILY`, `GRAVES_GRADE_TROOP`,
  `GRAVES_GRADE_PARTS=solo`, `GRAVES_GRADE_VIDEO_CLIP`): renders a family through all engine layers; sheets land in
  `test-tools/work/grade/<family>/<timestamp>/`. `python tools/anim_edit.py preview <family> <clip>` prints the full
  command. The grade video fails if it shows a different attack than the one named.
- Prior sheets for comparison: `docs/reports/art-qa-2026-11-04-ingame/` (facing-, review-, colour- per troop/House),
  `docs/review/2026-11-04-joints-contact.png` (on the anim-rework line).

A sheet a reviewer can use: the same camera relative to the troop's facing in every column; columns for wind-up, impact,
follow-through, idle and walk; front, 3/4 and a side view; troops labelled; NO CLIP printed where a clip is missing.
Assemble with a small Python/PIL script into the scratch folder; commit only the final sheet a report cites.

## 9. Verification commands

```
python tools/anim_edit.py apply          # clips JSON + checks
python tools/clips.py --verify           # committed JSON matches sources
python tools/preflight.py                # after any sheet edit; regenerates Spec.java
python tools/verify.py fast              # never launches Minecraft; includes clips --verify and motion unit checks
python tools/verify.py game              # client tier; defers while any client runs
```
Motion standalone checks live in `test-tools/standalone/Motion*Check.java`; in-game motion checks (`mo-*`) in
`test-tools/java/gg/gravesandglory/MotionChecks.java` (for example the hit-over-attack limit of 15 degrees per tick).
