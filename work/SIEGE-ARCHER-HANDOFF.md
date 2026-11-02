# Siege archer climb handoff (2026-11-02)

Branch fix/siege-archer-climb from siege/clock-200 (92dd5ab). Fix commit 71ba622. `python tools/verify.py fast --offline`: pass
(work/verify/20261102-000720-60712). Spec.java and sheets/preflight.json CRLF diffs reverted.

## Root cause
Tactics.walk (Tactics.java:282 and :313 on 92dd5ab) snaps every goal to Arena.ground, which is terrain only and ignores structures.
The tower post at y 77 became a ground point at y 65. The slot-arrival check (Tactics.java:176) was horizontal only, so archers
stopped at the tower's foot. Battle.formation also put ranged slots 5+ blocks behind the post, off the tower top, and Attack's
24-block chase and back-off would walk them back down.

## Fix (71ba622)
Assignment.perch, set by SiegeMode on the tower post. Perch slots are rows of three, one block apart, at the post's height.
walk(..., climb) paths with vanilla navigation to the raised slot once within 24 blocks. Arrival needs the height. Perched archers
do not chase or back off. TOWER_Z moves from 9 to 8 so the slots stay off the stair hole.

## Sims (gilded vs graveborn, normal, map 1, fast, 20 matches each, on 71ba622)
Roots are in the session scratchpad (run180, sim190), and eula.txt was written only there.
- Archers on walls: before the fix, 0 of 371 stage-1 ranged samples were at y 75 or above. After: 49% (180 s) and 39% (190 s) of samples.
  Archers reached the top in 22 of the 28 matches that got to the gate stage.
- 180 s: attackers 9/20 (45%). Yard falls at a mean of 163 s (147-179). Keep falls at a mean of 352 s.
- 190 s: attackers 19/20 (95%). Yard falls at a mean of 171 s (127-189). Keep falls at a mean of 338 s.

## Next
Report to the coordinator. The choice of clock is Kort's.
