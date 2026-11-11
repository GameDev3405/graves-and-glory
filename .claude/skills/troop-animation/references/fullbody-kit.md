# The full-body kit: authoring an action clip in world space

`art/anim/common/fullbody.py` turns a short spec (where the pelvis, feet, hand and blade are, tick by tick) into the
two GeckoLib clips the game plays (`<name>` and `<name>_legs`). You say where things are; it solves the joints, keeps
the edge leading, and refuses a target the arm cannot reach. Worked example, read it first:
`art/anim/knight/build_overhead_demo.py` (the knight's overhead cut, the skill's demo).

Use it for attacks and other one-shot action clips on the knight skeleton (knight, sentinel and the troops built on
`art/anim/knight/build_anims.py`). Idle, gait, stances, hits and deaths keep their own systems (`pipeline.md`).
Two-handed weapons, bows and non-knight rigs are not supported yet: extend the kit, do not fake it with a spec.

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
5. **Shield.** Then the cape follows by itself.
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

- `scorecard.py` on the command line only reads the exported file; the Python line above is how to score a copy.
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

## What the edge logic does (so you can tell when the spec is wrong)

When the tip is slow the hand keeps its natural grip. When the tip is fast the kit rolls the hand about the blade
so the edge is on the tip's line of travel, and lets go over 2.5 ticks after the cut. So:

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

## Merging into the game file

The knight's exported animation file is layered (later builders merge their clips into it). A full-body clip is one
more layer: the builder replaces only its own two clips in
`src/main/resources/assets/gravesandglory/animations/troop/<troop>.animation.json` and asserts that both names
already exist. Rebuilding the troop from its base builder restores the old clip, so the full-body builder runs last
(`pipeline.md`). Prove it with `git diff`: only the clips you meant to change differ.
