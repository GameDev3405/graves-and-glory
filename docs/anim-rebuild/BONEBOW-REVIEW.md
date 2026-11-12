# bonebow review record (2026-11-06)

The fourth bow unit, a light skeleton archer. It uses the archer's bow builder (`GG_BOW_TROOP=bonebow python art/anim/archer/build_bow_anims.py`) with its own character values (`CHAR["bonebow"]`: a light lean, hips sunk only 0.5×, a big rattle of a rock and recoil at the loose) and its own aim (volley 32°, direct 3.5°). Its rig comes from `art/anim/common/troop_rig.py bonebow`.

- **Gear.** The cape takes the hood-cape lag and the hood tip the tippet lag; it has no quiver bone.
- **Light hips.** The recoil hip key's extra drop is skipped for troops sunk under 1× (shared fix, see WM_HUNTER-REVIEW.md), so the back leg still reaches.

## Review

- **Round 1 (Sonnet high, reel `bonebow_v1`): SHIP WITH MINORS.** No blockers.
- **In-game checks** (`GRAVES_GEO_CHECK_TROOP=bonebow`): **run 6 PASS, all 51 checks.** Attack frames 25/25 at side and three-quarter and 12/12 far; sync 11/11 standing and 4/4 walking; impact weight 1.0; footwork 1.0; hits 7 blows × 3 views; deaths 4/4.
- **Verdict: finished** (visual review and gameplay testing both pass). Video `bonebow_v1`.

## Open minors (polish, not blocking)

- **Brace and hold the line.** Both read close to combat ready (same as the archer).
- **Death behind.** One bow limb rests raised about 45°.
- **Death right.** The bow and gear bunch at rest; the bow rod points up for a moment (t738.7).
- **Death left and behind** drift to the reel panel's edge (camera framing).
- **Light hits** are weak.
- **String and arrow** were not judged (shared art minor).
- **Cape lag** was not visible from the reel's cameras, so it is unconfirmed.

## v2 feel (2026-11-07, branch anim/v2-bow)

The same Knight v2 feel as the archer (see `ARCHER-REVIEW.md`, "v2 feel"), built by `art/anim/archer/build_bow_anims.py` with the bonebow's own CHAR values. The bonebow character is a bone archer with no muscle behind the pull, so it shows little lean and almost no sink. The draw, the shot and the deaths are unchanged, and the deaths are byte-identical to the previous export.

- **Probe.**
  - The bow grip sits inside the head geometry by 5-8 points in every clip, including idle and walk, as in the previous export. This is pre-existing (grip versus skull geometry), not caused by this pass.
  - The v2 hits removed the old heavy front clipping: 118 points before, 7 now, all of them the same grip-versus-skull points.
  - Floor: hits 0 to -0.56 px (medium from behind); hold the line -0.39 px.
  - Largest hit pop: 25.6 px/t, at the heavy front peak, at the bow tip. The volley reaches 29.9 px/t.
- **Review.** Sonnet high, 1 round, verdict SHIP WITH MINORS.
  - Reads like the Knight: hits graded, stances apart.
  - The bow arm always opens outward.
  - No pops, floor breaks or wrong facing seen.
- **Open minors.**
  - Heavy front peak leans about 65-80 degrees in the side view. That is steeper than the archer's accepted 45-55, and more than the "no muscle" character suggests. A per-troop trim of the heavy peak would fix it.
  - The plain idle weight shift is faint.
  - Brace and hold the line are close at thumbnail size.
- **Harness.** Not run: the 5-client budget for this pass ran out (see the family report). The clips share the archer's code path, and the archer was harnessed.
- Video: `bonebow_v2feel`.

## Batch review 2026-11-11

Bow-family batch review on `anim/review-bow`. Timing unchanged: `ATTACK_TICKS` 32, `FULL_AT` 16.5, `IMPACT` 22; both attacks keep `full` 0.825 and `impact` 1.1.

