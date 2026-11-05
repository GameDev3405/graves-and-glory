# Animating troops

This is how the Oathbound knight's animations were made: what was built, how it works, and what worked or failed along
the way. Use it as the workflow for animating the next troop. The project skill `.claude/skills/animate-troop/` is the
short, step-by-step version for Claude sessions.

Two animation sets exist:

| set | rig | where | status |
|---|---|---|---|
| `oath_combo`, `oath_guard` | the game's troop rig (rigid limbs) | `art/animations/oathbound/` | art; plays on today's model; not wired |
| combat pack: `combat_idle`, `combat_advance`, `combat_combo`, `combat_block`, `combat_hit` | the articulated rig (waist, elbows, knees, ankles) | `art/animations/oathbound_combat/` | art; needs new model bones; not wired |
| Thornhide archer: `archer_idle`, `archer_kite`, `archer_shoot`, `archer_hit` | the articulated rig, with hands placed by IK | `art/animations/thornhide_archer/` | art; needs new model bones; not wired |

Both are **preview art**. Wiring clips into the game and changing the troop model are gameplay-visible, so Kort decides
(QUESTIONS-FOR-KORT rows ANIM-01 and ANIM-02).

---

## 1. The pipeline at a glance

```
armour set (tools/player_armor/<set>.py)  ─┐
weapons    (tools/player_weapons/<set>.py) ┼─> Unit: boxes + textures + bones ──> split at joints (combat_rig.split_parts)
placeholder body (build_player_armor.BODY) ┘                                          │
                                                                                       ▼
timeline of poses (combat_clips.py) ──> build(): keyed torso + solved arms/legs/head/shield + clearance + springs ──> Clip
                                                                                       │
                       ┌───────────────────────────────────────────────────────────────┼───────────────────────┐
                       ▼                                       ▼                       ▼                       ▼
            checks (tests, --check)            renderer (frames, 2x supersampled)   exports: .java .json   .bbmodel
                                                  ──> reel .mp4, .gif, beat sheets
```

| file | role |
|---|---|
| `tools/unit_anim/rig.py` | The game's troop rig. It poses bones exactly as Minecraft's `AnimationHelper` does: degrees, rotation order Rz·Ry·Rx in model space, Minecraft's Catmull-Rom between keys. It also has `mc_rotation`/`mc_euler`, `sample`, `Clip`, `world()` (any bone table), and the spring solver for cape and plume. |
| `tools/unit_anim/combat_rig.py` | The articulated rig: bone table, armour cutting and joint sleeves, PCHIP interpolation, foot targets, leg IK, shield-wrist aim, arm aiming with a natural sword grip. |
| `tools/unit_anim/combat_clips.py` | Poses, presets and timelines for the five combat clips, and `build()`, which turns a timeline into a baked `Clip`. |
| `tools/unit_anim/clearance.py` | Measures how far held items pass through the body, and the per-tick solver that moves the arms aside. |
| `tools/unit_anim/oathbound_clips.py` | The rigid-rig clips (`oath_combo`, `oath_guard`). |
| `tools/animate_unit.py` | The rigid-rig driver. It also holds the shared pieces: `Unit`, grip transforms `SWORD_LOCAL`/`SHIELD_LOCAL`, `frame()`, `smoothness()`, `encode()`, `gif()`, `_bb_rotation()`. |
| `tools/animate_combat.py` | The combat driver: `CombatUnit`, floor studs, composite frames, beat sheets, the Java/JSON/bbmodel exports, the reel, and the `--still`/`--check` modes. |
| `tools/player_armor/engine.py` | The box model and the z-buffered renderer. A drawable can carry a 4x4 bone matrix; faces are lit by where they point. |
| `test-tools/standalone/test_unit_anim.py`, `test_combat_anim.py` | The quality gates (section 6). |

Commands:

```bash
python tools/animate_combat.py                     # everything: exports, beat sheets, reel (about 4 min, all cores)
python tools/animate_combat.py --still combo 0.4   # one composite frame of a clip
python tools/animate_combat.py --check             # IK, feet, smoothness and clipping numbers per clip
python tools/animate_combat.py --sheet combo 0.32 0.4 --out /tmp   # three large views per time, with clipping
python tools/animate_combat.py --strip combo 0.26 0.42 --out /tmp  # consecutive 60 fps frames, tiled
python -m unittest discover -s test-tools/standalone -p 'test_*.py'
```

