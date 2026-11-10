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

## Own builder (2026-11-10, branch anim/stalker-fable-2)

A trial run: all 35 stalker animations rebuilt from scratch on the stalker's own builder, not the shared bow builder. Preview art; not merged.

- **Build.** `python art/anim/stalker/build_stalker_anims.py`, then `python art/anim/common/export.py stalker`. The solver is `art/anim/stalker/stalker_core.py`. The shared bow builder is imported as a library only and is unchanged, so the other bow troops are untouched.
- **Timing.** Clip names, loop flags, lengths and attack markers match the shipped file, so gameplay timing is unchanged.
- **Archer-specific stances.** Hold stands the bow upright at the lead foot like a staff. Wall holds it upright before the chest with an arrow nocked. Ward thrusts it out level like a bar. Shield up is a deep duck under the level bow with the string hand guarding the face (he has no shield). Rest trails the bow at the thigh. Brace is a crouch at half draw. Idle carries the bow one-handed with no arrow on the string; ready nocks one.
- **Footage.** In-game capture suite `stalker-reel` (`test-tools/java/gg/gravesandglory/StalkerReelChecks.java`): 35 segments, 1,446 frames, all checks pass on the second run. Video `C:\Users\kortt\Melty\status\booth-videos\stalker-fable-2.mp4` (90 s, each animation labelled).
- **Review (Sonnet high, sheets, one round).** Fixed: the bow stood upright after death forward (the bow now lies flat in all four deaths); the run and charge reel segments were too short to reach the run gait. Not changed: wall and shield up read weakly head-on (the bow is edge-on from the front; they read from three-quarter in game); light hits are small by design.
- **Tests.** `python -m unittest discover -s tools/anim`: 115 pass. `python tools/verify.py fast`: 696 Python tests, one failure (jar leak guard missing the new reel class), fixed in `tools/release_config.py` and that test re-run alone; the full fast verify was not re-run after the fix.

### Scorecard: 19 of 35 clips miss a knight-derived limit (shipped stalker: 5)

- **Attacks.** String forearm inside the jaw-anchor volume (pen_limb 3.9 px, limit 1.4); volley torso speed 76 (limit 60); direct arm speed 164 (limit 160).
- **Stances.** pen_limb 1.8 to 2.7 px on ready, wall, ward and the three aims (limit 1.79); pen_weapon 1.6 to 2.7 px on wall, shield up, hold and rally (limit 1.49); fallback misses four limits (arms 9.0/8, legs 68/65, pen_weapon 1.16/1.01, pen_limb 2.0/0.6).
- **Movement.** Run torso speed 19.7 (limit 19.1); charge pen_weapon 2.9 px (limit 2.4).
- **Hits and stun.** Medium left weapon speed 102 (limit 100); stun loop_vel 10.3 (limit 9.4).
- **Deaths.** Weapon speed as the bow is let go: back 91, left 121, right 85 (limit 83).

### Open points

- **Ward is unreachable in game.** A troop with fallback clips plays those for `fall_back` (StanceController), so `stance_ward_upper` never plays; the reel's ward segment shows fallback standing.
- **Knockback and pulled** are filmed in place; the capture does not slide the body.
