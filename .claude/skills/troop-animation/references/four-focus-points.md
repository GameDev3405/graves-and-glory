# The four focus points and their checks

Kort's four complaints (clipping, stale motion, weapon facing, limbs; `kort-verdicts.md`) are measured by
`tools/anim/focus_check.py` on every quarter tick of every clip. A clip with a FAIL is not shown to anyone.

```
python tools/anim/focus_check.py --troop knight                         # every clip, FAIL and WARN rows only
python tools/anim/focus_check.py --troop knight --clip attack_overhead --verbose --ticks
python tools/anim/focus_check.py --troop stalker --anim other.animation.json   # another build of the same troop
python tools/anim/focus_check.py --all --json work/focus.json
```

One clip takes a few seconds on an idle machine and up to 45 s when other builds or a game client are running, so in a
sweep run
`fit_pairs.py` on the ticks you are working on (under a second) and the full check when a track is done.

`--ticks` prints one line per **quarter tick** (line `i` is tick `i / 4`) with a column for each check that has a
value on every sample: the clipping rows (`weapon_in_body`, `limb_in_body`, `armour_clip`, `floor`) among them. It is
the fastest way to see whether a change moved the worst tick or only its neighbour. For a sweep, import it:

```
python -c "import sys;sys.path.insert(0,'tools/anim');import focus_check as fc;c=fc.check_troop('.', 'wm_tusk', anim='work/<job>/wm_tusk.animation.json', only=['attack_thrust'])[0];print([(r['check'],r['status'],r['value'],r['tick']) for r in c['results'] if r['status'] in ('FAIL','WARN')]);print(max(c['ticks']['armour_clip']))"
```

Exit 1 on any FAIL. Each row gives the worst value, the tick it happens on and the parts involved, so the fix is
aimed: go to that tick in the builder, do not nudge the whole clip. The limits and the reason for each are in
`LIMITS` at the top of the tool.

## What each check means and the fix that works