- **Rows before -> after** (`work/review/bonebow.{before,after}.json`): clips with a FAIL 24 -> 22, FAIL rows 45 -> 32, WARN rows 43 -> 35 (no PASS became a WARN).
- **Changed clips (16, spliced; every other clip byte-identical to HEAD).** The hit `_legs` clips came out identical, so they are unchanged.
  - `attack_direct`, `attack_volley` (+legs): Kort's archer r3 shot fitted to the bonebow (`direct_shot.FIT["bonebow"]`): anchor 1 px higher and chest roll weight 0.2 on the direct shot, so the body no longer leans 23 deg sideways at full draw; bow canted -30 deg and raised 20 deg, head turned away at the nock (`nock_head`), so the bow clears the skull; higher, further-out nock and set grips, the nock grip 1 px further forward so the string bracer clears the jerkin (scorecard `pen_limb` 1.68 -> 1.35, limit 1.39). Fist through the head (3.14) and the loose pop are gone.
  - Aim stances: from the r3 full-draw frame, chest takes 85% of the pitch.
  - Hits and `hit_block`: gear swings (bone quiver, bands, pauldrons, skull) soft and low-passed: `secondary_life` and `dead_hold` FAILs cleared.
- **Remaining FAIL rows: the right pauldron fit (model).** `right_pauldron` overlaps the jerkin and `rag_hood` by 2.1-2.4 px whenever the string arm rises: both attacks at the set and hold, the three aim stances (new here, because they now hold the real full-draw arm), rally, ward and fallback. `bow_plate` sits into the head in brace, wall, hold and the front/back/left medium hits; `skull_pauldron` into `rag_hood` in the side hits. These are geometry, not motion: the pauldron cubes are larger than the shoulder they ride on.
- **Other remaining rows.** Aim stances `loop_life` 0.26-0.30 (pre-existing: the held aim barely breathes). Common modules: `hit_pulled`, `stun_upper`, `stun_out_upper`, `hit_knockback`, `charge_run_upper`.
- **Proposed exemption or model fix.** bonebow, every clip with the string arm raised, `armour_clip`, 2.0-2.4, `right_pauldron` into jerkin/rag_hood: shrink or lift the right pauldron in the hand model (preferred), else exempt that pair. bonebow, stances and hits, `armour_clip`, up to 2.5, `bow_plate` into head.
- **Tests.** Scorecard exit 0 (35 clips, 0 fail) after the nock-grip fix (it failed `pen_limb` on both attacks before it). Unit tests OK (see ARCHER-REVIEW batch section). Booth config valid.

## Batch review fix round 1 (2026-11-12)

Review `work/reviewer/BOW-sonnet-review.md` (FIX, 5.5/10). Clip fixes in the bow family's late layer `art/anim/archer/bow_fix_layer.py`
(fingerprinted in `out/bonebow.fixlayer.json`). Shared modules untouched; timing unchanged. No model change shipped (see B1/B2).

- **Focus check** FAIL rows 32 -> 21 (clips with a FAIL 22 -> 21). Scorecard exit 0. All 21 rows left are `armour_clip` on the plates.
- **B4 aims** `loop_life` 0.26-0.30 -> pass: one slow breath over the loop (torso 1.0, arms 0.9 deg).
- **B5 floor** pulled 1.64, stun 1.65, knockback 1.12 -> pass / pass / 0.71 WARN: hips lift 1.9, 1.3 + 0.6 and 1.7 px.
- **B6 still gear** -> pass on pulled, stun, knockback (stun_out 3.42 WARN): `soft_motion`, plus a 5-6 deg jolt by hand on bone_quiver,
  quiver_bands and both pauldrons (the kit does not know their names).
- **B3** skull_pauldron into rag_hood: the bow arm stands 14 deg out through the pulled jolt (bow_plate 2.0 -> WARN) and the stun loop
  (2.64 -> 2.14). Knockback 2.77 -> 2.48, hit_medium_right 2.63, stun_out 1.95 (inherited from the loop) stay.
- **B1/B2 model fit tried, not shipped.** `armour_clip` counts depth over each pair's rest fit. The right pauldron and the bow plate
  already sit in the hood at rest (inner edge 1.5-1.9 px inside), so trimming them lowers the rest fit and raises the score:
  right_pauldron 4 px wide (inner edge -4) gave volley 2.39 -> 3.37, 3.2 px 4.16, moved out 1.5 px 3.87, lifted 1 px 2.56; a thinner or
  shorter cube 2.03-2.48. At the full draw the pauldron passes clean through the jerkin (2.3 = half its 4.6 px inflated depth), so small
  slides of the plate (1-3 px down or out) do not move the number either. Head rolls trade the right plate for the left (bow_plate,
  skull_pauldron, fan_arrow0). Fix needs a redesign of both shoulders (plates outboard of the hood at rest) together with the draw pose.
- Changed clips: the three aims, `hit_pulled`(+legs), `hit_knockback`(+legs), `stun_upper`, `stun_legs`, `stun_out_upper`; every other clip
  byte-identical to ae138367.