Python 3.13 works for these tools (pytest is not installed; use unittest). `tools/build_skins.py` needs 3.12, but the
animation tools don't use it.

---

## 2. Conventions that must not drift

- **Blockbench space**: y up, feet at y 0, the troop's front is −z, its right arm is at +x.
- **Clip convention**, the game's `BipedClips`: angles in degrees. An arm pitch of −90 points forward, a positive body
  pitch leans forward, a positive head pitch looks down. Offsets are model px with model x = −Blockbench x.
- **Model ↔ Blockbench**: `mc_rotation(p, y, r) = Rz(r)·Ry(−y)·Rx(−p)` in Blockbench space. Blockbench animation keys
  are rotation `(−p, −y, r)` and position `(−x, y, z)`.
- **Interpolation**: hand keys use Minecraft's Catmull-Rom (`rig.sample`), so the preview moves the way the game will.
  Catmull-Rom over-shoots between two equal keys, so it can't hold a planted foot; placements use PCHIP (section 4).
- **Held items**: weapons are authored grip at origin, blade +y, south as the front. `SWORD_LOCAL = T(6, 12.5, −1)·Rx(−90)·Ry(90)`
  on `right_hand`; `SHIELD_LOCAL = T(−6, 13, −1)·Ry(180)` on `left_hand`. In a `.bbmodel` each item is a group with
  its origin at the grip and that rotation (`_bb_rotation`). The cubes are offset by the grip position.

---

## 3. The rigs

### The game's rig (rigid limbs)

root (0, 24, 0) → body (0, 12, 0) → head (0, 24, 0) and arms (±5, 22, 0) → hands (±6, 14, 0); legs (±1.9, 12, 0) hang
from the root. Arms and legs are single boxes, so there are no elbows, knees or crouch. With rigid legs, a ground
solver has to *lift the root* so the lowest sabaton touches the floor, about 0.9 px in a wide stance. That was the
visible limit that led to the articulated rig.

### The articulated rig (`combat_rig.BONES`)

| joint | bone | pivot | why there |
|---|---|---|---|
| waist | `chest` | (0, 17.5, 0) | splits the cuirass at a whole texel row; the head and arms ride on it |
| elbows | `*_forearm` | (±6, 17.5, **0.5**) | just behind the arm's centre (see "didn't work": at z 2.5 the fist folded into the upper arm) |
| knees | `*_shin` | (±1.9, 7.5, −2.5) | at the front of the leg, so flexing never opens a gap at the knee cop |
| ankles | `*_foot` | (±1.9, 2.5, 0) | the foot stays flat or rolls on its toe while the leg moves |

The hinges are clamped: elbow 0 to −145, knee 0 to 150.

**Cutting the armour** (`split_parts`): every box crossing a joint height becomes two boxes. The cut snaps to the box's
own texel rows. Side faces keep their rows of the original atlas (a UV sub-rect), so nothing is repainted. The new cut
faces show the plate's edge row, stretched. Pieces that should ride one segment whole are listed by name prefix in
`WHOLE`: couter, skirt, poleyn, sabaton, toe, pauldron, wing. Boxes with their own rotation go whole to the segment
holding their origin.

**Joint sleeves**: inside every cut except the ankle (the sabaton covers that one) sits a box 0.15 px smaller than the
limb, reaching 1.5 px either side of the cut, on the lower bone. It is invisible at rest and fills the wedge when the
joint bends, so no seam or skin shows.

---

## 4. Authoring a clip

### Timelines are chained poses

`seq(GUARD, [(t, 'label', {changes}), ...])`. Each beat lists only what changes from the beat before. A pose has:

| field | what | interpolated by |
|---|---|---|
| `root` | (model x, y, z): crouch is negative y, lunge is negative z | Catmull-Rom (game keys) |
| `body`, `chest` | (pitch, yaw, roll) of the hips and the waist | Catmull-Rom (game keys) |
| `sword` | (upper-arm direction, forearm direction, blade direction, grip twist in degrees), as vectors in the **chest's frame** (+x the troop's right, +y up, −z forward) | each vector by Catmull-Rom, then solved |
| `shield_arm` | (upper-arm direction, forearm direction) | the same |
| `shield` | the board's facing (pitch, yaw, roll) relative to the root; (0, 0, 0) is upright, facing forward | PCHIP, then wrist solved |
| `look` | where the eyes point, relative to the root | PCHIP, then neck solved (clamped 50/75/25) |
| `r_place`, `l_place` | foot placement (out, z, lift, toe_out, heel): distance from the centre line, Blockbench z, sole height, toe turn-out, heel rise (negative rocks onto the heel) | PCHIP, then leg IK |

