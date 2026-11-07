# tc_harpooner review record (2026-11-06)

The Tidecaller Harpooner, the first thrower: a harpoon (trident visual) carried on the shoulder and thrown overarm. Its clips come from the new thrower builder (`art/anim/thrower/build_thrower_anims.py`, `GG_THROW_TROOP=tc_harpooner`). Its rig comes from `art/anim/common/troop_rig.py tc_harpooner` (the new `thrower` kind, with `rest` and `pull_0` frames).

- **Clips:** guard, idle, walk and run carry, stances, hits and deaths, plus `attack_throw` (36 ticks) and `attack_throw_legs`. The timeline `impact` marker sits on the server's impact tick (8).
- **Throw:** a whole-body coil (lean back, twist, point raised, fist behind the head), a fast overarm drive and release at tick 8, an overshoot, then the next harpoon is drawn over the shoulder and brought down to guard.
- **Draw channel:** bone `draw` position x is 1 (`pull_0`, hand empty) from tick 8 to 18.25 and 0 (`rest`, harpoon in hand) from 18.5.
- **Gameplay (Kort approved, 2026-11-06):** `Tactics.announcesAhead` now includes the thrower family, so a throw is announced 10 ticks early (cosmetic) and the wind-up plays before the release. `GeoKnightChecks` runs a thrower duel at range (`THROW`: frames over the throw clip's own length, the gap window just after it).
- **Round 1 fix: tail spin.** The over-the-shoulder draw left the right arm's IK on a twisted branch, so blending the clip into guard spun the arm a full turn. A prior-pull ramp (`UNWIND`, ticks 22-35) slides the elbow back onto the guard's own solution. The clip now ends on the guard's joints, with fist error at most 2.2 px and no shaft-in-body samples.
- **Round 2 fix: wrist winding.** The bake's unwrap carried two fast wrist rolls on as whole turns, so the hand ended at (x -360, z +360), the same rotation as the guard but a full spin under the renderer's linear euler blend. `rewind()` takes the whole turns back off from the largest step in the clip's second half.
- **Also in round 2:** a deeper coil, a low planted `brace` (Hold position) and a cocked `wall` (Hold the line).

## Review

- **Round 1 (Sonnet high, reel `reel_hp_v1`): FIX NEEDED.** In the throw tail the harpoon dipped to the soles, vanished, popped overhead and swung back. Minors: weak coil, partly readable draw, subtle stances.
- **Round 2 (Sonnet high, reel `reel_hp_v2`): SHIP WITH MINORS.** The tail blocker is gone: the harpoon stays visible with no dip, vanish or spin into guard. The coil reads as a convincing wind-up; no clipping in any stance. death_left checked clean.
- **In-game checks** (round 2 clips): PASS, 22 of 22 checks, 0 false. 35 of 35 throw frames caught side and three-quarter (17 of 17 far). All 10 standing and 4 walking throws matched the server impact. Weight at impact is 1.0, and 0 between throws. In-game frames show the harpoon in flight with the hand empty, then back in hand.
- **Verdict: finished.** Video: `tc_harpooner_v2.mp4`.

## Open minors (polish, not blocking)

- **Draw (attack ticks 9-22):** the empty hand reaches back, but the new harpoon appears already vertical overhead around tick 22. No visible pull from the back.
- **Tail:** the harpoon snaps from about 45 degrees up to horizontal in one tick (reel t322-323). It reads as a slam-down, not a glitch.
- **Coil:** better than round 1, but there is no visible hold, and the hip drop is slight.
- **Stances:** ward is distinct. Brace is a shallow crouch. The reviewer saw Hold the line as close to guard at its first frames.
- The other three death sheets were not reopened in round 2. Hits, idle and locomotion showed no regressions.

## v2 feel (2026-11-07, branch anim/v2-ranged)

The Knight v2 feel (see `ARCHER-REVIEW.md`, "v2 feel"), built by `art/anim/thrower/build_thrower_anims.py`. The throw (`attack_throw`, 1.8 s, impact tick 8) and the four deaths are byte-identical to the previous export (the deaths keep the old guard: only the shipped idle uses the new weight-cycle amplitude), so the server sync is unchanged.

Changed clips:
- **Idle:** the guard weight cycle at a higher amplitude (hips shift over one foot and back, the harpoon trails the breath).
- **Brace:** lower and wider than ready, leaning forward, the harpoon pointed down at the charge; the wall legs carry the same cycle as the upper body.
- **Hits:** graded light / medium / heavy with the knight's v2 leg work mirrored for the thrower; the unshielded front hit is scaled to 0.85 so the heavy blow does not read as a tumble.

- **Probe.** Shaft in the body: 0 samples in the walk, run and throw. Throw fist error: max 2.2 px (unchanged). Feet and torso clear the floor in the unchanged deaths. Deaths and throw byte-identical to HEAD.
- **Review.** Sonnet high, 2 rounds.
  - Round 1: FIX (brace not distinct from ready; idle weight shift mild). Hits and the throw / draw-next passed.
  - Round 2: **PASS.** Brace is clearly apart from ready; no clipping, spear-through-head or floating feet.
- **Open minors.**
  - Idle lateral hip shift is still subtle from the front; it reads mainly as head and spear-tip movement.
  - Heavy front hit (t=6): the harpoon sits close to the head silhouette; no clip found.
  - Walk and run sheet samples are hard to judge at the cycle period.
- **Harness.** geo-knight suite with `GRAVES_GEO_CHECK_TROOP=tc_harpooner`: **22 `[p1] p1-` lines, 0 `=false`** (two "Can't keep up" log lines, no lag failure).
- Video: `group_d2_v2` (ic_arbalist, then tc_harpooner).
