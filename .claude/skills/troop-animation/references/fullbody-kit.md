# The full-body kit: authoring an action clip in world space

`art/anim/common/fullbody.py` turns a short spec (where the pelvis, feet, hand and blade are, tick by tick) into the
two GeckoLib clips the game plays (`<name>` and `<name>_legs`). You say where things are; it solves the joints, keeps
the edge leading, and refuses a target the arm cannot reach. Worked example, read it first:
`art/anim/knight/build_overhead_demo.py` (the knight's overhead cut, the skill's demo).

Use `fullbody.clips()` for attacks and other one-shot action clips on the knight skeleton (knight, sentinel and the
troops built on `art/anim/knight/build_anims.py`). Idle, gait, stances, hits and deaths keep their own systems
(`pipeline.md`). A bow or a weapon held in two hands has its own builder with its own solver: there you keep that
builder's keys and solver and take only the kit's leg solve and `soft_motion` (section "Other rigs" below, with two
worked examples: `art/anim/archer/build_direct_demo.py` and `art/anim/wm_tusk/build_thrust_demo.py`).

## Frame of reference

- World: **+Z forward** (toward the foe), **+Y up**, **+X the troop's LEFT**. The sword hand is on the right (x about
  -9). Units are model px (16 px = 1 block). Ticks are 1/20 s.
- Blade direction is `(pitch, yaw, lean)`: pitch 0 level forward, -90 straight up, -150 up and back over the
  shoulder, +60 forward and down. Yaw + swings a forward blade to the left. Lean + tips an upright blade to the left.
- `reach` is the sword wrist measured **from the sword shoulder**, so it moves with the body. The arm is 8.1 px:
  keep the offset under 7.9.

## The tracks

| Track | Says | Notes |
|---|---|---|
| `hips` | pelvis rotation and position | **Author this first.** It is the blow. Sink and coil for the load, drive forward and drop for the strike. |
| `steps` | which foot lifts and plants, when and where | A foot is planted between steps. An attack without a real step fails `whole_body`. |
| `torso` | lean, twist, side bend | Twist away in the load, through the target in the strike. Lean limit: see below. |
| `head` | added to the automatic eyes-on-foe counter-turn | Small numbers (under 10). |
| `reach` / `hand` | the sword wrist | The path of the hand is the path of the cut. Wide arcs, never through the helm. |
| `blade` | where the blade points | Keys at guard, load, impact, end of follow-through at least. |
| `fist`, `face`, `throw` | the shield fist, its facing, the extra swing of its weight | The shield comes in for the load and swings out as the counterweight. Never still. |
| `ticks`, `impact` | length and impact tick | From `sheets/design.json` (`B.ATTACK_TICKS`, `B.IMPACT[...]`). Never invented. |

Everything between keys is a monotone curve: a key is never overshot, so put a key where you want an extreme.
The first and last samples are the guard pose exactly.

## The order that works

1. **Write the phases as a comment first**, with tick ranges: anticipation, load, step, strike, follow-through,
   recover, settle. STYLE.md's contrast rule: a load that is held, a strike that is the fastest thing in the clip.
2. **Hips and steps.** Build and render with the arms still on guard. If the legs alone do not read as the move,
   the arms will not save it.
3. **Torso and head.**
4. **Reach and blade.** Run `fullbody.reach_report(SPEC)` first: it prints, every half tick, the shoulder, the hand
   target and the distance against the arm's reach, with `OUT` where it is too far. Fix the target (or move the
   hips toward it); do not shorten the blow.
5. **Shield.** Then every hanging piece follows by itself: `fullbody.clips()` ends with `soft_motion` (section
   "The whole unit").
6. **Build, check, look:** build the clip, run `focus_check.py --clip <name> --verbose` and `scorecard.py`, render
   every tick in front, side and three-quarter, and look at each tick at full size. Change one track per round and
   read the row that moved.

The build prints `peak tip speed` and `worst between-key error`. A between-key error over about 4 degrees means two
keys are too far apart for the game's interpolation to follow the solve: the kit bakes four keys a tick, so this
only happens on a wild wrist path. Smooth the path.

## Exact commands (copy these; do not work them out again)

Work on a copy until the clip passes; only then let the builder write the exported file.

