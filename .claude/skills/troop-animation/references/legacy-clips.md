# Legacy clip pipeline (troops not yet on GeckoLib)

Still live on 2026-11-09: the game draws every troop outside `GeoTroops.APPROVED` with these clips, and
`python tools/verify.py fast` runs `python tools/clips.py --verify`. Do not extend it. Use it only for a fix Kort asks
for on a troop's old model; rebuilt troops are improved through their GeckoLib builder. `docs/ANIMATION-EDITING.md`
is the user guide and wins over this file.

## Data flow

```
art/clips/<rig>/<family>.bbmodel                 bare rig plus its animations (edit only through anim_edit)
  | python tools/anim_edit.py open <family> [--troop <id>] [--fresh]   -> working copy with the real model
  | (edit in Blockbench, save)
  | python tools/anim_edit.py apply              -> writes clip JSON, runs the checks
src/main/resources/assets/gravesandglory/clips/<rig>/<family>/<clip>.json
  | ClipLibrary -> TroopMotion -> MotionEngine (MotionBody, MotionStack, MotionFeet) -> bones
```

- Never run `art/clips/build_clips.py` in write mode: it overwrites hand edits. Generated families come from
  `art/clips/build_attacks.py`.
- Clips are additive offsets on the stance: lint the net pose (stance plus clip), not the clip alone.
- Rig space is ModelPart: y down, -z forward, ZYX degrees. Forearm rx must be 0 or negative (positive is a backwards
  elbow); shin rx 0 or positive; root +y sinks. `art/hand/common/handlib.py` `JOINT_LIMITS` clamps at runtime.
- `tools/clips.py` refuses: unknown bones, Molang, scale keys, keys or markers off a 0.05 s tick, an `impact` marker
  not equal to `impactTicks`, a melee attack without one, an attack longer than 40 ticks, a looping or floating death,
  a loop that does not close, and an attackVariants row with no clip (`test-tools/standalone/test_attack_clips.py`).
- 64 bones maximum per old model (`TroopModel.MAX_BONES`).

## Commands

```
python tools/anim_edit.py open <family>
python tools/anim_edit.py apply
python tools/anim_edit.py preview <family> <clip>   # prints the motion-grade harness command
python tools/clips.py --verify                      # committed JSON matches sources
python tools/verify.py fast
```

Legacy troops have no scorecard, no booth and no geo-knight harness. Evidence is `python tools/clips.py --verify`
plus the motion-grade harness sheets (`GRAVES_PHASE2_CHECKS=motion-grade`, or `turn`; see
`test-tools/isolated-checks.gradle`; `anim_edit.py preview` prints the command), never desktop computer-use. The
review loop and rubric in `review-loop.md` apply; cite clip JSON keys instead of builder lines. Kort approves in chat
("show me first") before the lane lands through `tools/land.py`.
