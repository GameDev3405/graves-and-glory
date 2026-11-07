# ic_arbalist review record (2026-11-06)

The first crossbow unit, the Ironclad arbalist (pillager). Its own builder, `python art/anim/crossbow/build_crossbow_anims.py`, solves the crossbow (`bow_t3_*` bones) to the right fist and the left fist to the fore-stock every tick. Its rig comes from `art/anim/common/troop_rig.py ic_arbalist`. The attack, `crossbow_loose` (50 ticks, impact 4), aims, looses, kicks, lowers, spans the string with the foot, hauls, reaches to the hip quiver, places the bolt and settles back to ready.

- **Weapon bones.** The crossbow bones are named `bow_t3_*`, not `weapon*`, so the death floor-fit and the flat-hand pass treated them as body. `is_weapon(bone)` now covers both, which keeps deaths on the ground.
- **Quiver reach.** It blends by joint angle, not by fist position, so the arm cannot fold through the torso on the way to the hip.
- **Butt-anchored keys.** The kick, the lower and the quiver pose pivot about the stock's butt (`about_butt`), so the crossbow tips instead of sliding through the hands.
- **Round 2 changes.** The prod sits lower and further forward through the span and haul (`SPAN`, `HAUL_A`, `HAUL_B`). The quiver pose tips 40° instead of 50°, and the recoil kick pitches 9° instead of 12°.

## Gameplay change (cosmetic, logged as an assumption under WORKFLOW.md)

A crossbow shot's attack_v1 event was only sent at the loose. Its clip therefore arrived late, faded in (weight about 0.08 at impact), and skipped the raise-to-aim.

- **The change.** `Tactics.announcesAhead` now covers non-magic bow and crossbow troops, so a crossbow shot is announced on the beat before it, as a bow's is (R110 Plan B). For a crossbow, `Projectiles.drawAhead` starts the clip at `beat − impactTicks`, the tick `loose` stamps, so the client keeps the first copy and drops loose's duplicate.
- **What does not change.** `drawsAhead`, `looseTicks` and `hold` stay bow-only, so bolt timing, damage and cooldown are unchanged. If the target dies or the troop is stunned in that beat, the aim plays with no bolt, as a bow's draw does.
- **Ballista Crew.** `ic_ballista` (also the crossbow family) gets the same early announcement on its existing renderer. It has not been tested.
- **The harness.** `GeoKnightChecks` duels crossbow troops at range. Its shots are spaced 60 ticks apart (the clip is 50), its frame checks cover the first 30 clip ticks (shot, kick and span), and its gap window sits at 55.

## Review

- **Round 1 (Sonnet high, reel `ic_arbalist_v1`): SHIP WITH MINORS.**
  - After the loose, the crossbow rose upright beside the helm (t306–313).
  - It snapped down to the feet for the quiver reach (t316–325).
- **In-game checks run 1: FAIL, 11 of 51 checks.** The harness duelled the crossbow as melee, and in game the clip faded in late. Both are fixed above.
- **Round 2 (Sonnet high, reel `ic_arbalist_v2`): SHIP WITH MINORS.**
  - The post-loose pose is improved: the weapon is lower and diagonal, and its tip stays below head height.
  - The drop to the span reads as spanning with the foot but is still quick.
  - No new issues, and no regressions on the hit sheet.
  - That was the second fix round, so the remaining items are recorded below.
- **In-game checks run 2: FAIL, 2 of 51.** The walking sync duel saw too few swings. Fix: `GeoKnightChecks.nudgeR` spaces walking crossbow shots 40 ticks apart (60 standing).
- **In-game checks run 3: FAIL, 2 of 51.** Sync passed (stand 9/9, walk 4/4). Only `march-walk-side` failed (median 0.173 vs 0.151), plus the suite total. Heavy rig builds were running alongside and the log shows server hitches; runs 1 and 2 measured 0.151 exactly on the same code.
- **In-game checks run 4: PASS, 22 of 22 checks, 0 false**, with no other heavy work running.
- **Verdict: finished.** Video: `ic_arbalist_v2.mp4`.

## Open minors (polish, not blocking)

- **Crossbow near the face.** In front and three-quarter view at t313, the crossbow passes close to the chin. It does not clip.
- **Drop to the span** (t316.9 → t319.8) is fast, with no gradual lower between.
- **Left forearm** comes near the head during the span (t319.8–322.8).
- **Heavy hit** reads extreme.
- **Death ground contact** is hard to judge at reel size.

## v2 feel (2026-11-07, branch anim/v2-ranged)

The Knight v2 feel (see `ARCHER-REVIEW.md`, "v2 feel"), built by `art/anim/crossbow/build_crossbow_anims.py` with the crossbow's own values. The shot (`attack_loose`, 2.5 s, impact tick 4) and the four deaths are byte-identical to the previous export, so the server sync is unchanged.

Changed clips:
- **Idle:** a contrapposto weight cycle (hips slide over one foot and back, the free hip drops, the chest counter-tilts, the arms trail a beat late). One breath and one full shift per 3 s loop.
- **Brace:** a deeper, wider stance (hips down, feet out, yaw set apart from the ready footing) with the crossbow held out front.
- **Hits:** graded light / medium / heavy. Light is a small flinch at the knees; medium and heavy are pelvis-driven staggers; the crossbow arm opens outward in the front hits.

- **Probe.** Shaft or fist in the body: 0 samples in every clip. Grip error in the shot: max 3.7 px (unchanged). Deaths and shot byte-identical to HEAD.
- **Review.** Sonnet high, 2 rounds.
  - Round 1: FIX on the idle (weak weight shift); stances, hits and reload passed.
  - Round 2: idle amplitude raised and the chest counter-tilt relaxed; still judged FIX for the idle (low-resolution sheets), no new clipping or pops. Two fix rounds spent, so the rest is recorded as a minor.
- **Open minors.**
  - Plain idle weight shift is still subtle from the front on the contact sheets (hip roll about 5 degrees, 2 px slide); check it in-engine.
  - Walk and run show little pelvis sway on the sheets.
  - Light hit is close to idle at t=1-6.
  - Medium hit from behind (t=12) drops the crossbow low; heavy front (t=6) pitches the head close to the chest plate; medium hit from the right splays the arms.
  - Brace and hold-the-line are close from the front.
- **Harness.** geo-knight suite with `GRAVES_GEO_CHECK_TROOP=ic_arbalist`: **22 `[p1] p1-` lines, 0 `=false`** (a first launch hit a transient classpath failure before any check ran; the rerun passed, with two "Can't keep up" log lines and no lag failure).
- Video: `group_d2_v2` (ic_arbalist, then tc_harpooner).