Write named presets for arm aims (`S_GUARD`, `S_CUT`, `H_WIDE`, ...) and reuse them. `_blend(a, b, k)` makes in-between
aims.

### Why aims instead of angles

Authoring Euler angles for an elbow chain was slow and unpredictable. Directions are readable: "upper arm out and up,
forearm up, blade back over the shoulder". `aim_arm` converts them:

- The upper arm's frame points down the upper-arm direction. The elbow bends toward the forearm, steadied by a little
  of the blade direction and, when the arm is nearly straight, by the last tick's bend direction (continuity).
- The elbow angle is the angle between upper arm and forearm (clamped at 145).
- **Natural grip**: the blade's flats stay square to the forearm's sides, as a hand really holds a sword, turned by
  `twist`. That also keeps the wrist far from gimbal lock.
- `closest_euler` picks the Euler branch nearest the previous tick, so keys never flip.

### Timing principles that read well

- **Anticipation**: the wind-up holds a beat (0.22 to 0.26 s), loaded opposite to the strike (the hips twist about 60
  degrees away).
- **Fast strikes**: a cut takes 0.06 to 0.08 s; up to about 35 degrees a frame at 60 fps reads as a strike, not a pop.
  A strike in 0.04 s was too fast to read (66 degrees a frame).
- **Overlap**: `lags={'forearm': 0.015, 'hand': 0.03, 'shield_forearm': 0.02}` delays the forearm's and blade's aims,
  so the body breaks in order: hips, chest, arm, forearm, blade. Loops use larger lags (0.05 to 0.1).
- **Weight**: every strike is driven from a planted foot (back heel rising onto the toe), the root drops into the lunge,
  and a step lands heel first.
- **Recoveries are slower than strikes**: a high guard returning to guard in 0.08 s looked like a glitch; over 0.28 s
  with an in-between key it reads right.
- **Once-clips start and end on `GUARD` exactly**, and the idle starts on `GUARD` (its sine phases are zero at t = 0),
  so clips chain without pops.
- **Read both views**: a pose that reads in three-quarter view can be nonsense in profile. The first high guard pointed
  the blade at the camera across the face, and Kort read it as "a stab or a bunt?".

### What `build()` does (in order)

1. Hand keys for body and chest, and root keys, at the beats.
2. Legs: per tick (`STEP` = 0.025 s, twice a game tick), `solve_leg` puts the ankle where the placement says. The hip
   yaw follows the toe; damped Gauss-Newton on hip pitch, roll and knee, warm-started from the last tick. The foot's
   local rotation keeps the sole flat or on its toe. The head is solved to `look`.
3. Clearance (section 5): the push each arm needs, per tick, then smoothed.
4. Arms baked per tick from their aims (with pushes); the shield's wrist aimed per tick from the solved forearm.
5. Cape (from the chest) and plume (from the head) as damped springs, baked.

---

## 5. Keeping held items out of the body

- **Measure** (`clearance.Clipper`): points every 0.5 px through the sword and the shield, tested against every body
  and armour box in world space. It reports px³ inside the body (and, with `detail`, the deepest point and the part).
  Excluded: the grips (they belong in the fist), the pommel against the sword's own forearm, the board against the
  shield's own forearm (strapped behind it), and the cape and plume (soft).
- **Fix poses first.** List every beat that clips with clearance off, then fix the poses. Where hand-fixing kept
  missing, a search (random perturbations of the arm aims, up to about 35 degrees) found the clear aim closest to the
  authored one. Those results became the presets. That keeps each beat's intent.
- **Then the solver** covers what is left. Per tick it tries push levels (`LEVELS`, along fixed `PUSH` directions: the
  shield arm forward and out, the sword arm and blade out) and picks the smallest that brings the item to 1 px³ or
  less. `smooth()` holds each push 0.15 s either side and eases it with a Gaussian of σ 0.05 s (three σ fit inside
  the hold, so peaks keep their height). For once-clips it fades in over the first 0.06 s and out over the last 0.2 s,
  so they start and end on the guard.

