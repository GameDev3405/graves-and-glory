# Animating troops

How to author, check and review a troop clip offline with `tools/animate_troop.py` and `tools/troop_anim/`. It is the
workflow behind Kort's 2026-11 direction to redo every troop animation from scratch. The project skill
`.claude/skills/animate-troop/SKILL.md` is the short procedure. `.claude/skills/troop-animation/` still holds Kort's
rules, the in-game review loop and the rubric; this guide does not replace them.

The method was first proven on one Oathbound knight preview (branch `claude/gifted-ptolemy-j2rv7i`). Its armour-cutting
rig, player sets and exports were not ported. What carried over is the authoring method, the clearance solver, the
gates and the lessons, rebuilt on the game's own troop models and clip pipeline. Proof: the gilded knight's
`sword_shield_overhead`, re-authored with this toolkit (`docs/review/anim-redo/tools-proof/`).

---

## 1. Pipeline

```
recipe (tools/troop_anim/recipes.py) ─> Timeline (chained poses, PCHIP/linear tracks)
   ─> Baker: per 0.05 s tick: torso -> leg IK -> weapon arm, off arm (aims or reach) -> clearance pushes -> head -> lags
   ─> settle (arms onto the stance, Euler forms that move least) ─> clip channels = net - stance(0)
   ─> checks (every gate on the clip as the game plays it) ─> refuse on any failure
   ─> write: art/clips/<rig>/<family>.bbmodel (this animation only) ─> tools/clips.py ─> tools/clips.py --verify
```

| file | role |
|---|---|
| `tools/troop_anim/model.py` | A troop as TroopModel draws it: bones, boxes, visibility, skin and cloth, the held weapon and shield with the game's grip transforms. Any troop id from `--list`. |
| `tools/troop_anim/pose.py` | A clip on a troop as TroopMotion poses it: stance(0) + clip(t) sampled like `Clip.java`, the feet's ground lift, joint limits, carries (shield arm, idle arms, bow draw). |
| `tools/troop_anim/solve.py` | Arm aims with a hinge elbow and a natural grip, Euler continuity, PCHIP, leg IK, head look. |
| `tools/troop_anim/clearance.py` | How far each held item is inside the body (px³) and under the ground (px), and the push directions. |
| `tools/troop_anim/author.py` | Recipe format, Timeline, Baker, settle, keyframes, bbmodel writer, `tools/clips.py` runner. |
| `tools/troop_anim/checks.py` | The gates (section 6). |
| `tools/troop_anim/render.py` | Software rasteriser for sheets, strips, stills and House tiles. |
| `tools/troop_anim/recipes.py` | Family presets and tuned recipes (`RECIPES`). |
| `test-tools/standalone/test_troop_anim.py` | `Clip.java` sampling parity, aims and IK, export round-trip, settle, gates. |

Commands (Python 3.13; unittest, not pytest):

```bash
python tools/animate_troop.py --list                                     # troops, family, weapon hand, attacks
python tools/animate_troop.py --check knight sword_shield_overhead       # gates on the committed clip
python tools/animate_troop.py --author knight sword_shield_overhead --out DIR   # bake a recipe, check, sheet + strip
python tools/animate_troop.py --author knight sword_shield_overhead --write      # ... and write it if it passes
python tools/animate_troop.py --sheet knight sword_shield_overhead 0 0.25 0.5 --out DIR [--recipe]
python tools/animate_troop.py --strip knight sword_shield_overhead 0.15 0.7 --step 0.05 --out DIR [--recipe]
python tools/animate_troop.py --still knight --clip guard --t 0 --out DIR
python tools/animate_troop.py --house gilded --out DIR [--clips attacks|all]
python -m unittest test-tools/standalone/test_troop_anim.py
python tools/verify.py fast
```

`--recipe` reviews the recipe baked in memory instead of the committed clip. `--author` without `--write` never
touches a file. With `--write` it refuses any recipe that fails a gate, whose length or impact marker differs from
`sheets/design.json`, or whose animation is missing from the bbmodel.

Never run the old `art/clips/build_clips.py` in write mode: it overwrites hand edits.

---

## 2. Conventions that must not drift

- **Game model space**: px, y down, -z forward, the troop's right at -x. Rotations in degrees, applied ZYX
  (R = Rz·Ry·Rx), added to the bone's rest rotation channel by channel.
- **What the clip stores**: the net channels minus the stance at t 0. The game plays stance.sample(0) + clip.sample(t),
  then lifts the root so the lowest sole sits on the ground (`MotionFeet`, SINK_TOL 0.25). The toolkit poses exactly
  that, and `test_troop_anim` checks the sampling against `Clip.java` to 1e-6.
