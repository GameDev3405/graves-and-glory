# stalker review record (2026-11-06)

The fifth and last bow unit, the barrow stalker. It uses the archer's bow builder (`GG_BOW_TROOP=stalker python art/anim/archer/build_bow_anims.py`) with its own character values (`CHAR["stalker"]`: lean −18, hips sunk 1.6×, the least rock and recoil of the family, a patient low shot) and its own aim (volley 35°, direct 4°). Its rig comes from `art/anim/common/troop_rig.py stalker`.

- **Gear.** The pelt tail takes the hood-cape lag and the wrap tails the tippet lag; it has no quiver bone.
- **Death back.** The wide mantle swallowed a bow laid by the hip, so from tick 7 of the fall the bow arm flings out wide (`left_arm` −30, 0, −90, forearm straight) and the bow lands clear of the body.

## Review

- **Round 1 (Sonnet high, reel `stalker_v1`): fix needed.** In death back the bow was buried in the mantle.
- **Round 2 (Sonnet high, reel `stalker_v2`): SHIP WITH MINORS.** The arm fling clears the bow in death back; no blockers elsewhere.
- **In-game checks** (`GRAVES_GEO_CHECK_TROOP=stalker`): **run 7 PASS, all 51 checks.** Attack frames 25/25 at side and three-quarter (6 stepped past) and 12/12 far; sync 7/7 standing and 4/4 walking; impact weight 1.0; footwork 1.0; hits 7 blows × 3 views; deaths 4/4.
- **Verdict: finished** (visual review and gameplay testing both pass). Video `stalker_v2`.

## Open minors (polish, not blocking)

- **Bow art.** The bow reads thin and grey; the string renders as a rigid rod (shared art minor).
- **Bow limbs at the hip** read as floating cubes in some frames.
- **Death left.** The bow looks detached near the reel panel's edge.
- **Deaths** drift or clip at the reel panel's edges (camera framing).
- **Brace, hold the line and ward** read close to combat ready (same as the archer).
- **Foot slide** could not be confirmed at thumbnail size.
