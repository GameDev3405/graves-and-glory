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