Result: from 120 px³ (combo) and 43 px³ (guard at rest) to at most 1.1 px³ on any frame. The idle and the advance need
no push at all; the combo's average push is 0.2.

---

## 6. Quality gates (and the numbers they hold today)

| check | how | threshold | today |
|---|---|---|---|
| reach | worst ankle-target error from the IK | < 0.3 px | 0.27 |
| feet sink | lowest sole point below y 0, at 60 fps | ≤ 0.15 px | 0.07 |
| feet hover | gap under a foot meant to be planted | ≤ 0.15 px | 0.14 |
| feet skate | planted foot speed against the ground (the advance subtracts its ground speed) | ≤ 8 px/s | 6.7 |
| pops | **true rotation** of each bone between 60 fps frames (not Euler numbers) | ≤ 50 degrees a frame | 47 (the hit snap); strikes about 35 |
| jerk | change of that per-frame rotation | ≤ 30 degrees | under 30 |
| clipping | `Clipper` volume per item, every 60 fps frame | ≤ 1.5 px³ | 1.1 |
| loops | pose at 0 against pose at the end | < 0.5 degrees, root within 0.01 px | 0.02 degrees |
| chaining | once-clips start and end on the idle's first pose | < 1 degree (arms carry the idle's lag) | exact |
| armour cut | the pieces stack back into each original box; each face keeps its own rows | exact | pass |
| exports | committed `.java`/`.json` equal what the clips build | exact | pass |

Exports are regenerated, not hand-edited. Any change to clips means re-exporting before the tests pass.

---

## 7. The review loop (how poses were actually judged)

1. **Review sheet**: `python tools/animate_combat.py --sheet combo [times...] --out DIR` renders three large views
   (three-quarter shield side, profile sword side, front) per time, with each item's clipping volume printed. With no
   times it uses the clip's beats. Use it while blocking poses. This is where most problems were seen: hanging
   blades, shields seen edge-on, blades pointing at the camera.
2. **Beat sheets**: the committed `<clip>_beats.png`, every beat from both reel views, for Kort.
3. **Numbers**: `--check` and the tests. Measure true rotations, feet and clipping rather than trusting the eye on a
   still frame.
4. **Filmstrip**: `--strip combo 0.26 0.42 --out DIR` tiles consecutive 60 fps frames of a strike. Arc continuity and
   pops are visible here and not on a sheet.
5. **The reel**: two views, a timeline of beats, slow-motion repeats of strikes, scrolling floor studs on moving clips.

---

## 8. What worked

- **Aims instead of Euler angles** for arms, with a solver that keeps the Euler branch continuous.
- **Baking everything solved twice a tick** (0.025 s): arcs stay on track through fast strikes, planted feet stay
  put, and the game plays what the preview shows.
- **Placing feet and solving legs**: lunges, crouches and steps read as weight, and the feet never skate.
- **PCHIP for placements**: exact holds between equal keys, smooth through moving ones, no overshoot.
- **Cutting armour by texel rows plus joint sleeves**: the existing armour set works on the new rig with no repainting
  and no visible seams.
- **Natural sword grip**: the wrist looks right and gimbal lock disappears.
- **Measuring clipping and searching for clear aims** before relying on a corrective solver.
- **True-rotation smoothness** as the pop test. Euler steps lied in both directions.
- **Damped-spring cape and plume** with clamps, so cloth never swings through the body or a leg.
- **Supersampled frames (2x, LANCZOS)**, faces lit by their current direction, a soft contact shadow, a 60 fps
  libx264 reel, and beat sheets from two views.

## 9. What didn't work, and the fix