- **Hinges**: a forearm's pitch must be 0 or negative and a shin's 0 or positive. Models with joint limits clamp them
  in game, which hides a backwards key as a frozen elbow; the sign lint reads the pitch before the clamp.
- **Keys on the 0.05 s tick.** The bake keys every tick. Clip ids, lengths and impact ticks come from
  `sheets/design.json` attackVariants and are gameplay: never change them without Kort.
- **Aims**: (elevation, azimuth) in the arm's parent frame (the chest, else the body). Elevation + is up, azimuth 0 is
  forward and + is outward on that arm's side. Past 90 the aim goes over the top: (120, 0) is up and 30 degrees back,
  and its azimuth then reads inward. Everything sideways is mirrored for a left-handed troop.

## 3. The rig and the bone contract

Today's biped models have rigid torsos and feet: no `chest`, no `*_foot`. Kort approved two additions (2026-11),
built by a separate worker:

| bone | parent | pivot | effect |
|---|---|---|---|
| `chest` | `body` | (0, -6, 0) | waist; the head and arms re-parent to it |
| `right_foot`, `left_foot` | the shin | (0, 4, 0) | ankle; the sole stays flat or rolls on the toe |

The toolkit works on both. Without `chest`, the `chest` field of a pose folds into `body`; without feet, leg IK keeps
the shin's lowest corner where the stance has it. Once the bones are merged, the same recipes drive them with no edits,
but every committed clip should be re-checked, because the arms then hang from the chest.

---

## 4. Authoring a clip

### Recipes are chained poses