| Group | Check | It caught | Fix |
|---|---|---|---|
| CLIP | `weapon_in_body`, `weapon_in_arm`, `weapon_in_prop` | blade through helm, shoulder or shield | move the **hand** at that tick (carry it further from the head), not the blade angle; a blade angle change breaks facing |
| CLIP | `prop_in_body`, `limb_in_body` | shield rim in the thigh, forearm in the chest | move the fist out or down; turn the shield face; open the elbow |
| CLIP | `armour_clip` | "the textures on its chest are glitching into each other": plates of two different bones in the same place, even 1.7 px deep | open the arm away from the chest at that tick; a shoulder plate stays on its arm (`fullbody.plates_for` only lets it trail 10 degrees and slide down the arm), so turn the head or the torso less instead. Pieces of the same limb (knee guard and thigh) are not judged: a hinge overlaps. The row names one pair; `python tools/anim/fit_pairs.py --troop <troop> --clip <clip>` lists every pair and what the clip adds to it |
| CLIP | `floor` | tip or knee under the ground on a follow-through or a crouch | raise the hips at that key or flatten the blade; never lift the whole clip |
| STALE | `dead_hold` | a frozen frame inside an action | a held pose still drifts: key the hold's start and end a little apart |
| STALE | `loop_life` | an idle that is a statue | breath plus a weight shift; see the 25-40% sway rule before adding more |
| STALE | `whole_body` | an arms-only swing | drive it from `hips`, add a real `steps` entry, move the off hand |
| STALE | `phases` | a swing at one speed | hold the load 2+ ticks, put the peak tip speed on the impact, carry through after it. The row prints "lands at N% of it": the tip's speed over the half tick before the impact against its peak. Under 50% the blow has slowed before it hits (the approved knight thrust lands at 56%, the overhead cut at 75%): start the drive later and slower, and put the impact key short of the full reach |
| STALE | `secondary_life` | cape, plume, skirt and shoulder plates standing still (a helm's fixed crest is meant to: Kort had its wobble removed) while the body fights. "i want the whole unit to be animated including pieces of its armor" | pass the samples through `fullbody.soft_motion(upper, legs)` before baking; the row names the pieces that are still. A piece it does not find: add its name token to `focus_check.ALIVE_KINDS` or `ALIVE_NAMED` (then both the check and the kit see it) |
| FACE | `edge_lead`, `point_lead` | blade flat to the target, a thrust travelling sideways | the kit rolls the edge onto the tip's path; if it fails, the path itself bends too sharply: smooth the `reach`/`blade` keys around that tick |
| FACE | `grip_roll` | "weapons spins around in hand" in a wind-up: the weapon turning about its own length before the strike | never key a hand roll outside the strike; the kit only rolls the edge onto the cut from 3 ticks before the impact. In your own builder, carry the blade with the wrist's swing, not its twist |
| FACE | `at_foe`, `guard_up` | blade behind the hand at the impact, a guard hanging down | fix the `blade` key at the impact |
| FACE | `roll_spin`, `wrist` | sword spinning in the hand, blade folded back along the forearm | the arc is being taken by the wrist: move the hand along the arc so shoulder and elbow take it |
| LIMB | `elbow`, `knee`, `knee_aim`, `shoulder` | joints bending a way a body cannot | the hand or foot target is out of the comfortable range: bring it closer or move the hips toward it |
| LIMB | `pop`, `jitter` | a glitch frame, a bone flicking back and forth | two keys too close together, or an euler branch flip: space the keys, check `between-key error` in the build output |
| LIMB | `settle` | a clip that does not end on its start pose | first and last key are the guard, exactly |

## Three rules that change a verdict (2026-11-11, from the archer and the Warmaw tusk)

- **A second hand on the weapon is a grip.** When the other fist is within 1.5 px of the weapon (a spear, a bow's
  string hand), the weapon inside a 3.5 px ball around that fist is not `weapon_in_body`. Outside the ball it still is.
- **A weapon the game hides is not judged** (2026-11-12, harpooner). On ticks where the clip keys the `draw` bone onto a
  draw frame with no cubes (the fist after the harpoon left), the weapon rows and `floor` skip the weapon. A bow's pull
  frames have cubes, so a bow is always judged. Do not exempt such a row; if it still fires, the weapon is drawn there.
- **Pieces of one limb are not judged against each other** in `armour_clip` (elbow and knee gear overlap by design).
- **A clip does not own the clipping its stance starts with.** If the first frame of an action clip is already over
  the warn limit and the clip never gets more than 0.15 px worse than that, the row is a WARN that says "the stance
  it starts from already has X: fix the stance". Report it to whoever owns the stance; do not hide it. If the clip
  makes it worse, the clip owns the FAIL.
  Exactly: the baseline is the same row's value on **the clip's own tick 0** (not the stance clip, and whatever pair
  is worst there). The row becomes a WARN only if that value is over the row's WARN limit **and** the clip's worst is
  at most 0.15 above it. Example: `armour_clip` is 1.83 at tick 0 (WARN limit 1.7), so the clip may reach 1.98; at
  2.00 it is the clip's FAIL. Read the tick-0 value from the `--ticks` column before you plan a pose near a limit.

## Rules for using it

- **n/a is not pass.** A check that does not apply to a clip says so. Read the n/a rows of a new troop once: if
  `edge_lead` is n/a on a troop with a blade, the rig rules are missing its blade bone, and nothing is checking facing.
- **Never loosen a limit to pass a clip.** A real exception goes in `tools/anim/focus_exemptions.json` with troop,
  clip, check and the reason (e.g. a shield planted on the ground in a wall stance is below the sole on purpose).
  An exemption without a reason a reviewer would accept is a defect, not an exemption.
- **Deaths are not gated on clipping.** A body lying on its gear touches it. Those rows are WARN for the reviewer;
  the v4 death probes (`lessons.md` section 4) are the gate.
- **The numbers are necessary, not sufficient.** The limits were set on the knight skeleton with a one-handed blade.
  On 2026-11-11 the check was run on the stalker Kort rejected and the stalker he kept: it failed most clips of both
  and did not tell them apart. So: (1) on any rig other than the knight family, first run it on that troop's kept
  clips and record which rows are rig differences; (2) passing it never means the clip is good. Taste is judged by a
  fresh reviewer and then by Kort, frame by frame.
- **Run both tools.** `scorecard.py` asks whether a clip is inside the approved knight's envelope; `focus_check.py`
  asks the four questions with absolute limits. They disagree sometimes; a clip goes forward when both pass.
  Two scorecard rows have no twin in the focus check and are the ones a two-handed or bow clip trips: `pen_limb`
  (a limb inside the body, against the knight's own worst) and `elbow_bend` (an elbow folded further than the
  knight's deepest plus 10 degrees).
- Tests: `python -m unittest discover -s tools/anim -p "test_focus_check.py"` proves each of the four faults, and each
  fault Kort graded on 2026-11-11 (class `Graded`), is caught when it is put into the knight's overhead cut on purpose. When a new kind of fault gets past the tool, add the check and a test
  that plants the fault.
- **Hits are judged over the stance (2026-11-12).** The game adds every `hit_*` clip and `stun_out_upper` onto the pose
  the other layers made (`HitController.add`), so the focus check, the booth sheets and the phone videos now play them
  added onto `stance_ready_upper` + `stance_ready_legs`. Over the rest pose a hit showed false floor, elbow and knee
  rows (five of the knight's old baseline exemptions were only that) and hid real clipping: the hound's medium hits
  put the chest 2.6 px into its skullcap over the stance. Render a hit the same way:
  `render_geo.py ... --clip stance_ready_upper --clip stance_ready_legs --add-clip hit_medium_front --add-clip hit_medium_front_legs`.
  A hit must be zero at both ends (`settle` compares it with the stance under it). `scorecard.py` still scores hits
  over the rest pose. `focus_check.py --rest-pose` gives the old numbers.
  **The stun shake-off starts from the stun**: `stun_out_upper` is checked and shown the way `StanceController` plays
  it, `stun_upper` + `stun_legs` giving way to the ready stance over 8 ticks with the clip added on top
  (`render_geo.Scene(handover=...)`). Added onto `stance_ready_legs`, the stun's knee bend counted twice and every troop
  showed a false floor row (hearthguard 1.87, spider 4.26). Its tick 0 is the stun pose: a floor row there belongs to
  `stun_upper`. `whole_body` is n/a for it (it has no legs clip of its own).
- **Booth sheets end on the clip's last pose (2026-11-12).** A play-once clip has stopped at its length and the renderer
  returns the bind pose there; `booth_render` now takes that last frame a hair before the end. A sheet whose last frame
  stands in the bind pose was built before this fix: rebuild it.