| symptom | cause | fix |
|---|---|---|
| Arms swung 50 to 110 degrees in one frame | Catmull-Rom between far-apart Euler keys, and branch changes between keys | interpolate the *aims*, solve and bake every tick with branch continuity |
| Wrist spun 110 degrees during the rising cut | a keyed "edge" vector passed along the blade, so its perpendicular part vanished | natural grip (flats square to the forearm) plus a twist angle |
| Edge that followed the blade's motion | flipped whenever the blade reversed (wind-up hold, hit snap) | dropped; see the natural grip |
| Hand still spun with a continuous edge | gimbal lock in the hand's local Euler (yaw near ±90) | natural grip keeps the wrist away from it |
| Upper arm rolled 93 degrees at 0.83 s | between gather and rising cut the forearm crossed from one side of the upper arm to the other, so the bend plane swung 180 degrees | author both poses bending to the same side; carry the last bend direction through a straight arm |
| Planted feet crept | Catmull-Rom over-shoots between equal keys | PCHIP for placements |
| Feet skated at 17 px/s in lunges | legs baked at 20 Hz under fast root motion | bake at 40 Hz (0.025 s) |
| Toe cap sank 0.85 px when the heel rose | pivoted on the ball of the foot; the toe cap reaches 2 px further | pivot on the toe-cap tip |
| Shield showed its back at "aim 0" | the aim solve ignored the item's rest rotation (Ry 180) | aim = `mc_rotation(aim)·item_rest` |
| Pommel inside the upper arm in almost every guard | elbow hinge at the back edge (z 2.5) folded the fist up against the upper arm | elbow pivot at z 0.5; the sleeve closes the elbow's point |
| Shield board through the helm, even in guard | the 17 px board held high and close; the helm is 10x10 | lower, further-forward shield fist; swing out wide during big cuts |
| A clearance push caused new clipping at 0.27 s | the push for the cut was held earlier, where it drove the arm into the pauldron | fix the cause: route the cut's arc outside the helm |
| Large automatic pushes (1.5) bent the authored arcs | poses that clipped by tens of px³ | fix poses at the source (search), so the solver only mops up |
| Hit react started 5 degrees off guard | push smoothing bled into frame 0 | fade pushes in and out at once-clip ends |
| High guard read as "a stab or a bunt" in profile | blade pointed at the camera across the face | shoulder guard: blade cocked back over the shoulder |
| Wind-up blade hung down at the side | blade direction aimed down-back | blade up and back over the shoulder |
| Euler-based smoothness test passed bad motion and failed good motion | gimbal regions inflate Euler steps; small Euler steps can hide flips | measure the geodesic angle between frame rotations |
| `oath_knight.bbmodel` had the items at the origin | the item group had no rotation, and the cubes no grip offset | group rotation from `_bb_rotation`, cubes offset by the grip; regenerated |
| Rigid rig lifted the body off the floor | legs can't bend | the articulated rig with knee IK |

Process notes:

- **Stills lie about speed.** A cut that looks fine at its beats can be 66 degrees a frame. Always run `--check`.
- **Measure, then look.** The clipping numbers found problems a beat sheet hid, such as the board 15 px³ into the helm
  in the resting guard.
- **Pushing to GitHub**: the auto-mode classifier sometimes denies `git push`. Don't work around it; ask the user to
  approve it.

---

## 10. Animating another troop: checklist

The code is Oathbound-specific in a few places. To animate a new set (say Barrow Warden with Gravereaper and
Wakelight):

1. **Unit and show**: write a unit class and a `Show` (copy `tools/animate_archer.py`). The unit needs `parts` (cut
   with `split_parts`), `drawables_at` (for items that change per frame), `export_parts` and `clipper`. The `Show`
   holds the title, clips, titles, output folder, Java header and reel plan. The driver (`animate_combat.main`) does
   the rest.
2. **Grips**: write the grip transform for each held item (`SWORD_LOCAL`-style). Check it on a neutral pose from three
   views. A two-handed weapon needs both hands aimed at the haft (aim the off hand to a point on the item) and is a new
   feature.
3. **Cutting**: check `WHOLE` against the new armour's box names (knee cops, elbow cops, skirts, boots must ride one
   segment). Run the armour-cut test.
4. **Clearance**: set `HELD_IN_HAND` for the new items' grip boxes, and `PUSH` directions that suit them. A polearm
   pushes differently from a sword.
5. **Poses**: start from `GUARD`. Block the guard with `--sheet`, then the beats of each clip. Keep the presets
   readable.
6. **Clip clearance**: list beats that clip with `clearance=False`, fix them (search if needed), then build with
   clearance on.
7. **Gates**: run `--check`, look at the beat sheets and a filmstrip of each strike, then run the tests.
8. **Render and export**, then update `art/animations/<set>/README.md`, BACKLOG and QUESTIONS-FOR-KORT.
9. **Commit, then ask before pushing** if the session has not been told to push.

