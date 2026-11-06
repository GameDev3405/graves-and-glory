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
