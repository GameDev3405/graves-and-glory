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

## Batch review 2026-11-11

Bow-family batch review on `anim/review-bow`. Timing unchanged: `ATTACK_TICKS` 32, `FULL_AT` 16.5, `IMPACT` 22; both attacks keep `full` 0.825 and `impact` 1.1.

- **Rows before -> after** (`work/review/longbow.{before,after}.json`): clips with a FAIL 14 -> 7, FAIL rows 30 -> 11, WARN rows 34 -> 40. Every added WARN is a former FAIL (no PASS became a WARN).
- **Quiver side (named check).** Since 6935d38f the hand file rolled `tall_quiver` +8, so it leaned to the bow side, the opposite of the archer's quiver. Fixed at the source: `art/hand/longbow/build_longbow.py` `rot=(12, 0, -8)`; `out/` geo, rig and previews re-rigged and the geo copied to the resources. It now leans over the string shoulder. Not done: the legacy hand install, so `models/troop/longbow.json` still holds `[12, 0, 8]` (the GeckoLib geo is what the troop renders).
- **Changed clips (25, spliced; every other clip byte-identical to HEAD).**
  - `attack_direct`, `attack_volley` (+legs): Kort's archer r3 shot, fitted to the long stave and tall quiver (`direct_shot.FIT["longbow"]`): bow canted -30 deg at the nock and raised 20 deg so the lower limb clears the ground (floor 2.04 -> 0.9), uncanted by full draw; the pluck grabs the fletching's outer, front, top corner of the deep quiver (shoulder 162 deg -> pass); quiver roll -8. The fist no longer goes through the face (3.72 -> pass) and the 130 deg loose pop is gone.
  - Aim stances: from the r3 full-draw frame, chest takes 85% of the pitch.
  - Hits and `hit_block`: knight legs, gear swings (`tall_quiver`, `long_feather`, skirts, pouch) soft and low-passed.
- **Knockback "face into tall_quiver" (named check).** `hit_knockback` is merged by `reactions.py` (common module, not editable here). The quiver fix took it from 2.16 to 1.95 at t3, still over 1.9. Its bow under the ground 2.93 at t5.25 is in the same module.
- **Stances (named check).** Hold and wall: bow upright, string hand raised (can read as a salute, pre-existing). Ward: bow held across, borderline for a bowman. Brace and fallback fine. None is a sword or shield pose. Not re-authored.
- **Remaining FAIL rows.** Common modules: `hit_pulled`, `stun_upper`, `stun_out_upper`, `hit_knockback` (floor, still gear, face into quiver), `charge_run_upper`. Gait: `run_upper` skirt into jack 1.98 at t2. Builder: `hit_medium_right` bow under the ground 1.62 at t3.75 (pre-existing, second fix round not spent on it).
- **Proposed exemptions.** longbow, `run_upper`/`charge_run_upper`, `armour_clip`, 1.98, skirt into jack (cloth layers). longbow, `hit_knockback`, `armour_clip`, 1.95, face into tall_quiver at t3 (the knockback tumble throws the head back into the quiver top; fix belongs in `reactions.py`).
- **Tests.** Scorecard exit 0 (35 clips, 0 fail). Unit tests OK (see ARCHER-REVIEW batch section). Booth config valid.