```
A=src/main/resources/assets/gravesandglory
mkdir -p work/<job> && cp $A/animations/troop/knight.animation.json work/<job>/knight.animation.json
python art/anim/knight/build_<move>.py --out work/<job>/knight.animation.json      # --out is read AND written
python tools/anim/focus_check.py --troop knight --anim work/<job>/knight.animation.json --clip attack_<move> --verbose
python -c "import sys;sys.path.insert(0,'tools/anim');import scorecard as s,json;r=s.score_troop('.', 'knight', json.load(open('tools/anim/scorecard_limits.json')), anim='work/<job>/knight.animation.json', only=['attack_<move>']);print([(c['name'],c['pass'],[m['id'] for m in c['metrics'] if m['status']=='fail']) for c in r])"
for v in side three_quarter front; do python tools/anim/render_geo.py --geo $A/geo/troop/knight.geo.json \
  --texture $A/textures/entity/troop/knight_geo/t3_crimson.png \
  --anim work/<job>/knight.animation.json --clip attack_<move>_legs \
  --anim work/<job>/knight.animation.json --clip attack_<move> \
  --ticks 0:21:1 --views $v --scale 230 --out work/<job>/render/$v --no-gif; done
```

- `scorecard.py` on the command line only reads the exported file; the Python line above is how to score a copy
  (any troop: change `'knight'` and the paths).
- A troop with armour tiers: add `--rig $A/troop_rig/<troop>.json --tier 3` to `render_geo.py` so it
  draws one tier's cubes, as the game does, instead of all tiers on top of each other.
- **Another troop** (worked example: the tusk). Its demo builder makes the copy itself, there is no `--out`. The
  render needs `--rig` and `--tier`, or every armour tier is drawn at once:

  ```
  T=wm_tusk; J=work/wm_tusk_thrust; C=attack_thrust
  python tools/anim/focus_check.py --troop $T --clip $C --verbose > $J/focus-baseline.txt   # the exported clip, first
  python art/anim/$T/build_thrust_demo.py                                # writes $J/$T.animation.json
  python tools/anim/focus_check.py --troop $T --anim $J/$T.animation.json --clip $C --verbose
  for v in side three_quarter front; do python tools/anim/render_geo.py --geo $A/geo/troop/$T.geo.json \
    --rig $A/troop_rig/$T.json --tier 3 --texture $A/textures/entity/troop/${T}_geo/t3_crimson.png \
    --anim $J/$T.animation.json --clip ${C}_legs --anim $J/$T.animation.json --clip $C \
    --ticks 0:21:1 --views $v --scale 230 --out $J/render/$v --no-gif; done
  ```

  Score the copy with the Python line above, then run the same line with `anim=None` (the exported clip). A metric
  that fails in both is inherited from the stance or the old clip: report it to the owner. A metric that fails only in
  the copy is yours.
- **Ticks run 0 to length-1** (`0:21:1` for a 22-tick clip). Tick 22 is past the end and renders the bind pose.
- **One view per call** gives one folder of full-size frames per view. Several views in one call give a single
  side-by-side strip per tick, which is too small to judge.
- `focus_check.py --clip` takes the **upper** clip name; it plays `<name>_legs` under it by itself. Asking for the
  `_legs` clip alone answers "0 clips".
- Timing: `B.ATTACK_TICKS` (22) and `B.IMPACT["thrust"|"overhead"|"low_cut"]` (7, 10, 8) in
  `art/anim/knight/build_anims.py`, which mirror `sheets/design.json`. Guard constants to start and end on:
  `B.READY_HIPS`, `B.READY_FEET`, `B.GUARD`, `B.FIST_GUARD`. The limits of every check are the `LIMITS` table at
  the top of `tools/anim/focus_check.py`.
- `art/anim/knight/build_thrust_sonnet_test.py` is a second example that writes only to `work/` (it was the
  Sonnet-only trial of this skill, 2026-11-11; a clean 6.5/10 clip, not an approved one).

## Gates (a clip goes forward only when every line is true)

1. `focus_check` on the clip: 0 FAIL. Every WARN is either fixed or named in the report with its tick.
2. Scorecard: 0 fail on the clip.
3. You opened every tick in all three views at full size and wrote one line per tick (`ok` or the defect).
4. No stretch of 3 or more ticks where the feet and hips do not change (the load included: the body keeps coiling).
   **This gate is by eye.** `dead_hold` passes as soon as any visible point moves 0.25 px, and the hanging pieces
   always do. So lay ticks side by side (the hold after the impact and the last 4 ticks are where it fails) and, if
   three in a row are one picture, key a drift into the hips and chest there: 1 px and 2 degrees is not enough,
   aim for 2 px or 4 degrees across the hold. The settle eases the arms onto the guard over the last 3 ticks, so keep
   the hips and a foot arriving until then.
