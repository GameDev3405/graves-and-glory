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

## Batch review 2026-11-11 (branch anim/review-harpooner, cut from anim/fix-singles)

Kort's four focus points and the batch brief (`bb-anim-skill-fable/work/batch/BRIEF.md`). All changes are in
`art/anim/thrower/build_thrower_anims.py` (builder, then export, the shared stances / reactions / charge layers, then the new
`--post` step for the shared-layer clips). Logs: `work/review/tc_harpooner.{before,after}.{txt,json}`, scorecard logs beside them.

**Focus check:** before 16 clips with a FAIL (about 50 rows); after 11 clips, 15 rows, all listed below as rig/tool rows or
open. **Scorecard:** before attack_throw knee_bend 108.12 FAIL; after 34 clips, 0 failures, exit 0.

### Drift (no edit of mine)
- **Deaths (4):** the builder no longer reproduces the committed deaths (right_hand track differs; the committed file
  carries a dead weapon_t3 track). The rebuild FAILs (death_left pop 22; death_right roll_spin 240, pop 58), so the committed
  deaths are kept: the rebuild step copies them from the committed asset into `out/` before export.
- **`out/` was stale:** the committed `out/` predates f6b5064a (own ward, shield_up, brace; deaths). It now matches the asset
  for brace, ward and the deaths; those asset clips are byte-identical to HEAD.

### Changed clips and why
- **attack_throw (+legs), whole-body rework.** Same length (36 ticks), impact 8, markers kept. The point leads from the
  cock-back to the release (twist-limited reach, the harpoon frame carried, never rolled about its length); hips drop
  and a step drive it; the release is on tick 8 and the arm carries across. Fixed: roll_spin 203, pop 69, elbow 171, knee_bend
  108, right_bracer into head, head into wrap, secondary_life. The stance_aim clips follow (stances.py takes the aim pose
  from this clip's full-draw frame; only their hanging pieces move).
- **Hits (light x4, medium x4, block):** soft hanging pieces (eased so the scarf does not pop), the plate and sash ride,
  a minimum ride for the alive pieces on medium hits and the block, the right arm rolled half out so it does not cross the
  head in game; the medium legs rebuilt per hit.
- **hit_knockback, hit_pulled, stun, stun_out (post step):** the harpoon grounded by one eased local tip, eased feet lift
  where gear goes under the sole, the arm calmed and rolled out, soft pieces and skirt clearance on the composite.
- **stance_fallback, stance_rest (post step):** harpoon off the floor; fallback skirt clearance search.
- **stance_shield_up:** left arm lp (-140,0,-25,-95) -> (-115,0,25,-120): the forearm sits before the brow instead of
  3.1 px in the head (now WARN 1.55 / 1.85).

### Remaining rows (frames in `work/hp/look/`)
- **Tool rows, not faults:** the additive clips (hits, knockback, pulled) are judged on the bind pose. On the in-game
  composite (ready stance + offsets, `work/hp/compose.py`) all medium hits, block, light hits have 0 CLIP/LIMB FAILs; the
  elbow 148/164.6, the knee -9.01 (medium_back 11.5), bow_t3_rest floor, left_arm into torso and forearm into skirt rows exist
  only on the bind pose. The composite's only rows are `settle` (the ready loop at a different phase at the clip end; the
  same value for every direction) and the skirt rows below.
- **attack_throw weapon_in_body 2.0 / floor 6.66 on bow_t3_rest:** the hidden spare harpoon (draw channel hides it; the
  check ignores the draw channel). Not visible in any frame.
- **Open, ankle-length skirt:** walk 2.34, run 2.53, charge_run 2.53 (foot gear into skirt), fallback 2.12 (kelp_shin),
  knockback/pulled composite 2.05 / 2.2. The skirt is a tube from y 2.5 to 14 parented to the hips; the swinging boot passes
  inside it. No skirt pitch (+-20) or lift (0-2.5) clears it without driving it into the torso. A model or gait question.

### Rule 6 (by eye)
Hold: harpoon levelled forward like a spear. Shield_up: harpoon low and level, left forearm raised as cover; from the side
it reads close to hold. Wall: crouched and cocked to throw. Ward: an underhand fend. All suit a harpoon thrower.

### Tests
See the commit message for unit tests, verify fast and the geo-knight reel.
