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

## v2 feel (2026-11-07, branch anim/v2-bow)

The same Knight v2 feel as the archer (see `ARCHER-REVIEW.md`, "v2 feel"), built by `art/anim/archer/build_bow_anims.py` with the stalker's own CHAR values. The stalker character is a patient hunter, set low with the deepest sink. The draw, the shot and the deaths are unchanged, and the deaths are byte-identical to the previous export.

- **Probe.**
  - No bow or fist in the body in any clip.
  - Floor: hits -0.39 to -0.48 px; hold the line -0.62 px; brace -0.46 px (the mantle hem and boots, as at baseline).
  - Largest hit pop: 27.9 px/t, at the heavy front peak, at the bow tip. The volley reaches 32.2 px/t.
- **Review.** Sonnet high, 1 round, verdict SHIP WITH MINORS.
  - Reads like the Knight: hits graded, stances apart (brace wide and low, hold the line forward).
  - The bow arm swings out and back in the heavy hit and never folds in. In the front view at the heavy start, the bow tip sits near the chin; this is depth overlap only, as for the archer.
  - No clipping through the body or mantle, and no floor breaks, pops or wrong facing seen.
- **Open minors.**
  - Heavy front peak leans about 55-65 degrees, slightly past the archer's accepted 45-55.
  - Medium hits from behind (about 60 degrees forward) and from the left (the head pitched far down) read close to heavy. Their recovery cells are still hunched.
  - The plain idle weight shift is faint in the front view. The deepest sink shows in brace and combat ready more than in the idle.
- **Harness.** geo-knight suite with `GRAVES_GEO_CHECK_TROOP=stalker`: 22 `[p1] p1-` lines, 2 `=false`.
  - `attack-sync-stand` matched 8 of 9 swings; one swing logged `clipnull`.
  - The suite line is false only because that check failed.
  - The attack clips are byte-identical to the previous export, so this is not caused by this pass. The archer showed the same miss, under lag.
  - Not rerun: the client budget for this pass ran out.
- Video: `stalker_v2feel`.