5. The helm is level within about 10 degrees on every tick after the impact, and the feet are further apart at the
   impact than in the guard (a real step).
6. A fresh reviewer (`reviewer-prompt.md`) has seen the frames. Two fix rounds at most, then report what is left.

## What went wrong for the first worker who used this (fix before you meet it)

- **Blade grazes the helm in the first tick** (`weapon_in_body` WARN near tick 0.5): the hand rises before it moves
  out. Put the first `reach` key lower and further out to the sword side (y at or under the guard's, x about -5.5).
- **Every hold needs a drift**, not only the load: the extension after a thrust and the end of a follow-through too.
  Key the start and end of each hold a little apart (1-2 px of hand, 2-3 degrees of torso).
- **`reach` moves with the shoulder.** Changing `hips` or `torso` moves the shoulder and therefore the hand, even
  with the same `reach` numbers. After any hips or torso change, run `reach_report` again and look again.
- **A load that only moves the arm reads as one held pose.** Sink and turn the hips through the whole load and put
  a small gather step in it (the demo's first `steps` entry).
- **A lunge without a long step is weak.** The lead foot should land 4 px or more ahead of its guard place at the
  impact, and the hips should drop about 3 px on that tick.

## The whole unit: soft motion and plates (Kort 2026-11-11)

"i want the whole unit to be animated including pieces of its armor that way it feels alive." He named the cape and
the helm's crest as stale. `fullbody.soft_motion(upper, legs)` is the answer for every rig:

- It finds the pieces by itself (`soft_parts_for()`, the same list `focus_check`'s `secondary_life` judges): cloth
  (cape, kilt, skirt, tippet, hood cape), plumes, hung gear (quiver, pouch, trophy, necklace, hip skull).
  **Not a helm's crest**: he named it stale in round 1, saw it swing in rounds 2 and 3, and had it taken out
  ("remove the wobble of the red strip on his head, i thought i wanted it but i don't"). Things that hang move; a
  ridge fixed to the helm goes with the head. `crest` is out of `ALIVE_KINDS`, so every troop with a `crest` bone
  loses its crest keys at its next rebuild.
  Each one is a weight on a damped spring behind its parent bone, measured from the geo: longer pieces swing slower.
- Every piece also has a slow swing of its own (`sway`, about 2 degrees) with its own phase and its own period, so
  it is not a statue while the body holds a pose (a full draw, a held lunge, the top of a wind-up) and two pieces on
  one body never move as a pair. Reviewers of the archer and the tusk both wrote "pinned" and "one rigid pair"
  before this existed. It needs nothing from you; do not key a sway by hand.
- Sibling bones that hang from the same place (a skirt, its plate, its gilt; the same cloth at three tiers) swing as
  one piece. Two pieces that only share a default bone pivot are two pieces (`SAME_HINGE`).
- **Armour never leaves its limb** (Kort, knight round 2, score 8: "sholder kind of twists out of place in a couple
  frames", and on tick 10 "why is wrist/arm above shoulder?"). Round 2 let each pauldron follow only 30% of its upper
  arm so that a raised arm would not carry it into the helm. With the arm overhead the plate then sat where the
  shoulder was in the guard, and he read it as a dislocated shoulder. Now `plates_for()` lets a plate trail its arm's
  swing by `PLATE_FOLLOW` (0.7) of the turn but **never by more than `PLATE_LAG` (10 degrees)**: in a small motion it
  moves against the arm, in a big swing it stays on the arm.
- A plate still gives way to the helm, but only **along its own arm**: where the pose would sink it more than
  `PLATE_YIELD` (0.25 px) deeper into the head's pieces than the stance has it, it slides down the arm, up to
  `PLATE_SLIDE` (2.5 px), and stays wrapped round it. The report line says how far ("plate right_pauldron slides
  1.6 px down its arm, off the helm"). If it shows the full 2.5 px, the head or the torso is turned too far: fix the
  pose.
- **Do not detach a piece to win a clip number.** The 30% rule was added for a reviewer's shallow helm-into-pauldron
  figure (1.88 px) on ticks Kort had graded good. A plate that rides a raised arm past the helm is what armour does;
  a plate left behind is a fault he sees at once. When a number and the limb disagree, the piece stays on the limb.
- Everything is measured from the first sample and faded in over 1 tick and out over 2, so the clip still starts and
  ends on the stance exactly.
- `fullbody.clips()` already calls it. In any other builder, call it on the samples right before baking:
  `upper, legs = fullbody.soft_motion(upper, legs, report=True)`; the report line lists each piece and its swing.
- Tune one piece per troop in `SOFT_TUNED` (period, gain, max, `lim` per axis), never by keying it by hand. `inward`
  and `lim` stop cloth going through the body it hangs on: if `armour_clip` names a soft piece, lower its `max` or
  set `lim`.
- A piece the row `secondary_life` calls still but the kit does not move: its bone name has no token in
  `focus_check.ALIVE_KINDS`. Add the token there (or the whole name to `ALIVE_NAMED`); both tools then see it.

## What the edge logic does (so you can tell when the spec is wrong)

When the tip is slow the hand keeps its natural grip. Inside the strike (from 3 ticks before the impact to 2 after)
the kit rolls the hand about the blade so the edge is on the tip's line of travel, and lets go over 2.5 ticks after
the cut. **Never in the wind-up**: Kort graded a wind-up where the kit did that "weapons spins around in hand"
(2026-11-11, tick 5). The roll is carried from sample to sample in the world, so it cannot flip. So:

- `grip_roll` fails when the weapon turns about its own length before the strike. If you key a hand yourself, turn
  the blade with the wrist's swing, not its twist.

- `edge_lead` fails only if the **path** is wrong: the tip changes direction sharply mid-cut, or the hand stops
  while the blade keeps swinging. Fix the `reach` and `blade` keys around that tick.
- A visible hand roll right after the impact means the follow-through turns too sharply. Carry it on along the same
  line for a tick before curving.

## Limits learned on the demo (2026-11-11)

- Torso lean in a heavy strike: about 19 degrees (plus 8 from the hips) at the end of the follow-through reads as commitment; 24 read as a hunch. Kort
  rejected hits that "break their back": do not go past the approved knight's lean, and come back up in recovery.
- The head only counters 70% of the torso's lean and none of the hips'. With a torso lean of 24 the chin sat on the
  pauldron and the knight read as hunched (reviewer, round 2). Fix that worked: torso lean 19 at the deepest key and a
  `head` key of about -6 pitch on the follow-through, so the helm stays level and the eyes stay on the foe.
- A blade raised over the sword shoulder clips the helm or crown unless the hand is carried out to the side and up
  (reach x about -6, y above +4 on the knight). Check `weapon_in_body` at the top of the load.
- Sinking the hips in the strike puts the rear knee or the shield rim through the floor before it looks low. Sink
  about 3 px, not 5.
- An extra elbow-twist track made every result worse. Leave `elbow` out unless a check asks for it.
- A hold that is truly still fails `dead_hold`. Key the top of the load twice, two ticks apart, a little further on
  the second key.

## Other rigs: a bow, a weapon in two hands (2026-11-11, archer and Warmaw tusk)

Kort asked for the next tests "on a unit that isnt so easy, like a archer and a wildenmoot unit". What was learned,
as a procedure. Follow it in this order for any troop that is not a knight-skeleton sword troop.

**1. Find the owner and do not edit its builder.** `ROLLOUT-PLAN.md` and `pipeline.md` name the troop's builder
(archer: `art/anim/archer/build_bow_anims.py`; spear troops: `art/anim/spear/build_spear_anims.py` with
`GG_SPEAR_TROOP=<troop>`). Write a new file next to the troop (`art/anim/<troop>/build_<move>_demo.py`) that
imports the builder, reuses its key format and solver, and writes to `work/<job>/<troop>.animation.json` (a copy of
the exported file), never to the exported file. Copy the skeleton of one of the two examples.

**2. Take the baseline before you change anything.** Run `focus_check --troop <troop> --clip <clip> --verbose` on
the exported clip and save the rows. Two things come out of it: the fix list (its FAIL rows are your brief), and the
rows the **stance** already fails. The checks' limits were set on the knight; a row that is over the limit on the
first frame of the stance is the stance's, and the tool says so ("fix the stance"). Put those in the report for the
troop's owner. Do not spend rounds trying to animate them away.

**3. Look at the weapon once, big, before judging any animation.** Render one tick at `--scale 230` or more and
check the weapon is the right shape and in the hand. On 2026-11-11 the bow of every bow troop turned out to be
exported with its limb cubes turned the wrong way (the string of a drawn bow opened behind the archer). That is a
model fault: report it, render with a corrected copy if you can make one, and do not bend the clip around it.

**4. Measure the rig.** Arm length, weapon length, where the stance holds it. The archer's arms reach 9 px and the
bow draws 11.5: a keyed pose that looks right on a human rig is out of reach here. So:

- **Solve poses from targets, do not key joint angles** for anything that must touch something: the hand on the
  string, the fist at the ear, two fists on one shaft. The owning builder has the solver (`BW.solve_drawn`, `BW.string_arm`: the bow builder's arm
  solve, `SP.Solver().pose`); the demo adds what it lacks (`fit_bow_arm`, `string_arm`, `solve_full` in the archer
  demo).
- **Ease the target, then solve every sample.** Blending from a solved pose to a keyed pose pops (the archer's old
  loose: 124 degrees in one sample). Move the target along a curve and solve on every sample instead.
- **A solver reports its error; read it.** The spear builder prints `grip err`: the distance between the rear fist and
  where the key asked for it. Up to about 2 px means the arm is straight and the key asked for a little more: fine.
  More means the pose you keyed is not the pose you get: move the key.
  That printed number is the worst of the whole clip. To find the tick, reset the counter around each sample in your
  own demo builder: `sol.worst = {"right": 0.0, "left": 0.0}` just before the `sol.pose(...)` of sample `i`, and
  print `i / SUB, sol.worst` just after. `fullbody.reach_report` is for the knight's tracks only.

**5. Legs first, as always.** `K.attack_legs({"hips": [...], "steps": [...]})` works for every troop on the knight
leg solve. Signs, which are easy to get wrong:

| Thing | Format | Sign |
|---|---|---|
| hips key | `(tick, (pitch, yaw, roll), (x, y, z))` | x + is the troop's left, **z + is BACK**, yaw + turns the troop to its right |
| step | `(foot, lift tick, plant tick, (x, y, z), foot yaw, lift height)` | world ankle, **z + is FORWARD**, y 2.0 on the ground |
| torso, hips rotation | `(pitch, yaw, roll)` degrees | pitch **+ leans forward and down**; yaw **+ turns to the troop's RIGHT** (right shoulder 1.7 px back and left shoulder 1.7 px forward per 20 degrees: the coil of a right-handed blow); roll + tips toward the troop's left (left shoulder down). **Torso adds to hips**: hips yaw 20 with torso yaw 20 faces the chest 40 off the foe |
| head rotation | `(pitch, yaw, roll)` degrees | pitch **+ looks DOWN**, - looks up; yaw + looks to the troop's right. Knight kit: added to the automatic eyes-on-foe counter-turn. Spear `KEY`: relative to the world (the builder takes the chest and hips back out) |
| spear `KEY` | `KEY(torso, head, O, d, Ls=...)` | O is the rear fist in troop px, **x - is the troop's right**, z + forward; `d = aim(elevation, yaw to the left)`; **head is relative to the world, not the chest**: yaw 0 looks at the foe |

These signs were measured on the tusk rig on 2026-11-11 (shoulder positions with one bone turned 20 degrees). When
in doubt, do the same: turn one bone, render one tick, look. Do not reason the sign out.

"ankle unreachable" from the leg solve means the leg is over-straight: bring the foot closer or lower the hips.

**6. Rules that came out of the bow.** (Kort scored the archer demo 8: "legs look abnormal. shot seems quick. the
bows string is inside the archers arm to start. the head shake while the unit is pulling bow is nice touch.")
- **A bow is held ACROSS the forearm, never along it.** The resting string sits about 3.8 px behind the grip and a
  forearm is 4 px thick: with the arrow line within about 45 degrees of the forearm line (elbow to hand) the string
  runs through the arm. The lead's guard does this on every clip's first frame. The checks have no row for it, so
  measure it: the string's two segments against the bow arm's boxes on every tick the string is at rest. At most
  0.5 px.
- **The guard's hold is the aim's hold.** Give every guard-side key the wrist the aim uses (`GUARD_WRIST` in the
  archer demo), so the arm alone carries the bow up and down. A guard held any other way makes the bow turn over in
  the hand on the way up, and the string sweeps the forearm whichever way it turns (four attempts at steering it
  through all failed: pops of 68 degrees, the bow through the jerkin). While the bow travels keep the arrow line
  `STRING_CLEAR` (48 degrees) off the forearm line, on the side it already is.
- Bring the bow up late (after the hand has left for the quiver), not across the face while the head still turns.
- A tall bow's lower limb must not sweep through the legs: check `weapon_in_body` on the stepping ticks.
- **An archer stands tall on a long stance.** Round 1 sank the pelvis 3 px over feet 4 px apart: both knees near 90
  degrees through the hold, which he called "abnormal". Pelvis about 1.2 px under the guard's, knees 47 to 52 degrees
  (the guard's are 43 to 46; print `legs[i]["<side>_shin"][0][0]`), feet 7 px apart fore and aft, the string-side
  foot back and turned out about 45 degrees.
- **One foot in the air at a time**, out and back. Plant the first before the second lifts (step tuples must not
  overlap). Two feet off the ground at once reads as a hop, and he marked every stepping tick "ok", not "good".
- **Clip length is gameplay.** "shot seems quick": the ticks to the loose come from `sheets/design.json`
  (`impactTicks`), and in game the hold at `full` adds only 0 to 3 ticks (`MotionMath.LOOSE_MAX`). Do not slow a
  clip to answer a note like this; say where the number lives and ask. Asked, he set the shot at **22 ticks to the
  loose, same damage per arrow** (2026-11-11; it was 16). The demo is 32 ticks, `full` at 17, `impact` at 22; the
  animation lead applies 22 to the design sheet and the bow builder.
- **Re-time with one map, not key by key.** The demo's `RETIME` is a piecewise-linear map from the old clock to the
  new one (`w(t)`), and every key, step and marker goes through it. Keep each piece near a plain stretch, and keep
  the keys at their exact warped times: snapping them to the quarter-tick grid moved the reach's elbow from 103 to
  110 degrees. Re-run the string, grab, nock and loose measurements afterwards; they held to 0.1 px.
- **The knee tracks over the toes.** Archer round 2, his words: "why does its feet/ankles look abnormal? the bottom
  flat piece is flush with the ground but then its lower leg is at an angle and it makes a weird crease." Cause:
  `K.attack_legs` turns each thigh to face the troop's FRONT whatever the foot does, so a foot turned out 44
  degrees under a pelvis turned 72 put the ankle at pitch -51, twist 26, roll -38. A foot is a flat box: any ankle
  angle past about 30 degrees shows as that crease. Fix, in this order: (1) turn the thigh with the foot
  (`shot_legs` and `KNEE_TURN` in the archer demo: the knee yaw eases in with the step), (2) aim the foot where the
  leg can follow (-36 and 6 degrees, not -44 and -18), (3) rise 0.5 px into the draw so the shins stand nearer
  upright (more and the rear ankle goes out of reach). Result: right ankle (-27, -6, -12), left (-8, 1, 5).
  Print the foot's local rotation per tick (the third value `K.leg_pose` returns) on any side-on stance.
- **Keep**: the head settling onto the arrow during the draw ("nice touch"), the elbow-first reach to the quiver, the
  arrow going onto the string, shoulder and elbow at full draw, the loose (all graded good).
- At full draw limit the head (16 degrees off the foe) and the chest (84 degrees side-on) in the solve, or `head_turn`
  fails: an archer looks down the arrow with the head turned about 70 degrees on the chest, no more.
- The anchor is beside the ear, outside the hood. The fist flies back along the arrow's line at the loose.
- Gameplay markers stay: `full` (the controller holds there) and `impact`.
- **The loose is the fastest thing in the clip.** In the first build the hand whipped from the quiver to the string in
  0.8 ticks, faster than the loose (18.4 against 9.7 px of motion a tick), so the shot read as an afterthought. Give
  quiver-to-string about 2.7 ticks and the loose a 9 px fly-back of the fist in one tick plus a bow kick (24 degrees
  that rings out over 3 ticks). Print the per-tick motion and check the loose tick is the largest.
- **Solve the reach to the thing it reaches for.** The string hand's target at the quiver is the fletching cube's
  centre (within 1.5 px), not "up behind the shoulder": a reviewer read the first version as a wave at the wrong
  shoulder. Check which shoulder the quiver is on in the render before keying.
- **The arrow appears when the hand is on the string, not before and not a tick after.** Earlier pushes the forearm
  into the jerkin; later reads as a pop.
- **A hold at full draw trembles.** 1.6 px of bow-arm tremble and 1 degree of breath in the chest: no two ticks of
  the hold the same picture. Neck 60 degrees on the chest at the hold, not at the 70 limit.
- Lowering the bow: with the guard's hold equal to the aim's there is nothing to cap; the arm lowers it. (An
  earlier yaw cap of 8 degrees papered over a guard held another way.)

**7. Rules that came out of the spear in two hands.**
- The second hand is a grip, not clipping (the check knows it within 3.5 px of that fist).
- Keep the two fists 5.6 px or more apart along the shaft (`Ls`): closer, the wrist wraps sit inside each other.
- **In the guard the fists are a third of the shaft apart, not a hand apart.** Kort on the tusk's tick 0: "hand
  placement could be spaced out a tad bit more." The spear builder's guard has `Ls` 6.0; the demo uses 8.0
  (`G = nudge(SP.GUARD, Ls=8.0)`). The stance clips must take the same value or the attack pops out of the stance.
- **"Poke it out a bit more" is bought with the body, the front shoulder and the rear fist, in that order.** First
  measure which arm is straight at full reach (shoulder pivot to wrist pivot against the arm's 8.1 px). On the tusk
  it is the FRONT arm; the rear fist sits bent beside the ribs. So the point goes only as far as the front fist
  lets it, and a wide grip at full reach costs reach px for px (grip 7.4 there: +0.5 px and the right arm 2.0 px in
  the torso). What gave +1.3 px with every clip number better than before: lead foot 1.2 px further out (to the
  leg's limit: the solver says "ankle unreachable" one step past it), pelvis 1.1 px further, the leading shoulder 4
  degrees further forward, chest 2 degrees lower, and the rear fist driving the shaft through the front hand (8.0
  apart in the guard to 6.3 at full reach). Squaring the shoulders did nothing: it moves the wrong shoulder.
- **Short strides, long reach** (tusk round 2: "a little bit less body movement and more arms / thrusting the
  spear ... i like the steps but i feel like the strides are too long, shorten them up but extend the weapons
  reach"). Strides went 5.8 and 3.9 px to 3.0 and 2.0, pelvis travel 7.0 to 4.2, and the point still goes 2.2 px
  further (45.8 px ahead of the pelvis, was 41.4). The geometry: shoulders 11 px apart, fists 6, so one arm always
  crosses the chest and the crossing eats its reach (round 2's front arm spent 6 of 8 px on it). What works:
  shoulders about 35 degrees side-on at full reach with the leading one at the foe, the shaft low (fist y 14, under
  the chest), the rear arm nearly straight, and the **rear elbow carried out and level through the drive** (an `rp`
  prior on the drive keys, about `(-75, 0, 40, -25)` at full reach). Without the prior the same reach lays the
  rear upper arm 2.0 px in the chest (`armour_clip` FAIL); with it 1.87 against the guard's own 1.8. What does
  not: more chest yaw than that (the rear arm lies across the chest), a chest-height thrust (shaft 1.8 px in the
  torso), sliding the shaft in the rear hand (the grip section is thicker than the shaft), fully side-on (loses
  the rear shoulder). Add an in-between key each way where the fist passes the flank, a pixel outside it.
- A deeper lunge needs the rear foot to let go earlier (lift at 7.2, not 7.8) and the lead foot to lift earlier on
  the way home, or the planted ankle goes out of reach by a few hundredths of a px and the build stops.
- A load must show from the side: sit the pelvis back and down over the rear foot, draw the **front** hand back with
  the shaft (large `Ls`), level the point onto the target. A fist that only slides back along a still shaft reads
  as nothing.
- Drive one tick past the impact (hips and chest keep going, head down), then rip the point back out before the
  feet walk home. A thrust that stops on the impact tick gets "no follow-through" from `phases`.
- **Reach limits the pose. Know which key buys it.** Two fists share one shaft, so when one arm cannot reach, the
  *other* elbow folds to make up for it (scorecard `elbow_bend`, limit about 117 degrees on the tusk; `focus_check`
  has no row for it, so read the scorecard line) or `grip err` passes 2 px.

  | What you see | Key that buys the reach |
  |---|---|
  | front (left) arm straight, rear elbow folded past the limit | more + torso yaw (brings the front shoulder forward), or a smaller `Ls` (never under 5.6) |
  | rear fist cannot get forward or up (`grip err` over 2) | less torso yaw (a squarer chest brings the rear shoulder forward), more forward lean, or hips further forward |
  | both arms straight | move the hips toward the target; never shorten the blow |

- **The load has a safe window for the rear fist.** On the tusk: x -6.0, y 12 to 13, z -2.6 to -4.6 (troop px), point
  0 to -5 degrees. Lower and the butt enters the kilt (`weapon_in_body`); further out and the fist enters the hip
  skull (`armour_clip`). On another troop find the window with numbers, not renders:
  `python tools/anim/fit_pairs.py --troop <troop> --clip <clip> --anim <copy> --from 1 --to 6`.
- **A key at an extreme has no speed.** `K.curve` is a monotone curve that carries its slope through a key only when
  the values keep going the same way. Put the impact key **short of** the full reach and the full reach one tick
  later, and the point is still moving at the impact. An impact key at the full reach is a stop on the impact, which
  `phases` reports as "no follow-through". The same row prints "lands at N% of it": under 50% the weapon has slowed
  before it hits. The tusk demo's first fix round spread the drive evenly over two ticks and landed at 44%; the travel
  belongs in the last tick before the impact, with a slow start.
- **What makes a thrust read HIGH** (`attack_high_thrust`; WM_TUSK-REVIEW: "dips and drives up at the face"). The
  level thrust drops the hips and lunges with the point at 2-3 degrees and the chest leaning 24-29. The high one
  loads LOW (pelvis about 3.5 px down, point 0 to -5 degrees), then the legs spring: hips rise through the impact,
  the rear fist climbs about 4 px, the point ends 20 to 24 degrees up, the chest stays tall (lean under 20) and the
  head tips up (pitch about -10). Over about 24 degrees the front fist reaches the face (`armour_clip`). These
  numbers come from the Sonnet-only trial on 2026-11-11 (0 FAIL, self-graded 7, not reviewed): a starting point.
- The solver's warm start can leave the last sample a few degrees off the first (`settle`): ease the arm bones onto
  sample 0's own solution over the last 3 ticks.

**8. Then the whole unit:** `fullbody.soft_motion(upper, legs, report=True)` before baking. The tusk's old thrust
had six pieces that never moved.

**9. Check, score, look, report.** Same gates as below, with the commands above pointed at the troop and the copy
(the scorecard's Python line takes any troop). Results on 2026-11-11, for calibration:

| Clip | Exported (the owner's) | Demo |
|---|---|---|
| archer `attack_direct` | scorecard FAIL `pen_limb` 2.55; a 124 degree pop at the loose; fist inside the hood | focus 0 FAIL, 2 WARN; scorecard pass |
| wm_tusk `attack_thrust` | focus 4 FAIL (`weapon_in_body`, `armour_clip`, `secondary_life`, `settle`) | focus 0 FAIL, 4 WARN; scorecard still FAIL `pen_limb` 2.0 (limit 1.39): the guard stance holds the left arm 1.8 px in the chest, the owner's to fix |

| wm_tusk `attack_high_thrust` (built by Sonnet 5.5 high from this skill alone) | focus 4 FAIL | focus 0 FAIL, 4 WARN; scorecard `pen_limb` 2.0, inherited; its author's score 7 |

Kort's grades, 2026-11-11: archer 8, tusk no score ("stance and body movement look good"), both with notes that the
rules above now carry (`kort-verdicts.md`). Round 2 of each waits for his grades. A quadruped (wm_hound) has not been
tried.

## Merging into the game file

The knight's exported animation file is layered (later builders merge their clips into it). A full-body clip is one
more layer: the builder replaces only its own two clips in
`src/main/resources/assets/gravesandglory/animations/troop/<troop>.animation.json` and asserts that both names
already exist. Rebuilding the troop from its base builder restores the old clip, so the full-body builder runs last
(`pipeline.md`). Prove it with `git diff`: only the clips you meant to change differ.
