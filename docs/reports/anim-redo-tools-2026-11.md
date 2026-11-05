# Anim redo tools: handoff (2026-11-05)

Branch `art/anim-redo-tools` in `C:/Users/kortt/Melty/bannerbound-animredo-tools`, from 74316ed7. Local commits only;
nothing pushed or merged. Offline only: Minecraft was not launched.

## Outcome

The `/animate-troop` workflow from `claude/gifted-ptolemy-j2rv7i` (2583b683) is ported by hand onto main and
generalised from the one Oathbound knight preview to every in-game troop. The knight's `sword_shield_overhead` is
re-authored with it as the proof (same id, 1.2 s, impact tick 10) and passes every gate.

| commit | what |
|---|---|
| 27acc2ed | `tools/animate_troop.py`, `tools/troop_anim/` (model, pose, solve, clearance, author, checks, render, recipes), `test-tools/standalone/test_troop_anim.py` |
| 38b313ff | settle pass (arms ease onto the stance; least-motion Euler forms), `ground` gate, pre-clamp sign lint |
| 00abf359 | proof: `art/clips/biped/sword_shield.bbmodel` (that one animation), its clip JSON, sheet and strip |
| 5f611ffd | `docs/ANIMATING-TROOPS.md`, `.claude/skills/animate-troop/SKILL.md`, pointer in `.claude/skills/troop-animation/SKILL.md`, help and docstring fixes |
| (this commit) | this report |

Not ported, per the brief: the player armour and weapon sets, their art, the Oathbound preview exports.

## What was generalised

- Any troop id loads as TroopModel draws it, with its weapon or shield at the game's grip transforms.
- Posing matches the game: stance(0) + clip(t) sampled like `Clip.java` (parity test to 1e-6), MotionFeet's ground
  lift, joint limits, carries (shield arm, idle arms, bow draw).
- Recipes are hand-agnostic (`weapon`/`off`, mirrored for left-handed troops) and timed from `sheets/design.json`.
- Family presets for overhead, chop, cleave, hook, thrust, jab, cut, hew, reap, sweep, volley, direct, loose, throw,
  cast, fire; two-handed variants for greatsword, scythe, spear.
- The writer replaces only the named animation in `art/clips/<rig>/<family>.bbmodel`, keeps uuid, length, loop, snapping
  and the other clips, and refuses any gate failure, any length or impact mismatch with design.json, or a missing
  animation. It then runs `tools/clips.py`.
- Review modes: `--sheet`, `--strip`, `--still`, `--check`, `--house`, `--author [--write]`, `--list`.

## Checks

- Knight `sword_shield_overhead` (committed): reach 0, sink 0, hover 0, skate 1.5 px/s, pop 12.4, jerk 12.8, clip weapon
  1.5 (the stance's own floor is 1.25 in the great helm) and shield 0.38, ground clear, ends 0, rate 59.0
  (right_arm), wrist 8.5, signs ok, ticks off 0, impact 10 = design 10, length 1.2.
- `tools/clips.py`: wrote 91, `--verify` 91 green; only that animation's JSON changed.
- `python -m unittest test-tools/standalone/test_troop_anim.py`: 20 tests OK (Java parity ran, not skipped).
- `python tools/verify.py fast` at 00abf359: pass (preflight, all Python tests, UI and skin assets, clips, Java
  compile and every *Check, gradle build); report `work/verify/20261105-021532-35184/result.json`. The later docs
  commits change no code (help text and docstrings only).

Proof sheets: `docs/review/anim-redo/tools-proof/knight_sword_shield_overhead_sheet.png` (key times, three views) and
`knight_sword_shield_overhead_strip_0.15-0.70.png` (every tick through the strike).

## Preset smoke run (bake and check only; nothing written)

| troop | clip | result |
|---|---|---|
| revenant | sword_overhead | jerk 32.8, clip 17, left_shin rate 82 |
| revenant | sword_thrust, sword_low_cut | fail several gates |
| ic_hearthguard | chop | pop 110, shield clip 66 |
| gb_hollowking | cleave, side_cut | fail several gates |
| wm_tusk | spear_thrust | fails |
| reaper | scythe_reap | fails |
| ic_breaker | haft_jab | fails |
| archer | bow_volley | ends 152 (the game's instant +90 draw on the hand is not in the stance pose), stance clip 30 at t 0 |
| tc_harpooner | throw | fails |
| mage | cast | skate 172 |
| ic_mortar | crew_fire | only shin rate 88.8 |
| ic_arbalist | crossbow_loose | refused: committed impact marker 0.35 s vs design 0.2 s |
| wm_hound | hound_bite | no preset (non-biped) |

The presets are starting points; each troop needs the tuning loop in the guide.

## Process notes

- The overhead tuning took more than two fix rounds counted strictly: structured sweeps over the key aims for clipping,
  then rate and wrist, then the ground tip. Two generic fixes (settle, ground gate) came out of it.
- Fixes found by the proof: elevation-dominant elbow bends, cocked wrist for long hilts, half-tick clearance, 0.25 s
  wind-up with a creeping hold, the ground gate, the pre-clamp sign lint, the settle pass.

## Known limits

- Only the knight overhead is tuned; no preset passes every gate yet.
- The push solver caps at 1.5 per level and bends authored arcs; poses should be clear before it runs.
- The knight stance has 1.25 px³ of blade in the helm and a 1.33 px left-foot hover; reported as stance floors.
- Bow clips: `ends` does not model the drawn carry, so bow presets fail it until it does.
- The arm roll switches from the bend plane to the hint at 2 degrees of bend; a near-straight arm can still roll.
- Parity covers clip sampling, not MotionFeet grounding, which pose.py reimplements.
- Non-biped rigs (hound, spider, colossus) have no presets.
- Nothing is seen in game yet: offline sheets are not acceptance.

## Needs the rig bones first

The waist (`chest`, parent body, pivot (0,-6,0); head and arms re-parented) and ankles (`right_foot`/`left_foot`, parent
shin, pivot (0,4,0)) come from the parallel worker. The code handles both rigs, but:

- `chest` poses fold into `body` and feet stay rigid until they merge; the proof was authored without them.
- After the merge, re-run `--check` on every committed clip (arms then hang from the chest), re-bake the knight proof,
  and extend the parity test to a rig with the new bones.
- The full redo should start after the merge, so the waist and ankle articulation is authored once.

## Decisions for Kort

- `ic_arbalist crossbow_loose`: committed impact marker 0.35 s against design 0.2 s; which is right.
- Whether the in-game review of the knight proof should wait for the waist and ankle bones.