A recipe is `{'clip', 'length', 'impact', 'loop', 'keys', 'push', 'lag', 'stance'}` (`stance` names the stance under
the clip; by default the game's own for it). Each key is a time and a partial pose; it keeps whatever it leaves out
from the key before, and the first key starts from the stance. The full field list is
the docstring of `tools/troop_anim/author.py`; the ones used most:

| field | what |
|---|---|
| `root` | (side, down, forward) px from the stance; down + lowers the hips into the blow |
| `hips`, `body`, `chest` | (pitch, yaw, roll) on the stance; pitch + leans forward, yaw + turns the weapon shoulder forward |
| `weapon`, `off` | arm aims `upper`, `fore`, `item` as (elevation, azimuth); `twist` from the natural grip; `on` + `grab` put the fist on the other hand's item (two-handed haft, bowstring) |
| `look` | gaze (elevation, azimuth) in model space, or `'stance'` (the head keeps the stance's gaze) |
| `feet` | `{'weapon'/'off': (out, forward, lift, toe)}` from the stance's plant |
| `interp` | `'pchip'` (default, never overshoots a hold) or `'linear'` (a committed strike) for the next segment |
| `extra` | `{bone: (rx, ry, rz)}` raw channel offsets added last (cloth, tails, a non-biped's parts) |
| key `stance` | `True`: this key is the stance exactly; once-clips start and end on one |

Times come from the design row (`recipes.timing`), snapped to the tick. `recipes.preset(troop, clip)` picks a family
preset by the last recognised word of the clip name (overhead, chop, cleave, hook, thrust, jab, cut, hew, reap, sweep,
volley, direct, loose, throw, cast, fire). Two-handed families (greatsword, scythe, spear) take the two-handed variants. A tuned recipe
goes in `RECIPES[troop][clip]`; that is what `--write` commits.

### What the bake does, per tick

1. Torso: root, hips, body, chest from their tracks.
2. Legs by IK: each sole's lowest corner stays where the stance planted it (less `lift`); the knee never bends back.
3. The weapon arm, then the off arm: aims solved to a hinge elbow (the bend plane sets the upper arm's roll; a straight
   arm keeps its last roll), the item laid on its aim with a natural grip, Euler continuity from the last tick. A
   `grab` arm reaches the other hand's item instead.
4. Head look.
5. Clearance pushes (section 5), solved at ticks and at the half tick before each, then the ticks re-run with them.
6. Settle: every closing segment eases the arm and hand onto the stance rotation, and each tick takes the Euler form
   (two solutions, whole turns) that moves the channels least, with pinned ticks fixed to the stance's own numbers.
7. Clip = net - stance(0); then lags (`'lag': {'off': 0.05}` delays the off arm a tick; `weapon_hand`,
   `off_forearm` and bone names also work); stance keys are stored as the stance's exact zero.

---

## 5. Keeping held items out of the body and the ground

- `clearance.Clipper` voxelises the held weapon (the game's item mesh at its grip transform), weapon bones and the
  shield, and tests every point against every body box in world space. It excludes the grip in the fist, the pommel
  against its own hand and forearm, and the shield against its strap arm.
- Whatever the stance itself has inside the body is the floor: a clip starts and ends there, so it cannot do better.
  The knight's stance has 1.25 px³ of blade in the great helm.
- **Fix poses first.** Bake with `--author`, read which item clips and when, then change the aims. A small grid
  search over the key aims (keep the clear one closest to the intent) beats raising the push.
- The push solver only mops up: per tick it tries push levels along fixed directions and keeps the smallest that
  brings each item under tolerance. Its pushes bend authored arcs, so a large push means the pose is wrong.
- `ground` measures how far an item's lowest point sits under the ground plane, less the stance's own.

---

## 6. Gates

`checks.measure` runs them all on the clip as the game plays it, at 60 fps.

| gate | how | limit | knight overhead today |
|---|---|---|---|
| reach | worst IK error (legs, grab) | < 0.3 px | 0.0 |
| sink, hover | lowest sole under or over the ground, planted feet | ≤ 0.15 px | 0.0, 0.0 |
| skate | planted foot speed | ≤ 8 px/s | 1.5 |
| pop | geodesic rotation of a bone between frames | ≤ 50 deg | 12.4 |
| jerk | change of that per-frame rotation | ≤ 30 deg | 12.8 |
| clip | item px³ inside the body, any frame | ≤ 1.5 | weapon 1.5 (stance floor), shield 0.38 |
| ground | item px under the ground, less the stance's | ≤ 0.5 px | clear |
| ends | once-clip first and last pose against the stance | < 1 deg | 0.0 |
| loop | last pose against the first; root | < 0.5 deg; 0.01 px | n/a |
| rate | peak per-axis change in any 100 ms window, linear between keys, graded bones | ≤ 60 deg | 59.0 (right_arm) |
| wrist | hand turn through the strike, models with a forearm | ≤ 40 deg | 8.5 |
| signs | forearm pitch > 0 or shin < 0, before the clamp | none | ok |
| ticks | keys off the 0.05 s tick | none | 0 |
| timing | impact marker and length against design.json | equal | impact 10 = 10, 1.2 s |

The stance's own left-foot hover (the knight's guard shows 1.33 px) is reported as `stance gap`, not gated: it is a
stance defect, not the clip's.

---

## 7. The review loop

1. `--author ... --out DIR`: numbers first, then the sheet at the key times and a strip around the strike.
2. `--sheet`: three views (3/4, profile, front) per time, with each item's clipping volume printed. Where most
   problems show: blades pointing at the camera, edge-on shields, pommels in the arm, tips in the ground.
3. `--strip FROM TO --step 0.05`: consecutive frames of the strike. Arc continuity and pops show here, not on a sheet.
4. `--check`: every gate on the committed clip. Stills lie about speed; always run it.
5. In game: offline sheets are not acceptance. Shoot the in-game contact sheet and run the reviewer loop in
   `.claude/skills/troop-animation/` before Kort sees it.

---

## 8. Timing principles that read well

- **Anticipation**: the wind-up holds a beat (0.1 to 0.25 s), loaded opposite to the strike. A moving hold (the top
  creeps a few degrees further back) reads better than a dead one.
- **Wind-up pacing**: the shoulder may not move more than 60 degrees in 100 ms. Out of a low guard into a high
  overhead top, 0.25 s with eased ends is about the shortest that passes.
- **Fast strikes**: 0.06 to 0.15 s, linear, edge first. About 35 degrees a frame at 60 fps reads as a strike; 66 was
  too fast to read.
- **Overlap**: lag the off arm (and, where it reads, the weapon hand) so the body breaks in order: hips, chest, arm,
  blade.
- **Weight**: drop the root at impact, bend the knees, carry the root forward; drive from the back foot.
- **Recoveries are slower than strikes**: at least 0.25 s, with an in-between key. 0.08 s looked like a glitch.
- **Once-clips start and end on the stance exactly**, so clips chain without pops.
- **Read every view**: a pose that reads in 3/4 can be nonsense in profile.

## 9. Traps (each cost a rework)

- Never interpolate arm Euler keys between far-apart poses. Interpolate aims and bake every tick.
- **Let elevation carry the elbow bend.** If the forearm differs from the upper arm mostly in azimuth, the bend plane
  is sideways and the upper arm rolls 80 to 90 degrees to follow it. Give the forearm a higher elevation than the upper
  arm and a similar azimuth.
- Keep the elbow bending to the same side between consecutive poses; crossing it forces a 180-degree roll.
- **Long hilts need a cocked wrist.** The knight's pommel reaches 4 to 6 px behind the grip; with the blade in line
  with or below the forearm it sinks into the upper arm. Keep the blade 20 to 35 degrees above the forearm.
- **Hold the wrist through the strike.** Keep item minus forearm elevation nearly constant from the strike key to
  impact, or the wrist gate trips.
- **Check between ticks.** The game interpolates channels between keys; a path between two clear ticks can cut
  through the helm. The push solver checks the half tick before each tick for that reason.
- **Low follow-throughs reach the ground.** A blade aimed 12 to 15 degrees down from a lowered root put the tip 2 px
  under the ground; aim it level or slightly up.
- A straight arm's roll is free, so the solver can come home rolled with the hand counter-rolled. The settle step
  eases arms onto the stance; do not undo it with a late key.
- Catmull-Rom cannot hold a planted foot (it overshoots between equal keys). Tracks are PCHIP.
- A clearance push that causes new clipping elsewhere means the pose is wrong; fix the pose.

## 10. What did not work, and the fix

| symptom | cause | fix |
|---|---|---|
| Arms swung 50 to 110 degrees in one frame | Catmull-Rom between far-apart Euler keys, branch changes | interpolate aims, solve every tick with branch continuity |
| Wrist spun 110 degrees in a rising cut | a keyed edge vector passed along the blade | natural grip plus a twist angle |
| Upper arm rolled 93 degrees mid-combo | the forearm crossed sides of the upper arm, so the bend plane swung 180 | bend to the same side; carry the last bend through a straight arm |
| Knight strike at 67 to 90 deg/100 ms | sideways bend at impact and follow-through rolled the upper arm | elevation-dominant bends (fore (-8, 2) under upper (-25, 6)) |
| Blade in the upper arm through the follow-through (5.5 px³) | wrist down, so the long pommel ran up the forearm | cocked wrist: item 20 to 35 degrees above the forearm |
| 2.4 px³ in the helm at 0.017 s, between two clean ticks | the path between ticks was never checked | half-tick clearance in the push solver |
| Wind-up at 63 deg/100 ms | 0.2 s from guard to a high top | top at T - 0.25 with a 5 degree creeping hold |
| Presets came home with 85 to 340 degree snaps | free arm roll and a second Euler form at the end | settle step in the bake |
| Tip 2.1 px under the ground in the follow-through | blade aimed down from a lowered root; no gate saw it | `ground` gate; blade level in the follow-through |
| Backwards elbow passed the sign lint | the model's joint limits clamp the net pose | lint the pitch before the clamp |
| Planted feet crept | Catmull-Rom overshoot | PCHIP |
| Euler smoothness test passed bad motion and failed good | gimbal regions inflate Euler steps | geodesic angle between frame rotations |
| A big automatic push bent the arcs | poses clipping by tens of px³ | fix poses at the source; the solver only mops up |
| High guard read as "a stab or a bunt" in profile | blade pointed at the camera across the face | blade cocked back over the shoulder |

---

## 11. Animating the next troop: checklist

1. `--list` to find the troop's family, weapon hand and attacks. Read the family's design row (impactTicks, length).
2. `--still TROOP --out DIR` and `--check TROOP` to see the stance and the committed clips as they are.
3. Start from `recipes.preset(troop, clip)`: `--author TROOP CLIP --out DIR`. Read the fails, then the sheet.
4. Copy the preset into `RECIPES[troop][clip]` and tune key poses: clipping first (aims, a small search), then rate,
   wrist and ground. Keep timing.
5. Up to two fix rounds per failing gate, then stop and report what is left with numbers and sheets.
6. `--author ... --write`, `tools/clips.py --verify`, `python tools/verify.py fast`.
7. Sheets into `docs/review/anim-redo/<troop>/`, then the in-game review loop. Kort sees it before any merge.

## 12. Known limits

- Only the knight overhead is tuned. The family presets bake and run, but none passes every gate yet: they need the
  per-troop tuning above. Bow troops also start a clip with the game's instant draw rotation on the hand, which the
  `ends` gate reports.
- `crossbow_loose`'s committed impact marker (0.35 s) disagrees with design.json (0.2 s); the writer refuses it until
  Kort decides.
- Non-biped rigs (hound, spider, colossus) have no presets; `extra` channel offsets are the only authoring path.
- The parity test covers clip sampling, not `MotionFeet` grounding, which `pose.py` reimplements.
- Cloth and plume springs are not simulated; cloth bones keep their channels.
