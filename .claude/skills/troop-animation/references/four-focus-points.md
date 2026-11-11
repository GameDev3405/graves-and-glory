# The four focus points and their checks

Kort's four complaints (clipping, stale motion, weapon facing, limbs; `kort-verdicts.md`) are measured by
`tools/anim/focus_check.py` on every quarter tick of every clip. A clip with a FAIL is not shown to anyone.

```
python tools/anim/focus_check.py --troop knight                         # every clip, FAIL and WARN rows only
python tools/anim/focus_check.py --troop knight --clip attack_overhead --verbose --ticks
python tools/anim/focus_check.py --troop stalker --anim other.animation.json   # another build of the same troop
python tools/anim/focus_check.py --all --json work/focus.json
```

Exit 1 on any FAIL. Each row gives the worst value, the tick it happens on and the parts involved, so the fix is
aimed: go to that tick in the builder, do not nudge the whole clip. The limits and the reason for each are in
`LIMITS` at the top of the tool.

## What each check means and the fix that works

| Group | Check | It caught | Fix |
|---|---|---|---|
| CLIP | `weapon_in_body`, `weapon_in_arm`, `weapon_in_prop` | blade through helm, shoulder or shield | move the **hand** at that tick (carry it further from the head), not the blade angle; a blade angle change breaks facing |
| CLIP | `prop_in_body`, `limb_in_body` | shield rim in the thigh, forearm in the chest | move the fist out or down; turn the shield face; open the elbow |
| CLIP | `floor` | tip or knee under the ground on a follow-through or a crouch | raise the hips at that key or flatten the blade; never lift the whole clip |
| STALE | `dead_hold` | a frozen frame inside an action | a held pose still drifts: key the hold's start and end a little apart |
| STALE | `loop_life` | an idle that is a statue | breath plus a weight shift; see the 25-40% sway rule before adding more |
| STALE | `whole_body` | an arms-only swing | drive it from `hips`, add a real `steps` entry, move the off hand |
| STALE | `phases` | a swing at one speed | hold the load 2+ ticks, put the peak tip speed on the impact, carry through after it |
| FACE | `edge_lead`, `point_lead` | blade flat to the target, a thrust travelling sideways | the kit rolls the edge onto the tip's path; if it fails, the path itself bends too sharply: smooth the `reach`/`blade` keys around that tick |
| FACE | `at_foe`, `guard_up` | blade behind the hand at the impact, a guard hanging down | fix the `blade` key at the impact |
| FACE | `roll_spin`, `wrist` | sword spinning in the hand, blade folded back along the forearm | the arc is being taken by the wrist: move the hand along the arc so shoulder and elbow take it |
| LIMB | `elbow`, `knee`, `knee_aim`, `shoulder` | joints bending a way a body cannot | the hand or foot target is out of the comfortable range: bring it closer or move the hips toward it |
| LIMB | `pop`, `jitter` | a glitch frame, a bone flicking back and forth | two keys too close together, or an euler branch flip: space the keys, check `between-key error` in the build output |
| LIMB | `settle` | a clip that does not end on its start pose | first and last key are the guard, exactly |

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
- Tests: `python -m unittest tools/anim/test_focus_check.py` proves each of the four faults is caught when it is put
  into the knight's overhead cut on purpose. When a new kind of fault gets past the tool, add the check and a test
  that plants the fault.