## 11. Placed hands and searched poses (the archer)

The archer adds what a bow needs, and lessons that hold for any two-handed or ranged unit.

- **Placing a hand.** A bow's grip and a draw hand on the string are *positions*, not aims. `combat_rig.reach_arm` is
  position IK: damped Gauss-Newton on small turns of last tick's arm directions (tangent space, so no poles), with a
  forearm-direction guide (a straight wrist), a continuity term, a soft 15 degree minimum elbow bend, and a term that
  keeps the forearm's bend direction (the arm's twist) turning smoothly.
- **Items that change per frame** (a bow drawn continuously, an arrow that comes and goes) are drawn by the unit's
  `drawables_at(pose, clip, t)`. The clipper sees them through pseudo-bones that a clip's `extra_pose` adds.
- **Proportions decide the design.** Measure reach (about 8 px from the shoulder), head and collar size, and the item's
  geometry *before* designing poses. A real archer's form (an anchor at the jaw, an arrow nocked across the belly) does
  not fit a 10 px head and a 12 px fur ruff. The archer's poses changed design twice before they worked.
- **Measure the forearms too.** The item clipper never sees an arm through the chest. `clearance.LimbClipper` measures
  forearm volume deeper than 1.1 px in the torso or head. It skips pairs that already overlap at rest (armour modelled
  that way), and it treats soft parts as soft (fur, cloth).
- **Search, don't guess.** `tools/unit_anim/pose_search.py` fixes beats: random steps on a beat's own fields, scored by
  items + forearms + reach + a cost for straying from the written beat. The results are stored as data
  (`<unit>_poses.json`). There are two modes:
  - judged as a still pose (fast)
  - judged over the motion between the beat's neighbours (`search_window`), for transitions a still cannot see
- **Proportions first, then search, then the solver.** First check the proportions, then search the key poses, then
  search the beats, then search the windows. A search cannot rescue a design that does not fit the proportions.

### What failed, and the fix

| symptom | cause | fix |
|---|---|---|
| No full-draw pose could be reached | an anchor in front of the face is 11 px from the draw shoulder | side-on stance, string to the cheek; the anchor follows from where the string is |
| The bow limb ran through the forearm | the bow was attached like a sword (limbs along the forearm) | grip across the fist; forearm guide down the shot |
| Forearm 200 to 450 px³ in the chest at ready | a nocked arrow held low across the belly | carry the bow at the side; nock on the shooting line |
| The nock was out of reach | the bow arm fully out puts the string 3 px past the cheek | push-pull: the bow arm bent to nock, pushed out while drawing |
| The arm flipped about 160 degrees in a frame | yaw and pitch IK parameters have a pole at an arm hanging straight down; a near-straight elbow's bend side is undefined | tangent-space parameters; continuity, minimum bend and twist terms |
| Analytic two-bone IK made it worse | it changed every elbow the poses had been tuned against; its side memory made loops path-dependent | kept the numeric solver and fixed its causes |
| The bow swept through a thigh while lowering | its cant swung the lower limb across the body | roll the bow flat (cant -70) while it is still high |
| Fixes per beat moved the clipping into the gaps | a still pose cannot see the motion | the window search, plus in-between beats routing the bow around the body |
| The clip did not end on ready (arm off by 9 degrees) | the IK reaches the same hand with a different twist | settle the last 0.3 s onto the start by slerp |
| The settle blend broke the hand (150 degrees) | it blended Euler numbers across branches | slerp of quaternions, then `closest_euler` |
| Extra log entries at t = 0 | the solver warm-up appended to the per-tick log | reset the logs after warm-up |
| The test passed but the game would show more | the test sampled at 30 fps, the game draws at 60 | test at 60 fps |

## 12. Open decisions (Kort)

- Whether troops get the articulated model: seven new bones and armour cut at the joints. It changes every troop's look.
- Which clips become the attack, block, hit and in-combat idle/move animations (ANIMATION-PLAN gap 1).
- Advance pace (0.5 block/s) against the AI's approach speed.
- Cape and plume bones for the sprung keys.
- For the archer: drive the bow's pull frames from the clip's draw; whether the hip quiver joins the Thornhide armour.
- No `.bbmodel` has been opened in Blockbench yet. The conversion is verified numerically only.
