# Longbow review record (2026-11-06)

The longbow is built by the archer's builder (`art/anim/archer/build_bow_anims.py`, `GG_ANIM_TROOP=longbow`), which gives the same solved shot, held draw and bow-aware deaths. A per-troop `CHAR` table makes the tall stave read as a heavier pull than the archer's:

- **Volley lean:** −22° (archer −16°).
- **Hip sink into the draw:** ×1.35.
- **Rock at the loose:** 3.5 (archer 1.5).
- **String-hand recoil:** larger.
- **Ward stance:** the bow is lifted (left arm −7°) so its lower tip clears the ground.
- **death_left:** the bow arm goes out to the side (−160°, forearm −5°) rather than overhead, so the long bow lies flat.

The archer's clips rebuild byte-identical with the table in place.

## Review

- **Round 1 (Sonnet high, reel `longbow_v1`): SHIP WITH MINORS.**
  - The ward bow tip touched the ground.
  - The death_left bow leaned up about 50°.
  - It read too close to the archer.
- **Round 2 (Sonnet high, reel `longbow_v2`): SHIP WITH MINORS, no blockers.**
  - **Ward:** the tip is clear.
  - **Deaths:** the bow lies flat in all four.
  - **Volley:** the lean reads clearly deeper than the archer's.
  - **Direct shot:** the string hand stays on the string through the draw.
- **In-game checks** (`GRAVES_GEO_CHECK_TROOP=longbow`): **run 6 PASS, all 51 checks.** Attack frames 25/25 at side and three-quarter and 12/12 far; sync 25/25 standing and 4/4 walking; impact weight 1.0; footwork 1.0; hits 7 blows × 3 views; deaths 4/4.
- **Verdict: finished** (visual review and gameplay testing both pass). Video `longbow_v2`.

## Open minors

- **Bow limbs in the pull frames** break into separate blocks at the tips. The archer shares this; it is an art fix in the pull-frame geo.
- **Loose:** hip sink and rock are subtle. A bigger string-hand snap and body rock would sell the weight more.
- **death_left:** the bow tucks under the torso and is hidden from three-quarter. Check that it does not intersect the body.
- **Death forward:** the topple is quick, about 7 ticks.
- **Death right:** the bow passes near-vertical for a frame mid-flail.
- **Volley front view:** the shoulder roll reads slightly twisted, though readable.
- **Ward front view:** the lower tip sits at about sole level. It is likely perspective; check it in-engine.

## v2 feel (2026-11-07, branch anim/v2-bow)

The same Knight v2 feel as the archer (see `ARCHER-REVIEW.md`, "v2 feel"), built by `art/anim/archer/build_bow_anims.py` with the longbow's own CHAR values. The draw, the shot and the deaths are unchanged, and the deaths are byte-identical to the previous export.

The longbow needs two overrides, because its tall stave hits the floor otherwise:
- In the medium hit from the right, the bow arm opens less: `BOW_ARM_RIGHT` (-4, -8, -11) instead of the archer's.
- The bow arm is held open while the hips are still low: a longbow-only track in `bow_arm_keys`, peaking at 2.3 and easing out by tick 16.

- **Probe.**
  - No bow or fist in the body in any clip.
  - Floor: the front hits -0.86 to -1.08 px. The medium hit from the right is -1.80 px: the lower stave tip grazes the floor (it was -2.84 px before the overrides). That matches the unchanged direct shot (-1.78 px) and is under the volley (-2.67 px). Hold the line is -1.60 px.
  - Largest hit pop: 29.7 px/t, at the heavy front peak, at the stave tip. The volley reaches 33.7 px/t.
- **Review.** Sonnet high, 1 round, verdict SHIP WITH MINORS.
  - Hits graded like the Knight's.
  - The stave stays upright on the bow side and opens outward in the front hits.
  - No clipping, pops or wrong facing seen.
- **Open minors.**
  - Brace is only marginally distinct from combat ready in the front view.
  - The plain idle weight shift is weak from the front.
  - The medium hits from the left and from behind pitch nearly as far as the heavy hit, so the grading is compressed.
  - In the medium hit from behind, the stave may cross the waist in the front view. The probe finds no bow in the body.
  - Heavy front peak leans about 60-70 degrees, past the archer's 45-55.
  - The stave grazes the floor by 1.8 px in the medium hit from the right (see the probe).
- **Harness.** geo-knight suite with `GRAVES_GEO_CHECK_TROOP=longbow`: 22 `[p1] p1-` lines, 0 `=false`.
- Video: `longbow_v2feel`.
